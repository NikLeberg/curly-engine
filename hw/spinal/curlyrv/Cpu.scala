package curlyrv

import spinal.core._
import spinal.lib._

import scala.collection.mutable.ArrayBuffer

import curlyrv.ops._

/** CPU configuration options.
  */
case class CpuConfig() {

  /** Data width of CPU, fixed at 32 bits. */
  final val XLEN: Int = 32
}

class CpuBase(val config: CpuConfig) extends Component with CpuOpsStore with InstructionStore {
  val pipes = CpuPipelines(config)

  def build() {
    buildOps()
    pipes.build()
  }
}

case class Cpu(c: CpuConfig) extends CpuBase(c) {
  import config._

  val io = new Bundle {
    val iBus = master port IFetchBus(config)
  }

  addOp(IFetchOps(this, io.iBus))
  addOp(CommonDecodeOps(this))
  addOp(RegfileOps(this))
  addOp(CommonImmediateOps(this))
  // addOp(IntegerAluOps(this))

  // TEMP START
  addOp(PcPlus4Ops(this))

  addInstruction(M"1100111", M"000", M"0000000", InstructionTypes.instIType)
  addInstruction(M"0000011", M"000", M"0000000", InstructionTypes.instIType)
  addInstruction(M"0010011", M"000", M"0000000", InstructionTypes.instIType)

  addInstruction(M"0100011", M"000", M"0000000", InstructionTypes.instSType)
  addInstruction(M"0110011", M"000", M"0000000", InstructionTypes.instSType)

  addInstruction(M"1100011", M"000", M"0000000", InstructionTypes.instBType)

  addInstruction(M"0110111", M"000", M"0000000", InstructionTypes.instUType)
  addInstruction(M"0010111", M"000", M"0000000", InstructionTypes.instUType)

  addInstruction(M"1101111", M"000", M"0000000", InstructionTypes.instJType)
  // TEMP END

  this.build()
}
