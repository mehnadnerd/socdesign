// See README.md for license details.

package socdesign

import chisel3._
import chisel3.util.{Decoupled, DecoupledIO, Enum, ReadyValidIO, log2Up}

class MainGEMMCtrl(addrWidth: Int) extends Bundle {
  val start = Input(Bool())
  val cmd = Input(new MainGEMMCmd(addrWidth))
  val finished = Output(Bool())
}

class MainGEMMCmd(addrWidth: Int) extends Bundle {
  val a_addr = Input(UInt(addrWidth.W))
  val b_addr = Input(UInt(addrWidth.W))
  val c_addr = Input(UInt(addrWidth.W))
  val m = Input(UInt(addrWidth.W))
  val n = Input(UInt(addrWidth.W))
  val k = Input(UInt(addrWidth.W))
}

class MainGEMMMemReadChannel(addrWidth: Int, dataWidth: Int) extends Bundle {
  val ctrl = Output(Decoupled(UInt(addrWidth.W))) // TODO: may need smarter mem
  val data = Input(Decoupled(UInt(dataWidth.W)))
}

class MainGEMMMemWriteChannel(addrWidth: Int, dataWidth: Int) extends Bundle {
  val ctrl = Output(Decoupled(UInt(addrWidth.W)))
  val data = Output(Decoupled(UInt(dataWidth.W)))
}

class MainGEMMMemIo(addrWidth: Int, indataWidth: Int, outdataWidth: Int) extends Bundle {
  val a = new MainGEMMMemReadChannel(addrWidth, indataWidth)
  val b = new MainGEMMMemReadChannel(addrWidth, indataWidth)
  val c = new MainGEMMMemWriteChannel(addrWidth, outdataWidth)
}

// Note: requires m, n, k to be multiples of bWidth/aPack

class MainGEMM extends Module {
  val addrWidth = 32 // Width of addresses
  val indataWidth = 16 // Width of input data
  val outdataWidth = 32 // Width of output data
  val aLength = 2048 // Maximum length of a row of A
  val bcLength = 2048 // Maximum length of a row of B/C, very cheap to increase
  val acHeight = 16 // Number of rows of A/C we hold at once
  val bWidth = 16 // Number of elements of B we read in at once, probably should match dramWidth/indataWidth
  val outMaxHeight = 2048 // Maximum number of rows of A/C, very cheap to increase
  val dramWidth = 256 // Width of channel from memory, used to figure out SIMD-esque length
  val aPack = dramWidth / indataWidth
  assert(aPack == bWidth) // TODO: this probably could be loosened

  // TODO: might need to do packing of data to make sure fits in BRAMs
  // TODO: the halving-quartering-&c of effective bWidth to get taller

  val io = IO(new Bundle {
    val ctrl = new MainGEMMCtrl(addrWidth)
    val mem = new MainGEMMMemIo(addrWidth, dramWidth, dramWidth) // dram Width, do Vec trasform internnally
  })

  val a = Wire(Vec(aPack, UInt(indataWidth.W)))
  a := io.mem.a.data.bits.asTypeOf(a)
  val b = Wire(Vec(bWidth, UInt(indataWidth.W)))
  b := io.mem.b.data.bits.asTypeOf(b)


  val aStorage = Seq.fill(acHeight) {
    SyncReadMem(aLength, Vec(aPack, UInt(indataWidth.W)))
  } // note that multiple elements are packed together
  val aRead = Wire(Vec(acHeight, UInt(indataWidth.W))) // note: this is multiple rows of A
  val aReadValid = Wire(Bool())
  val aReadReady = Wire(Bool())
  val bRead = Wire(Vec(bWidth, UInt(indataWidth.W)))
  val bReadValid = Wire(Bool())
  val bReadReady = Wire(Bool())

  {
    bRead := b
    bReadValid := io.mem.b.data.valid
    io.mem.b.data.ready := bReadReady // TODO: wtf is thsi things problem
  }

  val cReg = Seq.fill(acHeight) {
    Seq.fill(bWidth) {
      RegInit(0.U(outdataWidth.W))
    }
  }

  val rowProgress = RegInit(0.U(log2Up(outMaxHeight).W)) // progress in terms of how many rows
  val aProgress = RegInit(0.U(log2Up(aLength).W)) // progress through row
  val aRowProgress = RegInit(0.U(log2Up(acHeight).W)) // progress through the acHeight number of rows

  val bRowProgress = RegInit(0.U(log2Up(outMaxHeight).W)) // progress in terms of how many rows of B
  val bColProgress = RegInit(0.U(log2Up(bcLength).W)) // progress in terms of how many columns of B we have done

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
    io.ctrl.finished := false.B
    io.mem.a.data.ready := false.B
    bReadReady := false.B
    io.mem.c.data.valid := false.B
  }

  // TODO: maybe just have the memory address generation seperate, so this stuff only worries about trackig elements


  val cmd = Reg(new MainGEMMCmd(addrWidth))

  when(state === s_idle) {
    when(io.ctrl.start) {
      state := s_reload_a
      rowProgress := 0.U
      aProgress := 0.U
      cmd := io.ctrl.cmd
    }
  }

  when(state === s_reload_a) {
    io.mem.a.data.ready := true.B
    for (i <- 0 until acHeight) {
      when(aRowProgress === i.U) {
        aStorage(i).write(aProgress, a)
      }
    }
    when(io.mem.a.data.valid) {
      aProgress := aProgress + aPack.U

      // new row of A
      when(aProgress === (cmd.m - 1.U)) { // TODO: Check is M
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
          cReg(i)(j) := cReg(i)(j) + (a(i) * b(j))
        }
      }
      bRowProgress := bRowProgress + 1.U
    }

    // TODO stuff done
    {
      state := s_out
    }
  }

  when(state === s_out) {
    // TODO write out C, do the zeroing of it too?

    // TODO normal done with colset
    {
      bRowProgress := 0.U
      bColProgress := bColProgress + bWidth.U
    }

    // TODO done with rowset
    {
      rowProgress := rowProgress + acHeight.U

      bRowProgress := 0.U
      bColProgress := 0.U

      aProgress := 0.U
      aRowProgress := 0.U

      state := s_reload_a
    }

    // TODO check if completely done
    {
      state := s_finish
    }
  }

}
