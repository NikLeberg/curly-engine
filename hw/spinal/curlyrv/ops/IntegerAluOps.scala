package curlyrv.ops

import spinal.core._
import spinal.lib.misc.pipeline._

import curlyrv.Cpu

object AluOps extends SpinalEnum {
  val opAdd, opSub, opSlt, opSltu, opXor, opOr, opAnd = newElement()
}

case class IntegerAluOps(cpu: Cpu) extends CpuBaseOps {
  import cpu.pipes._
  import cpu.config._

  val ALU_OP = Payload(AluOps())
  val ALU_USE_IMM = Payload(Bool())

  val decoder = new decode.Area {
    import AluOps._
    val op = AluOps()
    val useImm = Bool()
    // when(apply(OPCODE) === M"0-10011") {
    //   switch(apply(FUNC3)) {
    //     is(B"000") { op := FUNC7(5) ? opSub | opAdd }
    //     is(B"001") { op := opAdd } // todo: SLL
    //     is(B"010") { op := opSlt }
    //     is(B"011") { op := opSltu }
    //     is(B"100") { op := opXor }
    //     is(B"101") { op := opAdd } // todo: SRL & SRA
    //     is(B"110") { op := opOr }
    //     is(B"111") { op := opAnd }
    //   }
    //   useImm := !OPCODE(5)
    // }.otherwise {
    //   op := opAdd // default to add with imm so address generation can use ALU
    //   useImm := True
    // }
    ALU_OP := op
    ALU_USE_IMM := useImm
  }

  val executer = new execute.Area {
    val zero = S(0, 32 bits)
    val one = S(1, 32 bits)
    val a = apply(RS1D)
    val b = apply(IMM) // BOOL_PAYLOAD ? IMM | apply(RS2D)

    import AluOps._
    RDD := ALU_OP.mux(
      opAdd -> (a + b),
      opSub -> (a - b),
      // opSll -> (a |<< U(b(4 downto 0))),
      opSlt -> ((a < b) ? one | zero),
      opSltu -> ((U(a) < U(b)) ? one | zero),
      opXor -> (a ^ b),
      opOr -> (a | b),
      opAnd -> (a & b)
    )
  }
}
