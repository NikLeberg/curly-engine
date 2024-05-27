package curlyrv.ops

import org.scalatest.funsuite.AnyFunSuite

import spinal.core._
import spinal.core.sim._
import spinal.core.formal._

import curlyrv._

case class CommonDecodeOpsHarness() extends CpuBase(CpuConfig()) {
  import config._
  import pipes._

  val io = new Bundle {
    val ir = in port Bits(XLEN bits)
    val opcode = out port Bits(7 bits)
    val rd, rs1, rs2 = out port UInt(5 bits)
    val func3 = out port Bits(3 bits)
    val func7 = out port Bits(7 bits)
  }.simPublic()

  // DUT
  // input: Payload(IR)
  // output: Payload(RD, RS1/2, FUNC3/7)
  addOp(CommonDecodeOps(this))

  // build dummy pipeline around DUT to set and get payloads
  fetch(IR) := io.ir
  val decoder = new decode.Area {
    io.opcode := OPCODE
    io.rd := RD
    io.rs1 := RS1
    io.rs2 := RS2
    io.func3 := FUNC3
    io.func7 := FUNC7
  }
  build()
}

class CommonDecodeOpsSim extends AnyFunSuite {
  var compiled: SimCompiled[CommonDecodeOpsHarness] = null

  test("CommonDecodeOps.Sim.Compile") {
    compiled = Config.sim.compile(CommonDecodeOpsHarness())
  }

  test("CommonDecodeOps.Sim.Run") {
    compiled.doSim(seed = 2) { dut =>
      dut.clockDomain.doStimulus(10)
      dut.clockDomain.waitSampling(1)

      // Test OPCODE
      for (i <- 0 to 7) {
        dut.io.ir #= i
        dut.clockDomain.waitRisingEdge(1)
        sleep(1)
        val opcode = dut.io.opcode.toInt
        assert(opcode == i, s"OPCODE: got $opcode, expected $i")
      }

      // Test RD
      for (i <- 0 to 31) {
        dut.io.ir #= (i << 7)
        dut.clockDomain.waitRisingEdge(1)
        sleep(1)
        val rd = dut.io.rd.toInt
        assert(rd == i, s"RD: got $rd, expected $i")
      }

      // Test RS1
      for (i <- 0 to 31) {
        dut.io.ir #= (i << 15)
        dut.clockDomain.waitRisingEdge(1)
        sleep(1)
        val rs1 = dut.io.rs1.toInt
        assert(rs1 == i, s"RS1: got $rs1, expected $i")
      }

      // Test RS2
      for (i <- 0 to 31) {
        dut.io.ir #= (i << 20)
        dut.clockDomain.waitRisingEdge(1)
        sleep(1)
        val rs2 = dut.io.rs2.toInt
        assert(rs2 == i, s"RS2: got $rs2, expected $i")
      }

      // Test FUNC3
      for (i <- 0 to 7) {
        dut.io.ir #= (i << 12)
        dut.clockDomain.waitRisingEdge(1)
        sleep(1)
        val func3 = dut.io.func3.toInt
        assert(func3 == i, s"FUNC3: got $func3, expected $i")
      }

      // Test FUNC7
      for (i <- 0 to 7) {
        dut.io.ir #= (i << 25)
        dut.clockDomain.waitRisingEdge(1)
        sleep(1)
        val func7 = dut.io.func7.toInt
        assert(func7 == i, s"FUNC7: got $func7, expected $i")
      }
    }
  }
}

class CommonDecodeOpsFormal extends AnyFunSuite {
  test("CommonDecodeOps.Formal") {
    Config.formal
      .withCover()
      .doVerify(new Component {
        setDefinitionName("CommonDecodeOpsFormal")
        val dut = FormalDut(CommonDecodeOpsHarness())

        assumeInitial(dut.clockDomain.isResetActive)
        val initDone = !initstate() && !dut.clockDomain.isResetActive

        anyseq(dut.io.ir)

        cover(initDone && dut.io.opcode === B"7'x00")
        cover(initDone && dut.io.opcode === B"7'x7f")

        cover(initDone && dut.io.rd === U"5'x00")
        cover(initDone && dut.io.rd === U"5'x1f")

        cover(initDone && dut.io.rs1 === U"5'x00")
        cover(initDone && dut.io.rs1 === U"5'x1f")

        cover(initDone && dut.io.rs2 === U"5'x00")
        cover(initDone && dut.io.rs2 === U"5'x1f")

        cover(initDone && dut.io.func3 === B"3'x0")
        cover(initDone && dut.io.func3 === B"3'x3")

        cover(initDone && dut.io.func7 === B"7'x00")
        cover(initDone && dut.io.func7 === B"7'x7f")
      })
  }
}
