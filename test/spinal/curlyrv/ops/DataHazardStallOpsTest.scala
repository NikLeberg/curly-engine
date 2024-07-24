package curlyrv.ops

import org.scalatest.funsuite.AnyFunSuite

import spinal.core._
import spinal.core.sim._
import spinal.core.formal._

import curlyrv._

case class DataHazardStallOpsHarness() extends CpuBase(CpuConfig()) {
  import config._
  import pipes._

  val io = new Bundle {
    val enRs1, enRs2, enRd = in port Bool()
    val rs1, rs2, rd = in port UInt(5 bits)
    val stalled = out port Bool()
  }.simPublic()

  // DUT
  // input: Payload(RSx, RD, READ_RSx, WRITE_RD)
  // output: stalled
  addOp(DataHazardStallOps(this))

  // build dummy pipeline around DUT to set and get payloads
  val decoder = new decode.Area {
    READ_RS1 := io.enRs1
    READ_RS2 := io.enRs2
    WRITE_RD := io.enRd
    RS1 := io.rs1
    RS2 := io.rs2
    RD := io.rd

    RS1_ZERO := io.rs1 === 0 // usually done in regfile
    RS2_ZERO := io.rs2 === 0
  }

  io.stalled := !fetch.isReady

  build()
}

class DataHazardStallOpsSim extends AnyFunSuite {
  var compiled: SimCompiled[DataHazardStallOpsHarness] = null

  test("DataHazardStallOps.Sim.Compile") {
    compiled = Config.sim.compile(DataHazardStallOpsHarness())
  }

  test("DataHazardStallOps.Sim.Run") {
    compiled.doSim(seed = 2) { dut =>
      dut.clockDomain.doStimulus(10)
      dut.clockDomain.waitSampling(1)

      // Simulate sequence to read a register through rs1.
      def simRs1(reg: Int): Unit = {
        dut.io.enRs1 #= true
        dut.io.rs1 #= reg
        dut.clockDomain.waitRisingEdge(1)
        dut.io.enRs1 #= false
        dut.io.rs1 #= 0
      }

      // Simulate sequence to read a register through rs2.
      def simRs2(reg: Int): Unit = {
        dut.io.enRs2 #= true
        dut.io.rs2 #= reg
        dut.clockDomain.waitRisingEdge(1)
        dut.io.enRs2 #= false
        dut.io.rs2 #= 0
      }

      // Simulate sequence to write a register.
      def simRd(reg: Int): Unit = {
        dut.io.enRd #= true
        dut.io.rd #= reg
        dut.clockDomain.waitRisingEdge(1)
        dut.io.enRd #= false
        dut.io.rd #= 0
      }

      // Check if pipeline is stalled.
      def isStalled: Boolean = dut.io.stalled.toBoolean

      // wait for whole pipeline to be valid (is actually filled with garbage)
      dut.clockDomain.waitRisingEdge(5)
      assert(!isStalled)

      for (r <- 1 to 31) {
        // read rs1, no stall
        simRs1(r)
        assert(!isStalled)

        // second rs1 read, no stall
        simRs1(r)
        assert(!isStalled)

        // rd write, no stall
        simRd(r)
        assert(!isStalled)

        // dependent rs1 read, stalls as long as write has not passed WB
        simRs1(r)
        assert(isStalled) // write in EX
        simRs1(r) // reapply, otherwise ID forgets instruction
        assert(isStalled) // write in MEM
        simRs1(r)
        assert(isStalled) // write in WB

        // rs1 read, no stall
        simRs1(r)
        assert(!isStalled)
      }

      for (r <- 1 to 31) {
        // read rs2, no stall
        simRs2(r)
        assert(!isStalled)

        // second rs2 read, no stall
        simRs2(r)
        assert(!isStalled)

        // rd write, no stall
        simRd(r)
        assert(!isStalled)

        // dependent rs2 read, stalls as long as write has not passed WB
        simRs2(r)
        assert(isStalled) // write in EX
        simRs2(r) // reapply, otherwise ID forgets instruction
        assert(isStalled) // write in MEM
        simRs2(r)
        assert(isStalled) // write in WB

        // rs2 read, no stall
        simRs2(r)
        assert(!isStalled)
      }
    }
  }
}
