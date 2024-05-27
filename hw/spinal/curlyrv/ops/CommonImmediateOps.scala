package curlyrv.ops

import spinal.core._
import spinal.lib._

import curlyrv.CpuBase
import curlyrv.InstructionTypes

/** Common Immediate Generation Operation
  *
  * Inserts the appropriate immediate value (as IMM) based on the instruction
  * type of OPCODE. The association between OPCODEs and iTypes is based on the
  * previously registered instructions on the cpu via cpu.addInstruction().
  *
  * @param cpu CPU to which the immediate op is added
  */
case class CommonImmediateOps(cpu: CpuBase) extends CpuBaseOps {
  import cpu.pipes._
  import cpu.config._

  override def build(): Unit = {
    val decoder = new decode.Area {
      val imm = Bits(XLEN bits)
      switch(apply(OPCODE)) {
        import InstructionTypes._
        is(cpu.getOpcodeMask(instSType)) {
          imm(4 downto 0) := IR(11 downto 7)
          imm(10 downto 5) := IR(30 downto 25)
          imm(XLEN - 1 downto 11).setAllTo(IR(XLEN - 1))
        }
        is(cpu.getOpcodeMask(instBType)) {
          imm(0) := False
          imm(4 downto 1) := IR(11 downto 8)
          imm(10 downto 5) := IR(30 downto 25)
          imm(11) := IR(7)
          imm(XLEN - 1 downto 12).setAllTo(IR(XLEN - 1))
        }
        is(cpu.getOpcodeMask(instUType)) {
          imm(11 downto 0).setAllTo(False)
          imm(31 downto 12) := IR(31 downto 12)
        }
        is(cpu.getOpcodeMask(instJType)) {
          imm(0) := False
          imm(10 downto 1) := IR(30 downto 21)
          imm(11) := IR(20)
          imm(19 downto 12) := IR(19 downto 12)
          imm(XLEN - 1 downto 20).setAllTo(IR(XLEN - 1))
        }
        default { // I-Type or any other
          imm(10 downto 0) := IR(30 downto 20)
          imm(XLEN - 1 downto 11).setAllTo(IR(XLEN - 1))
        }
      }
      IMM := S(imm)
    }
  }
}
