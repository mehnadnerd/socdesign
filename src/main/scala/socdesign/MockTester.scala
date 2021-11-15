package socdesign

import chisel3._
import chisel3.util.Queue
import chisel3.util.Cat

class MockTester extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val done = Output(Bool())
  })

  val gemm = Module(new MainGEMM())

  gemm.io.ctrl_start := io.start
  io.done := gemm.io.ctrl_finished

  val dramWidth = 256
  val ty = UInt(dramWidth.W)
  val elemSize = 16
  val elemPack = dramWidth / elemSize

  val aq = Module(new Queue(ty, 8))
  val bq = Module(new Queue(ty, 8))
  gemm.io.mem.a.data <> aq.io.deq
  gemm.io.mem.b.data <> bq.io.deq

  aq.io.enq <> gemm.io.mem.a.ctrl
  aq.io.enq.bits := Cat((0 until elemPack) map { i => gemm.io.mem.a.ctrl.bits(15, 0) + i.U } reverse)
  bq.io.enq <> gemm.io.mem.b.ctrl
  bq.io.enq.bits := Cat((0 until elemPack) map { i => gemm.io.mem.b.ctrl.bits(15, 0) + i.U } reverse)

  gemm.io.mem.c.ready := true.B
  when(gemm.io.mem.c.fire()) {
    printf("write addr %x data %x\n", gemm.io.mem.c.bits.addr, gemm.io.mem.c.bits.data)
  }
  gemm.io.ctrl_cmd.m := 32.U
  gemm.io.ctrl_cmd.n := 32.U
  gemm.io.ctrl_cmd.k := 32.U

  gemm.io.ctrl_cmd.a_addr := 0.U
  gemm.io.ctrl_cmd.b_addr := 0.U
  gemm.io.ctrl_cmd.c_addr := 0.U

}