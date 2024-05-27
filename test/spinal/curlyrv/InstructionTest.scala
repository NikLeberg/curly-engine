package curlyrv

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfter

import spinal.core._

case class InstructionStoreHarness() extends InstructionStore

class InstructionStoreTest extends AnyFunSuite with BeforeAndAfter {
  var harness: InstructionStoreHarness = null

  before {
    harness = InstructionStoreHarness()
  }

  test("InstructionStore.getOpcodeMask get mask of each instruction type") {
    import InstructionTypes._
    for (iType <- Seq(instRType, instIType, instSType, instBType, instUType, instJType)) {
      harness.addInstruction(M"0000000", M"000", M"0000000", iType)
      assert(harness.getOpcodeMask(iType) == M"0000000")
    }
  }

  test("InstructionStore.getOpcodeMask combined mask") {
    harness.addInstruction(M"0000000", M"000", M"0000000", InstructionTypes.instRType)
    harness.addInstruction(M"0000001", M"000", M"0000000", InstructionTypes.instRType)
    assert(harness.getOpcodeMask(InstructionTypes.instRType) == M"000000-")

    harness.addInstruction(M"1100111", M"000", M"0000000", InstructionTypes.instIType)
    harness.addInstruction(M"0000011", M"000", M"0000000", InstructionTypes.instIType)
    assert(harness.getOpcodeMask(InstructionTypes.instIType) == M"--00-11")
  }
}
