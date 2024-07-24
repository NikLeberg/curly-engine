package curlyrv.ops

import spinal.core._
import spinal.lib.misc.pipeline._

import curlyrv.CpuBase

/** Daza Hazard Stall Operation
  *
  * Detects read after write (RAW) hazards and solves them by stalling the
  * dependent instruction.
  *
  * Following RAW hazards are detected in ID stage:
  *  - ID(RSx) == EX(RD) ?
  *  - ID(RSx) == MEM(RD) ?
  *  - ID(RSx) == WB(RD) ?
  *
  * @param cpu CPU to which the RAW stall is added
  */
case class DataHazardStallOps(cpu: CpuBase) extends CpuBaseOps {
  import cpu.pipes._
  import cpu.config._

  val decoder = new decode.Area {
    // Check for RAW hazards of RSx and RD of previous instructions that are
    // still in-flight in EX, MEM and WB stages and not yet commited to regfile.
    def isRAWHazard(stage: CtrlLink, i: Int) = {
      val rs: Payload[UInt] = Array(RS1, RS2).apply(i-1)
      val rsZero: Payload[Bool] = Array(RS1_ZERO, RS2_ZERO).apply(i-1)
      val rsRead: Payload[Bool] = Array(READ_RS1, READ_RS2).apply(i-1)
      isValid && stage.isValid && rsRead && stage(WRITE_RD) && !rsZero && rs === stage(RD)
    }

    // detect
    val isRs1RawEx = isRAWHazard(execute, 1)
    val isRs1RawMem = isRAWHazard(memory, 1)
    val isRs1RawWb = isRAWHazard(writeback, 1)

    val isRs2RawEx = isRAWHazard(execute, 2)
    val isRs2RawMem = isRAWHazard(memory, 2)
    val isRs2RawWb = isRAWHazard(writeback, 2)

    // stall
    haltWhen(
      isRs1RawEx || isRs1RawMem || isRs1RawWb ||
      isRs2RawEx || isRs2RawMem || isRs2RawWb
    )
  }
}
