package curlyrv.ops

import spinal.core._
import spinal.lib.misc.pipeline._

import curlyrv.CpuBase

/** Daza Hazard Forwarding Operation
  *
  * Implements the forwarding network for the read after write (RAW) hazards in
  * the CPU pipeline.
  *
  * Following RAW hazards are detected in ID stage:
  *  - ID(RSx) == EX(RD) ?
  *  - ID(RSx) == MEM(RD) ?
  *  - ID(RSx) == WB(RD) ?
  *
  * @param cpu CPU to which the forwarding network is added
  */
case class DataHazardForwardingOps(cpu: CpuBase) extends CpuBaseOps {
  import cpu.pipes._
  import cpu.config._

  val RS1_HAZARD, RS2_HAZARD = Payload(Bool())
  val RS1_FORWARD, RS2_FORWARD = Payload(SInt(XLEN bits))

  val decoder = new decode.Area {
    // Check for RAW hazards of RSx and RD of previous instructions that are
    // still in-flight in EX, MEM and WB stages and not yet commited to regfile.
    def isRAWHazard(stage: CtrlLink, rs: Payload[UInt], rsZero: Payload[Bool]) = {
      isValid && stage.isValid && !rsZero && stage(WRITE_RD) && rs === stage(RD)
    }

    val isRs1RawEx = isRAWHazard(execute, RS1, RS1_ZERO)
    val isRs1RawMem = isRAWHazard(memory, RS1, RS1_ZERO)
    val isRs1RawWb = isRAWHazard(writeback, RS1, RS1_ZERO)
    RS1_HAZARD := isRs1RawEx || isRs1RawMem || isRs1RawWb

    val rs1dForwarded = SInt(XLEN bits)
    rs1dForwarded := writeback(RDD)
    when(isRs1RawMem) { rs1dForwarded := memory(RDD) }
    when(isRs1RawEx) { rs1dForwarded := execute(RDD) }
    RS1_FORWARD := rs1dForwarded

    val isRs2RawEx = isRAWHazard(execute, RS2, RS2_ZERO)
    val isRs2RawMem = isRAWHazard(memory, RS2, RS2_ZERO)
    val isRs2RawWb = isRAWHazard(writeback, RS2, RS2_ZERO)
    RS2_HAZARD := isRs2RawEx || isRs2RawMem || isRs2RawWb

    val rs2dForwarded = SInt(XLEN bits)
    rs2dForwarded := writeback(RDD)
    when(isRs2RawMem) { rs2dForwarded := memory(RDD) }
    when(isRs2RawEx) { rs2dForwarded := execute(RDD) }
    RS2_FORWARD := rs2dForwarded
  }

  val executer = new execute.Area {
    when(RS1_HAZARD) {
      bypass(RS1D) := RS1_FORWARD
    }
    when(RS2_HAZARD) {
      bypass(RS2D) := RS2_FORWARD
    }
  }
}
