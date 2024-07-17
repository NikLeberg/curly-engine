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
  * RAW data hazards are not handled here! Another Ops is required to solve it.
  *
  * @param cpu CPU to which the registerfile is added
  */
case class RegfileOps(cpu: CpuBase) extends CpuBaseOps {
  import cpu.pipes._
  import cpu.config._

  val RS1_NONZERO, RS2_NONZERO = Payload(Bool())
  val regfile = Mem(SInt(XLEN bits), 32)

  val decoder = new decode.Area {
    RS1_NONZERO := RS1 =/= 0
    RS2_NONZERO := RS2 =/= 0

    RS1D := RS1_NONZERO ? regfile.readAsync(RS1) | 0
    RS2D := RS2_NONZERO ? regfile.readAsync(RS2) | 0
  }

  val wb = new writeback.Area {
    regfile.write(RD, RDD, isValid && ENABLE_WB)
  }
}
