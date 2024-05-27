package curlyrv.ops

import spinal.core._
import spinal.lib.misc.pipeline._

import curlyrv.CpuBase

/** Registerfile Operation
  *
  * Implements the 32 register x0 to x31 as dual port read and single port
  * write RAM. To enable FPGA mapping of regfile to RAM, register x0 is allowed
  * to be written to any value. Reading it is forced to be zero though.
  *
  * RAW data hazard is detected by comparing RS1 and RS2 from execute stage to
  * RD in memory and writeback stages. Hazard is solved by forwarding the value.
  *
  * @param cpu CPU to which the registerfile is added
  */
case class RegfileOps(cpu: CpuBase) extends CpuBaseOps {
  import cpu.pipes._
  import cpu.config._

  val RS1_NONZERO, RS2_NONZERO = Payload(Bool)
  val regfile = Mem(SInt(XLEN bits), 32)
  val rs1d, rs2d = SInt(XLEN bits)
  val isHazard = Vec(Bool, 3)

  val decoder = new decode.Area {
    RS1_NONZERO := RS1 =/= 0
    RS2_NONZERO := RS2 =/= 0

    // Regfile is read synchronously in decode stage and result is available in
    // execute stage.
    rs1d := RS1_NONZERO ? regfile.readSync(RS1) | 0
    rs2d := RS2_NONZERO ? regfile.readSync(RS2) | 0
    RS1D := 0
    RS2D := 0

    // Check for RAW hazards of RS1/2 and RD of previous instructions that are
    // still in-flight in EX, MEM and WB stages and not yet commited to regfile.
    def isRAWHazard(stage: CtrlLink, rs: Payload[UInt], rsNonZero: Payload[Bool]) = {
      isValid && stage.isValid && rsNonZero && stage(ENABLE_WB) && rs === stage(RD)
    }
    // isRAWHazard(execute, RS1, RS1_NONZERO)
  }

  // Detect RAW daza hazards between EX <-> MEM and EX <-> WB.
  val rs1RawMem, rs2RawMem, rs1RawWb, rs2RawWb = Bool
  val executer = new execute.Area {
    val possibleRawMem = isValid && memory.isValid && memory(ENABLE_WB)
    rs1RawMem := RS1 === memory(RD) && RS1_NONZERO && possibleRawMem
    rs2RawMem := RS2 === memory(RD) && RS2_NONZERO && possibleRawMem
    val possibleRawWb = isValid && writeback.isValid && writeback(ENABLE_WB)
    rs1RawWb := RS1 === writeback(RD) && RS1_NONZERO && possibleRawWb
    rs2RawWb := RS2 === writeback(RD) && RS2_NONZERO && possibleRawWb
  }

  // Inject either the sync read or the forwarded RAW hazard into execute stage.
  when(execute.up.isFiring) {
    execute.bypass(RS1D) := rs1RawMem ? memory(RDD) | (rs1RawWb ? writeback(RDD) | rs1d)
    execute.bypass(RS2D) := rs2RawMem ? memory(RDD) | (rs2RawWb ? writeback(RDD) | rs2d)
  }

  val wb = new writeback.Area {
    regfile.write(RD, RDD, isValid && ENABLE_WB)
  }
}
