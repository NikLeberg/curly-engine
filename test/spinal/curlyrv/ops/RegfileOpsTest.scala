package curlyrv.ops

import org.scalatest.funsuite.AnyFunSuite

import spinal.core._
import spinal.core.sim._
import spinal.core.formal._

import curlyrv._

case class RegfileOpsHarness() extends CpuBase(CpuConfig()) {
  import config._
  import pipes._

  val io = new Bundle {
    val rd, rs1, rs2 = in port UInt(5 bits)
    val rdd = in port SInt(XLEN bits)
    val wb = in port Bool
    val rs1d, rs2d = out port SInt(XLEN bits)
  }.simPublic()

  // DUT
  // input: Payload(RS1/2, RD)
  // output: Payload(RS1D/2D, RDD)
  val regfileOps = RegfileOps(this)
  addOp(regfileOps)
  regfileOps.regfile.simPublic()

  // build dummy pipeline around DUT to set and get payloads
  val decoder = new decode.Area {
    RS1 := io.rs1
    // RS1D := 0
    RS2 := io.rs2
    // RS2D := 0
    RD := io.rd
    RDD := io.rdd
    ENABLE_WB := io.wb
  }
  val executer = new execute.Area {
    io.rs1d := RS1D
    io.rs2d := RS2D
  }
  build()
}

class RegfileOpsSim extends AnyFunSuite {
  var compiled: SimCompiled[RegfileOpsHarness] = null

  test("RegfileOps.Sim.Compile") {
    compiled = Config.sim.compile(RegfileOpsHarness())
  }

