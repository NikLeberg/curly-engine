package curlyrv.ops

import spinal.core._
import spinal.lib.misc.pipeline._

import curlyrv.CpuBase
import curlyrv.InstructionTypes

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

  val regfile = Mem(SInt(XLEN bits), 32)

  val decoder = new decode.Area {
    RS1_ZERO := RS1 === 0
    RS2_ZERO := RS2 === 0

    RS1D := !RS1_ZERO ? regfile.readAsync(RS1) | 0
    RS2D := !RS2_ZERO ? regfile.readAsync(RS2) | 0
  }

  val wb = new writeback.Area {
    regfile.write(RD, RDD, isValid && WRITE_RD)
  }

  override def build(): Unit = {
    val decoder = new decode.Area {
      val readRs1, readRs2, writeRd = Bool()
      switch(apply(OPCODE)) {
        import InstructionTypes._
        is(cpu.getOpcodeMask(instIType)) {
          readRs1 := True
          readRs2 := False
          writeRd := True
        }
        is(cpu.getOpcodeMask(instSType)) {
          readRs1 := True
          readRs2 := True
          writeRd := False
        }
        is(cpu.getOpcodeMask(instBType)) {
          readRs1 := True
          readRs2 := True
          writeRd := False
        }
        is(cpu.getOpcodeMask(instUType)) {
          readRs1 := False
          readRs2 := False
          writeRd := True
        }
        is(cpu.getOpcodeMask(instJType)) {
          readRs1 := False
          readRs2 := False
          writeRd := True
        }
        default { // R-Type or any other
          readRs1 := True
          readRs2 := True
          writeRd := True
        }
      }
      READ_RS1 := readRs1
      READ_RS2 := readRs2
      WRITE_RD := writeRd
    }
  }
}
