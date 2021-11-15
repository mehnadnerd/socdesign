package socdesign

import chisel3._


object Main extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new MainGEMM())
}

object FirrtlMain extends App {
  (new chisel3.stage.ChiselStage).emitFirrtl(new MainGEMM())
}

object MockTesterMain extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new MockTester())
}