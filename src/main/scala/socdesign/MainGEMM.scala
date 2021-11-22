// See README.md for license details.

package socdesign

import chisel3._
import chisel3.util.{Decoupled, DecoupledIO, Enum, ReadyValidIO, log2Up, Cat}

class MainGEMMCmd(addrWidth: Int) extends Bundle {
  val a_addr = UInt(addrWidth.W)
  val b_addr = UInt(addrWidth.W)
  val c_addr = UInt(addrWidth.W)
  val m = UInt(addrWidth.W)
  val n = UInt(addrWidth.W)
  val k = UInt(addrWidth.W)

  override def cloneType = new MainGEMMCmd(addrWidth).asInstanceOf[this.type]
}

class MainGEMMMemReadChannel(addrWidth: Int, dataWidth: Int) extends Bundle {
  val addr = Decoupled(UInt(addrWidth.W)) // TODO: may need smarter mem
  val data = Flipped(Decoupled(UInt(dataWidth.W)))

  override def cloneType = new MainGEMMMemReadChannel(addrWidth, dataWidth).asInstanceOf[this.type]
}

class MainGEMMMemWriteChannel(addrWidth: Int, dataWidth: Int) extends Bundle {
  val addr = Decoupled(UInt(addrWidth.W))
  val data = Decoupled(UInt(dataWidth.W))

  override def cloneType = new MainGEMMMemWriteChannel(addrWidth, dataWidth).asInstanceOf[this.type]
}

class MainGEMMMemIo(addrWidth: Int, indataWidth: Int, outdataWidth: Int) extends Bundle {
  val a = new MainGEMMMemReadChannel(addrWidth, indataWidth)
  val b = new MainGEMMMemReadChannel(addrWidth, indataWidth)
  val c = new MainGEMMMemWriteChannel(addrWidth, outdataWidth)

  override def cloneType = new MainGEMMMemIo(addrWidth, indataWidth, outdataWidth).asInstanceOf[this.type]

}

// Note: requires m, n, k to be multiples of bWidth/aPack

