package socdesign

import chisel3._
import chisel3.util.Decoupled

class GEMMCtrlChannel(addrWidth: Int) extends Bundle {
  val waddr = Flipped(Decoupled(UInt(addrWidth.W)))
  val wdata = Flipped(Decoupled(UInt(addrWidth.W)))
  val wresp = Decoupled()
  val wid = Input(UInt(16.W))
  val bid = Output(UInt(16.W))

  val raddr = Flipped(Decoupled(UInt(addrWidth.W)))
  val rdata = Decoupled(UInt(addrWidth.W))
  val arid = Input(UInt(16.W))
  val rid = Output(UInt(16.W))
  override def cloneType = new GEMMCtrlChannel(addrWidth).asInstanceOf[this.type]
}

class GEMM(aLength: Int = 2048,
               acHeight: Int = 32,
               dramWidth: Int = 128,
               addrWidth: Int = 49,
           addrMask: Int = 0xff) extends Module {
  val io = IO(new Bundle {
    val ctrl = new GEMMCtrlChannel(addrWidth)
    val mem = new MainGEMMMemIo(addrWidth, dramWidth, dramWidth)
  })

  val gemm = Module(new MainGEMM(aLength, acHeight, dramWidth, addrWidth))
  gemm.io.mem <> io.mem
  gemm.io.ctrl_start := false.B

  val m = RegInit(0.U(addrWidth.W))
  val n = RegInit(0.U(addrWidth.W))
  val k = RegInit(0.U(addrWidth.W))
  val a_addr = RegInit(0.U(addrWidth.W))
  val b_addr = RegInit(0.U(addrWidth.W))
  val c_addr = RegInit(0.U(addrWidth.W))

  //val regs = VecInit(m, n, k, a_addr, b_addr, c_addr)

  gemm.io.ctrl_cmd.m := m
  gemm.io.ctrl_cmd.n := n
  gemm.io.ctrl_cmd.k := k
  gemm.io.ctrl_cmd.a_addr := a_addr
  gemm.io.ctrl_cmd.b_addr := b_addr
  gemm.io.ctrl_cmd.c_addr := c_addr

  gemm.io.ctrl_start := false.B
  val finishedReg = RegInit(false.B)
  when(gemm.io.ctrl_finished) {
    finishedReg := true.B
  }


  // to deal with AXI bullshit
  val waddrCache = RegInit(0.U(addrWidth.W))
  val wacValid = RegInit(false.B)
  val wdataCache = RegInit(0.U(addrWidth.W))
  val wdcValid = RegInit(false.B)
  val wrPending = RegInit(false.B)
  // I could probably do things to make this like a cycle faster but idk enough
  val wid = RegInit(0.U(16.W))
  io.ctrl.bid := wid

  io.ctrl.waddr.ready := !wacValid
  when (io.ctrl.waddr.valid) {
    waddrCache := io.ctrl.waddr.bits
    wacValid := true.B
    wid := io.ctrl.wid
  }

  io.ctrl.wdata.ready := !wdcValid
  when (io.ctrl.wdata.valid) {
    wdataCache := io.ctrl.wdata.bits
    wdcValid := true.B
  }

  val wcmdaddr = waddrCache & addrMask.U

  when (wacValid && wdcValid && !wrPending) {
    wacValid := false.B
    wdcValid := false.B
    wrPending := true.B

    when (wcmdaddr === 0.U) { // start
      gemm.io.ctrl_start := true.B
    }
    when (wcmdaddr === 4.U) { // m
      m := wdataCache
    }
    when (wcmdaddr === 8.U) { // n
      n := wdataCache
    }
    when (wcmdaddr === 12.U) { // k
      k := wdataCache
    }
    when (wcmdaddr === 16.U) { // a_addr
      a_addr := wdataCache
    }
    when (wcmdaddr === 20.U) { // b_addr
      b_addr := wdataCache
    }
    when (wcmdaddr === 24.U) { // c_addr
      c_addr := wdataCache
    }
  }
  io.ctrl.wresp.valid := wrPending
  when (io.ctrl.wresp.fire()) {
    wrPending := false.B
  }

  val raddrCache = RegInit(0.U(addrWidth.W))
  val racValid = RegInit(false.B)

  val arid = RegInit(0.U(16.W))
  io.ctrl.rid := arid

  val rPending = RegInit(false.B)
  val rResult  = RegInit(false.B)

  io.ctrl.raddr.ready := !racValid && !rPending
  when (io.ctrl.raddr.valid) {
    raddrCache := io.ctrl.raddr.bits
    racValid := true.B
    arid := io.ctrl.arid
  }

  when (racValid) {
    racValid := false.B
    rResult := finishedReg
    rPending := true.B
  }

  io.ctrl.rdata.valid := rPending
  io.ctrl.rdata.bits := rResult
  when(io.ctrl.rdata.fire()) {
    rPending := false.B
  }
}