  test("RegfileOps.Sim.Run") {
    compiled.doSim(seed = 2) { dut =>
      dut.clockDomain.doStimulus(10)
      dut.clockDomain.waitSampling(1)

      // Get/set values direcly in the registerfile.
      def getReg(reg: Int): Int = { dut.regfileOps.regfile.getBigInt(reg).toInt }
      def setReg(reg: Int, value: BigInt): Unit = { dut.regfileOps.regfile.setBigInt(reg, value) }

      // Simulate sequence to read a register through rs1.
      def readRs1(reg: Int): Int = {
        dut.io.rs1 #= reg
        dut.clockDomain.waitRisingEdge(2) // decode + execute
        dut.io.rs1d.toInt
      }

      // Simulate sequence to read a register through rs2.
      def readRs2(reg: Int): Int = {
        dut.io.rs2 #= reg
        dut.clockDomain.waitRisingEdge(2) // decode + execute
        dut.io.rs2d.toInt
      }

      // Simulate sequence to write a register.
      def writeRd(reg: Int, value: BigInt, wait: Boolean = true): Unit = {
        dut.io.rd #= reg
        dut.io.rdd #= value
        dut.io.wb #= true
        if (wait) {
          dut.clockDomain.waitRisingEdge(5) // decode, execute, memory, writeback, sync
        }
      }

      // // x0 reads always as zero
      // val rs1d = readRs1(0)
      // assert(rs1d == 0, s"RS1(reg x0): read $rs1d, expected 0")
      // val rs2d = readRs2(0)
      // assert(rs2d == 0, s"RS2(reg x0): read $rs2d, expected 0")

      // // can read registers 1 to 31 through rs1
      // for (i <- 1 to 31) {
      //   val rs1d = readRs1(i)
      //   val ex = getReg(i)
      //   assert(rs1d == ex, s"RS1(reg x$i): read $rs1d, expected $ex")
      // }

      // // can read registers 1 to 31 through rs2
      // for (i <- 1 to 31) {
      //   val rs2d = readRs2(i)
      //   val ex = getReg(i)
      //   assert(rs2d == ex, s"RS2(reg x$i): read $rs2d, expected $ex")
      // }

      // // can write to register x0, but either read returns 0
      // writeRd(0, 0xabababab)
      // assert(readRs1(0) == 0, s"x0 is expected to be constant zero")
      // assert(readRs2(0) == 0, s"x0 is expected to be constant zero")

      // // can write to registers 1 to 31
      // for (i <- 1 to 31) {
      //   writeRd(i, 0xabababab)
      //   val rdd = getReg(i)
      //   assert(rdd == 0xabababab, s"RD(reg x$i): write 0xabababab, but read $rdd")
      // }

      // // can write to and then read from registers 1 to 31
      // for (i <- 1 to 31) {
      //   writeRd(i, i)
      //   val rs1 = readRs1(i)
      //   assert(rs1 == i, s"RD+RS1(reg x$i): write $i, but read $rs1")
      //   val rs2 = readRs2(i)
      //   assert(rs2 == i, s"RD+RS2(reg x$i): write $i, but read $rs2")
      // }

      // // RAW hazard between EX <-> MEM stages is solved with forwarding
      // writeRd(1, 0x12345678, wait = false)
      // dut.clockDomain.waitRisingEdge(1) // decode
      // dut.io.rd #= 0
      // dut.io.wb #= false
      // var rs1 = readRs1(1)
      // assert(rs1 == 0x12345678, s"RS1: RAW hazard between EX<->MEM stages not solved, read $rs1")
      // writeRd(1, 0x87654321, wait = false)
      // dut.clockDomain.waitRisingEdge(1) // decode
      // dut.io.rd #= 0
      // dut.io.wb #= false
      // var rs2 = readRs2(1)
      // assert(rs2 == 0x87654321, s"RS2: RAW hazard between EX<->MEM stages not solved, read $rs2")

      // // RAW hazard between EX <-> WB stages is solved with forwarding
      // writeRd(1, 0x12345678, wait = false)
      // dut.clockDomain.waitRisingEdge(1) // decode
      // dut.io.rd #= 0
      // dut.io.wb #= false
      // dut.clockDomain.waitRisingEdge(1) // delay
      // rs1 = readRs1(1)
      // assert(rs1 == 0x12345678, s"RS1: RAW hazard between EX<->WB stages not solved, read $rs1")
      // writeRd(1, 0x87654321, wait = false)
      // dut.clockDomain.waitRisingEdge(1) // decode
      // dut.io.rd #= 0
      // dut.io.wb #= false
      // dut.clockDomain.waitRisingEdge(1) // delay
      // rs2 = readRs2(1)
      // assert(rs2 == 0x87654321, s"RS2: RAW hazard between EX<->WB stages not solved, read $rs2")

      // RAW hazard of instructions two slots apart are solved via the regfile
      writeRd(1, 0x12345678, wait = false)
      dut.clockDomain.waitRisingEdge(1) // decode
      dut.io.rd #= 0
      dut.io.wb #= false
      dut.clockDomain.waitRisingEdge(2) // delay
      var rs1 = readRs1(1)
      assert(rs1 == 0x12345678, s"RS1: RAW hazard with two delays not solved, read $rs1")
      writeRd(1, 0x87654321, wait = false)
      dut.clockDomain.waitRisingEdge(1) // decode
      dut.io.rd #= 0
      dut.io.wb #= false
      dut.clockDomain.waitRisingEdge(2) // delay
      var rs2 = readRs2(1)
      assert(rs2 == 0x87654321, s"RS2: RAW hazard with two delays not solved, read $rs2")

      // RAW hazard of instructions three slots apart are solved via the regfile
      writeRd(1, 0x12345678, wait = false)
      dut.clockDomain.waitRisingEdge(1) // decode
      dut.io.rd #= 0
      dut.io.wb #= false
      dut.clockDomain.waitRisingEdge(3) // delay
      rs1 = readRs1(1)
      assert(rs1 == 0x12345678, s"RS1: RAW hazard with three delays not solved, read $rs1")
      writeRd(1, 0x87654321, wait = false)
      dut.clockDomain.waitRisingEdge(1) // decode
      dut.io.rd #= 0
      dut.io.wb #= false
      dut.clockDomain.waitRisingEdge(3) // delay
      rs2 = readRs2(1)
      assert(rs2 == 0x87654321, s"RS2: RAW hazard with three delays not solved, read $rs2")
    }
  }
}

class RegfileOpsFormal extends AnyFunSuite {
  test("RegfileOps.Formal") {
    Config.formal.withSymbiYosysAndGhdl
      .withCover()
      .doVerify(new Component {
        setDefinitionName("RegfileOpsFormal")
        // val dut = FormalDut(RegfileOpsHarness())
        val dut = RegfileOpsHarness()

        assumeInitial(dut.clockDomain.isResetActive)
        // val initDone = !initstate() && !dut.clockDomain.isResetActive

        // anyseq(dut.io.rs1)
        // anyseq(dut.io.rs2)
        // anyseq(dut.io.rd)
        // anyseq(dut.io.rdd)
        // anyseq(dut.io.wb)
        // dut.io.rs1 := 0
        // dut.io.rs2 #= 0
        // dut.io.rd #= 0
        // dut.io.rdd #= 0
        // dut.io.wb #= false

        // cover(
        //   initDone &&
        //     dut.io.rs1 === dut.io.rd && dut.io.rs1d === dut.io.rdd &&
        //     dut.io.rs1 =/= 0 && dut.io.rs1d =/= 0
        // )
      })
  }
}
