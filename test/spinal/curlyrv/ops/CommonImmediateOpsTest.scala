package curlyrv.ops

import org.scalatest.funsuite.AnyFunSuite

import spinal.core._
import spinal.core.sim._
import spinal.core.formal._

import curlyrv._

case class CommonImmediateOpsHarness() extends CpuBase(CpuConfig()) {
  import config._
  import pipes._

  val io = new Bundle {
    val ir = in port Bits(XLEN bits)
    val imm = out port SInt(XLEN bits)
  }.simPublic()

  // DUT
  // input: Payload(OPCODE)
  // output: Payload(IMM)
  addOp(CommonImmediateOps(this))

  // build dummy pipeline around DUT to set and get payloads
  val decoder = new decode.Area {
    IR := io.ir
    OPCODE := io.ir(6 downto 0)
    io.imm := IMM
  }
  addInstruction(M"0000001", M"000", M"0000000", InstructionTypes.instBType)
  addInstruction(M"0000010", M"000", M"0000000", InstructionTypes.instIType)
  addInstruction(M"0000100", M"000", M"0000000", InstructionTypes.instJType)
  addInstruction(M"0001000", M"000", M"0000000", InstructionTypes.instRType)
  addInstruction(M"0010000", M"000", M"0000000", InstructionTypes.instSType)
  addInstruction(M"0100000", M"000", M"0000000", InstructionTypes.instUType)
  build()
}

class CommonImmediateOpsSim extends AnyFunSuite {
  var compiled: SimCompiled[CommonImmediateOpsHarness] = null

  test("CommonImmediateOps.Sim.Compile") {
    compiled = Config.sim.compile(CommonImmediateOpsHarness())
  }

  test("CommonImmediateOps.Sim.Run") {
    compiled.doSim(seed = 2) { dut =>
      dut.clockDomain.doStimulus(10)
      dut.clockDomain.waitSampling(1)

      val vectors = Array(
        // iType, IR, expected IMM
        ("B", 0x11111101L, 258),
        ("I", 0x11111102L, 273),
        ("J", 0x11111104L, 71952),
        ("R", 0x11111108L, 273),
        ("S", 0x11111110L, 258),
        ("U", 0x11111120L, 286330880),
        ("B", 0x81111101L, -4094),
        ("I", 0x81111102L, -2031),
        ("J", 0x81111104L, -976880),
        ("R", 0x81111108L, -2031),
        ("S", 0x81111110L, -2046),
        ("U", 0x81111120L, -2129588224)
      )
      for ((iType, ir, exImm) <- vectors) {
        dut.io.ir #= ir
        dut.clockDomain.waitRisingEdge(1)
        val imm = dut.io.imm.toInt
        assert(imm == exImm, s"IMM($iType-Type): got $imm, expected $exImm")
      }
    }
  }
}

class CommonImmediateOpsFormal extends AnyFunSuite {
  test("CommonImmediateOps.Formal") {
    Config.formal
      .withCover()
      .doVerify(new Component {
        setDefinitionName("CommonImmediateOpsFormal")
        val dut = FormalDut(CommonImmediateOpsHarness())

        assumeInitial(dut.clockDomain.isResetActive)
        val initDone = !initstate() && !dut.clockDomain.isResetActive

        anyseq(dut.io.ir)

        // full range of immediates can be acheived
        val isBType = dut.io.ir(6 downto 0) === B"7'x01"
        cover(initDone && isBType && dut.io.imm === S"32'xfffff000") // -4096
        cover(initDone && isBType && dut.io.imm === S"32'xfffffffe") // -2
        cover(initDone && isBType && dut.io.imm === S"32'x00000000") // 0
        cover(initDone && isBType && dut.io.imm === S"32'x00000ffe") // 4094

        val isIType = dut.io.ir(6 downto 0) === B"7'x02"
        cover(initDone && isIType && dut.io.imm === S"32'xfffff800") // -2048
        cover(initDone && isIType && dut.io.imm === S"32'xffffffff") // -1
        cover(initDone && isIType && dut.io.imm === S"32'x00000000") // 0
        cover(initDone && isIType && dut.io.imm === S"32'x000007ff") // 2047

        val isJType = dut.io.ir(6 downto 0) === B"7'x04"
        cover(initDone && isJType && dut.io.imm === S"32'xffffe000") // -8192
        cover(initDone && isJType && dut.io.imm === S"32'xfffffffe") // -2
        cover(initDone && isJType && dut.io.imm === S"32'x00000000") // 0
        cover(initDone && isJType && dut.io.imm === S"32'x00001ffe") // 8190

        val isRType = dut.io.ir(6 downto 0) === B"7'x08"
        cover(initDone && isRType && dut.io.imm === S"32'xfffff800") // -2048
        cover(initDone && isRType && dut.io.imm === S"32'xffffffff") // -1
        cover(initDone && isRType && dut.io.imm === S"32'x00000000") // 0
        cover(initDone && isRType && dut.io.imm === S"32'x000007ff") // 2047

        val isSType = dut.io.ir(6 downto 0) === B"7'x10"
        cover(initDone && isSType && dut.io.imm === S"32'xfffff800") // -2048
        cover(initDone && isSType && dut.io.imm === S"32'xffffffff") // -1
        cover(initDone && isSType && dut.io.imm === S"32'x00000000") // 0
        cover(initDone && isSType && dut.io.imm === S"32'x000007ff") // 2047

        val isUType = dut.io.ir(6 downto 0) === B"7'x20"
        cover(initDone && isUType && dut.io.imm === S"32'x80000000") // -2147483648
        cover(initDone && isUType && dut.io.imm === S"32'xfffff000") // -4096
        cover(initDone && isUType && dut.io.imm === S"32'x00001000") // 4096
        cover(initDone && isUType && dut.io.imm === S"32'x40000000") // 1073741824
      })
  }
}
