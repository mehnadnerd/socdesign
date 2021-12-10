package socdesign

import chisel3._
import chisel3.util.Queue
import chisel3.util.Cat
import chisel3.util.Counter

class MockTester extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val done = Output(Bool())
  })

  val gemm = Module(new MainGEMM(dramWidth = 128, acHeight = 32, aLength = 2048, addrWidth = 48))

  gemm.io.ctrl_start := io.start
  io.done := gemm.io.ctrl_finished

  val dramWidth = 128
  val ty = UInt(dramWidth.W)
  val addrTy = UInt(48.W)
  val elemSize = 16
  val elemPack = dramWidth / elemSize

  val aq = Module(new Queue(ty, 8))
  val bq = Module(new Queue(ty, 8))
  gemm.io.mem.a.data <> aq.io.deq
  gemm.io.mem.b.data <> bq.io.deq

  aq.io.enq <> gemm.io.mem.a.addr
  aq.io.enq.bits := Cat((0 until elemPack) map { i => gemm.io.mem.a.addr.bits(15, 0) + (i * elemSize / 8).U } reverse)
  bq.io.enq <> gemm.io.mem.b.addr
  bq.io.enq.bits := Cat((0 until elemPack) map { i => gemm.io.mem.b.addr.bits(15, 0) + (i * elemSize / 8).U } reverse)

  val caq = Module(new Queue(addrTy, 8))
  val cdq = Module(new Queue(ty, 8))

  val c = Counter(19)
  val c2 = Counter(29)

  c.inc()
  c2.inc()

  gemm.io.mem.c.addr <> caq.io.enq
  gemm.io.mem.c.data <> cdq.io.enq

  gemm.io.mem.c.addr.ready := caq.io.enq.ready && (c.value < 4.U)
  caq.io.enq.valid := gemm.io.mem.c.addr.valid && (c.value < 4.U)
  gemm.io.mem.c.data.ready := cdq.io.enq.ready && (c2.value < 6.U)
  cdq.io.enq.valid := gemm.io.mem.c.data.valid && (c2.value < 6.U)


  caq.io.deq.ready := cdq.io.deq.valid
  cdq.io.deq.ready := caq.io.deq.valid
  when(caq.io.deq.fire()) {
    printf("write addr %x data %x\n", caq.io.deq.bits, cdq.io.deq.bits)
  }
  gemm.io.ctrl_cmd.m := 32.U
  gemm.io.ctrl_cmd.n := 32.U
  gemm.io.ctrl_cmd.k := 32.U

  gemm.io.ctrl_cmd.a_addr := 0.U
  gemm.io.ctrl_cmd.b_addr := 0.U
  gemm.io.ctrl_cmd.c_addr := 0.U

}