class MainGEMM(aLength: Int = 2048,
               acHeight: Int = 32,
               dramWidth: Int = 128,
               addrWidth: Int = 49) extends Module {
  //val addrWidth = addressWidth// Width of addresses
  val indataWidth = 16 // Width of input data
  val outdataWidth = 32 // Width of output data
  //val aLength = aLength // Maximum length of a row of A, max K
  val bcLength = 2048 // Maximum length of a row of B/C, very cheap to increase, max N
  //val acHeight = acHeight//16 // Number of rows of A/C we hold at once
  val bWidth = dramWidth / indataWidth//8 // Number of elements of B we read in at once, probably should match dramWidth/indataWidth
  val outMaxHeight = 2048 // Maximum number of rows of A/C, very cheap to increase, max M
  //val dramWidth = dramWidth//128 // Width of channel from memory, used to figure out SIMD-esque length
  val aPack = dramWidth / indataWidth // how may elements of a are packed together
  val numCs = acHeight * bWidth * outdataWidth / dramWidth // how many packings of C there are, to write out
  val cpr = bWidth * outdataWidth / dramWidth // how many C packings per row, needed for C addr generation
  assert(aPack == bWidth) // TODO: this probably could be loosened

  // TODO: might need to do packing of data to make sure fits in BRAMs
  // TODO: the halving-quartering-&c of effective bWidth to get taller

  val io = IO(new Bundle {
    val ctrl_start = Input(Bool())
    val ctrl_cmd = Input(new MainGEMMCmd(addrWidth))
    val ctrl_finished = Output(Bool())
    val mem = new MainGEMMMemIo(addrWidth, dramWidth, dramWidth) // dram Width, do Vec trasform internnally
  })

  val a = Wire(Vec(aPack, UInt(indataWidth.W)))
  a := io.mem.a.data.bits.asTypeOf(a)
  val b = Wire(Vec(bWidth, UInt(indataWidth.W)))
  b := io.mem.b.data.bits.asTypeOf(b)


  val aStorage = Seq.fill(acHeight) {
    SyncReadMem(aLength / aPack, Vec(aPack, UInt(indataWidth.W)))
  } // note that multiple elements are packed together
  val aRead = Wire(Vec(acHeight, UInt(indataWidth.W))) // note: this is multiple rows of A
  val aReadValid = Wire(Bool())
  val aReadReady = Wire(Bool())
  val bRead = Wire(Vec(bWidth, UInt(indataWidth.W)))
  val bReadValid = Wire(Bool())
  val bReadReady = Wire(Bool())
  val cWrite = Wire(UInt(dramWidth.W))
  val cWriteValid = Wire(Bool())
  val cWriteReady = Wire(Bool())

  {
    bRead := b
    bReadValid := io.mem.b.data.valid
    io.mem.b.data.ready := bReadReady

    io.mem.c.data.bits := cWrite
    cWriteReady := io.mem.c.addr.ready && io.mem.c.data.ready // n.b. if writevalid was more complicated, this could lead to combinaitional loops
    io.mem.c.addr.valid := cWriteValid
    io.mem.c.data.valid := cWriteValid
  }

  val cReg = Seq.fill(acHeight) {
    Seq.fill(bWidth) {
      RegInit(0.U(outdataWidth.W))
    }
  }

  val rowProgress = RegInit(0.U(log2Up(outMaxHeight).W)) // progress in terms of how many rows
  val aProgress = RegInit(0.U(log2Up(aLength).W)) // progress through row, in terms of elements
  val aProgressWire = Wire(UInt(log2Up(aLength).W)) // progress through row, in terms of a groups
  aProgressWire := aProgress / aPack.U
  val aRowProgress = RegInit(0.U(log2Up(acHeight).W)) // progress through the acHeight number of rows

  val bRowProgress = RegInit(0.U(log2Up(outMaxHeight).W)) // progress in terms of how many rows of B
  val bColProgress = RegInit(0.U(log2Up(bcLength).W)) // progress in terms of how many columns of B we have done

  val cProgress = RegInit(0.U(log2Up(acHeight * bWidth * outdataWidth / dramWidth).W))
  val aAddr = Reg(UInt(addrWidth.W))
  val bAddr = Reg(UInt(addrWidth.W))
  val bAddrRow = RegInit(0.U(log2Up(outMaxHeight).W))
  val bAddrCol = RegInit(0.U(log2Up(bcLength).W))
  val cAddr = Reg(UInt(addrWidth.W))
  io.mem.c.addr.bits := cAddr

  val s_idle :: s_reload_a :: s_calc :: s_out :: s_finish :: Nil = Enum(5)
  val state = RegInit(s_idle)

  /*
   * State plan:
   * s_idle: not doing anything, goes to s_reload_a when start
   * s_reload_a: load a new rowset of a, goes to s_calc when rowset loaded
   * s_calc: stream in a colset of b, accumulate/mul in c reg, goes to s_out at end of col
   * s_out: write out c, goes back to s_calc when written out, s_finish if done all
   * s_finish: send response, go to s_idle
   */

  // Defaults
  {
    io.ctrl_finished := false.B
    io.mem.a.data.ready := false.B
    aReadReady := false.B
    bReadReady := false.B
    cWriteValid := false.B

    io.mem.a.addr.valid := false.B
    io.mem.b.addr.valid := false.B
  }

  val cmd = Reg(new MainGEMMCmd(addrWidth))

  when(state === s_idle) {
    when(io.ctrl_start) {
      state := s_reload_a
      rowProgress := 0.U
      aProgress := 0.U
      aRowProgress := 0.U
      cmd := io.ctrl_cmd
      aAddr := io.ctrl_cmd.a_addr
      bAddr := io.ctrl_cmd.b_addr
      cAddr := io.ctrl_cmd.c_addr

      bAddrRow := 0.U
      bAddrCol := 0.U
    }
  }

  when(state === s_reload_a) {
    io.mem.a.data.ready := true.B
    when(io.mem.a.data.fire()) {
      for (i <- 0 until acHeight) {
        when(aRowProgress === i.U) {
          aStorage(i).write(aProgressWire, a)
        }
      }
      aProgress := aProgress + aPack.U

      // new row of A
      when(aProgress === (cmd.k - aPack.U)) {
        aRowProgress := aRowProgress + 1.U
        aProgress := 0.U

        // done with reload
        when(aRowProgress === (acHeight - 1).U) {
          state := s_calc
        }
      }
    }
  }

  when(state === s_calc) {
    bReadReady := aReadValid
    aReadReady := bReadValid
    when(aReadValid && bReadValid) {
      // no halving/quarterig right now
      for (i <- 0 until acHeight) {
        for (j <- 0 until bWidth) {
          cReg(i)(j) := cReg(i)(j) + (aRead(i) * bRead(j))
        }
      }
      bRowProgress := bRowProgress + 1.U // used for a as well
    }

    when(bRowProgress === cmd.k - 1.U) {
      state := s_out
      cProgress := 0.U
    }
  }

  val cBundles = Wire(Vec(numCs, UInt(dramWidth.W)))
  cBundles := VecInit(cReg.flatten.grouped(dramWidth / outdataWidth).map { a => Cat(a.reverse) }.toSeq)
  cWrite := cBundles(cProgress)

  when(state === s_out) {
    cWriteValid := true.B

    when(cWriteReady) {
      cProgress := cProgress + 1.U
      when((cProgress + 1.U) % cpr.U === 0.U) { // new row of C,
        cAddr := cAddr + (cmd.n * (outdataWidth / 8).U) - ((cpr - 1) * dramWidth / 8).U
      }.otherwise { // just next element in row of C
        cAddr := cAddr + (dramWidth / 8).U
      }

      bRowProgress := 0.U // done here so a can be ready in time

      when(cProgress === (numCs - 1).U) { // done with a colset of B
        bRowProgress := 0.U
        bColProgress := bColProgress + bWidth.U

        cAddr := cAddr - cmd.n * ((acHeight - 1) * outdataWidth / 8).U + (dramWidth / 8).U

        cReg.foreach { //  zero entirety of C
          _.foreach {
            _ := 0.U
          }
        }
        state := s_calc

        when(bColProgress === cmd.n - bWidth.U) { // done with a rowset of A
          rowProgress := rowProgress + acHeight.U

          bRowProgress := 0.U
          bColProgress := 0.U

          cAddr := cAddr + (dramWidth / 8).U

          aProgress := 0.U
          aRowProgress := 0.U

          state := s_reload_a

          when(rowProgress === cmd.m - acHeight.U) { // done done
            state := s_finish
          }
        }
      }
    }
  }

  when(state === s_finish) {
    io.ctrl_finished := true.B
    state := s_idle
  }

  // A storage read
  // abuse the fact that multiple elements in each As
  assert(aPack >= 2)
  val aReadAddr = Wire(UInt(log2Up(aLength).W))
  val aReadRaw = aStorage.map { a => a.read(aReadAddr) }
  aReadAddr := bRowProgress / aPack.U
  val aReadElem = bRowProgress % aPack.U

  aReadRaw.zipWithIndex.foreach { case (a, i) => aRead(i) := a(aReadElem) }

  aReadValid := false.B
  when(state === s_calc) {
    aReadValid := true.B // always true since we nset bRowProgress and hence aReadAddr a cycle before calc
    when(bRowProgress % aPack.U === (aPack - 1).U && aReadReady) {
      // at end of aPack group
      aReadAddr := bRowProgress / aPack.U + 1.U
    }
  }

  // A address gen
  io.mem.a.addr.bits := aAddr
  when(state =/= s_idle) {
    io.mem.a.addr.valid := aAddr < (cmd.a_addr + (cmd.m * cmd.k * (indataWidth / 8).U)) // TODO: memoise?
    when(io.mem.a.addr.fire()) {
      aAddr := aAddr + (dramWidth / 8).U // step to next dramWidth-sized chunk. uses fact rows are right after another
    }
  }

  // B address gen
  io.mem.b.addr.bits := bAddr
  when(state =/= s_idle) { // TODO: state?? should we wait for calc?
    io.mem.b.addr.valid := bAddrCol < cmd.n
    when(io.mem.b.addr.fire()) { // next set of B
      bAddr := bAddr + cmd.n * (indataWidth / 8).U // step one row down
      bAddrRow := bAddrRow + 1.U
      when(bAddrRow === cmd.k - 1.U) { // at bottom, so ext column of B
        bAddr := bAddr + (bWidth * indataWidth / 8).U - ((cmd.k - 1.U) * cmd.n * (indataWidth / 8).U) // step 1 element over, then k rows ups
        bAddrRow := 0.U
        bAddrCol := bAddrCol + bWidth.U
        when(bAddrCol === cmd.n - bWidth.U && (rowProgress =/= cmd.m - acHeight.U)) {
          // the second check is so we do't try to read when done
          // at bottom right, reset to beginning
          bAddr := cmd.b_addr
          bAddrRow := 0.U
          bAddrCol := 0.U
        }
      }
    }
  }
}
