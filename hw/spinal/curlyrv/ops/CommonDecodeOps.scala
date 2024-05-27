package curlyrv.ops

import spinal.core._

import curlyrv.CpuBase

/** Common Decoding Operation
  *
  * Decodes the fetched IR into OPCODE, RD, RS1/2 and FUNC3/7.
  *
  * @param cpu CPU to which the decode op is added
  */
case class CommonDecodeOps(cpu: CpuBase) extends CpuBaseOps {
  import cpu.pipes._
  import cpu.config._

  val decoder = new decode.Area {
    OPCODE := IR(6 downto 0)
    RD := U(IR(11 downto 7)) // destination register
    RS1 := U(IR(19 downto 15)) // source register 1
    RS2 := U(IR(24 downto 20)) // source register 2
    FUNC3 := IR(14 downto 12)
    FUNC7 := IR(31 downto 25)
  }
}
