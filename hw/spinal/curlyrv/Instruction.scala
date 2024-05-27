package curlyrv

import spinal.core._

import scala.collection.mutable.ArrayBuffer

object InstructionTypes extends SpinalEnum {
  val instRType, instIType, instSType, instBType, instUType, instJType = newElement()
}

case class Instruction(
    opcode: MaskedLiteral,
    funct3: MaskedLiteral,
    funct7: MaskedLiteral,
    iType: InstructionTypes.E
) {}

/** Instruction Registrar/Store Trait
  *
  * Meant to be attached to an CPU such that the various systems of the CPU can
  * register their implemented instructions i.e. OPCODE(s). Other systems can
  * then query the registered instructions via the getXXX() calls.
  */
trait InstructionStore {
  val instructions = ArrayBuffer[Instruction]()
  def addInstruction(instruction: Instruction): Unit = { instructions += instruction }
  def addInstruction(
      opcode: MaskedLiteral,
      funct3: MaskedLiteral,
      funct7: MaskedLiteral,
      iType: InstructionTypes.E
  ): Unit = { addInstruction(Instruction(opcode, funct3, funct7, iType)) }

  /** Based on all registered instructions in the CPU, get the opcode mask that
    * corresponds to matching the given instruction type.
    *
    * E.g. If I-type instructions B"1100111" and B"0000011" are registered, this
    * returns the mask M"--00-11".
    *
    * @param iType The instruction type
    * @return MaskedLiteral of all known opcodes for this type.
    */
  def getOpcodeMask(iType: InstructionTypes.E): MaskedLiteral = {
    val opcodes = instructions.filter(_.iType == iType).map(_.opcode)
    def getOpcodeString(n: Int): String = opcodes(n).getBitsString(7, '-')
    // init mask with first entry
    val mask = getOpcodeString(0).toCharArray()
    // add other entries to it
    for (i <- 1 to opcodes.length - 1) {
      for ((c, j) <- getOpcodeString(i).zipWithIndex) {
        if (mask(j) != c) {
          mask(j) = '-'
        }
      }
    }
    MaskedLiteral(mask.mkString(""))
  }
}
