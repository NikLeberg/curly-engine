package curlyrv

import spinal.core._
import spinal.lib._
import spinal.lib.misc.pipeline._

/** Common and shared CPU pipeline stages.
  *
  * Provides the shared classical five CPU pipelines.
  * Defines the commonly used pipeline payloads like instruction register (IR),
  * program counter (PC) etc.
  */
case class CpuPipelines(config: CpuConfig) extends Area {
  import config._

  val fetch, decode, execute, memory, writeback = CtrlLink()

  val f2d = StageLink(fetch.down, decode.up)
  val d2e = StageLink(decode.down, execute.up)
  val e2m = StageLink(execute.down, memory.up)
  val m2w = StageLink(memory.down, writeback.up)

  def build(): Unit = {
    Builder(fetch, decode, execute, memory, writeback, f2d, d2e, e2m, m2w)
  }

  val PC = Payload(UInt(XLEN bits))
  val IR = Payload(Bits(32 bits))

  val OPCODE = Payload(Bits(7 bits))
  val RD, RS1, RS2 = Payload(UInt(5 bits)) // register numbers
  val FUNC3 = Payload(Bits(3 bits))
  val FUNC7 = Payload(Bits(7 bits))
  val IMM = Payload(SInt(XLEN bits))

  val RDD, RS1D, RS2D = Payload(SInt(XLEN bits)) // register values

  // true if instruction reads or writes from regfile
  val READ_RS1, READ_RS2, WRITE_RD = Payload(Bool())

  // set in ID stage whenever RSx != 0
  val RS1_ZERO, RS2_ZERO = Payload(Bool())
  
  val ALU_RES = Payload(Bits(XLEN bits))
}
