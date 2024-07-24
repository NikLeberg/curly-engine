package curlyrv.ops

import org.scalatest.funsuite.AnyFunSuite

import spinal.core._
import spinal.core.sim._
import spinal.core.formal._

import curlyrv._

case class DataHazardForwardingOpsHarness() extends CpuBase(CpuConfig()) {
  import config._
  import pipes._

  val io = new Bundle {
    val rs1, rs2, rd = in port UInt(5 bits)
    val rdd = in port SInt(XLEN bits)
    val wb = in port Bool()
    val haltEx, haltMem = in port Bool()
    val rs1d, rs2d = out port SInt(XLEN bits)
  }.simPublic()

  // DUT
  // input: Payload(RSx, RD, RDD, WB, haltX)
  // output: Payload(RSxD)
  addOp(DataHazardForwardingOps(this))

  // build dummy pipeline around DUT to set and get payloads
  val decoder = new decode.Area {
    RS1 := io.rs1
    RS2 := io.rs2
    RD := io.rd
    RDD := io.rdd
    WRITE_RD := io.wb

    RS1_ZERO := io.rs1 === 0 // usually done in regfile
    RS2_ZERO := io.rs2 === 0
    RS1D := 0 // required for bypass() in forwarder to work
    RS2D := 0
  }
  val executer = new execute.Area {
    io.rs1d := RS1D
    io.rs2d := RS2D
    haltWhen(io.haltEx)
  }
  val mem = new memory.Area {
    haltWhen(io.haltMem)
  }
  build()
}

class DataHazardForwardingOpsSim extends AnyFunSuite {
  var compiled: SimCompiled[DataHazardForwardingOpsHarness] = null

  test("DataHazardForwardingOps.Sim.Compile") {
    compiled = Config.sim.compile(DataHazardForwardingOpsHarness())
  }

  test("DataHazardForwardingOps.Sim.Run") {
    compiled.doSim(seed = 2) { dut =>
      dut.io.wb #= false
      dut.io.haltEx #= false
      dut.io.haltMem #= false

      dut.clockDomain.doStimulus(10)
      dut.clockDomain.waitSampling(1)

      // Simulate sequence to read a register through rs1.
      def simRs1(reg: Int): Unit = {
        dut.io.rs1 #= reg
        dut.clockDomain.waitRisingEdge(1)
        dut.io.rs1 #= 0
      }

      // Simulate sequence to read a register through rs2.
      def simRs2(reg: Int): Unit = {
        dut.io.rs2 #= reg
        dut.clockDomain.waitRisingEdge(1)
        dut.io.rs2 #= 0
      }

      // Simulate sequence to write a register.
      def simRd(reg: Int, value: BigInt): Unit = {
        dut.io.rd #= reg
        dut.io.rdd #= value
        dut.io.wb #= true
        dut.clockDomain.waitRisingEdge(1)
        dut.io.rd #= 0
        dut.io.rdd #= 0
        dut.io.wb #= false
      }

      def getRs1d: Int = dut.io.rs1d.toInt
      def getRs2d: Int = dut.io.rs2d.toInt

      for (r <- 1 to 31) {
        // RAW hazard between ID <-> EX stages is solved with forwarding
        simRd(r, 0x12345678)
        // no delay in-between
        simRs1(r)
        dut.clockDomain.waitRisingEdge(1) // wait for read to finish
        var rs1 = getRs1d
        assert(rs1 == 0x12345678, s"RS1: RAW hazard between ID<->EX stages not solved, read $rs1")
        simRd(r, 0x87654321)
        // no delay in-between
        simRs2(r)
        dut.clockDomain.waitRisingEdge(1) // wait for read to finish
        var rs2 = getRs2d
        assert(rs2 == 0x87654321, s"RS2: RAW hazard between ID<->EX stages not solved, read $rs2")

        // RAW hazard between ID <-> MEM stages is solved with forwarding
        simRd(r, 0x12345678)
        dut.clockDomain.waitRisingEdge(1) // delay
        simRs1(r)
        dut.clockDomain.waitRisingEdge(1) // wait for read to finish
        rs1 = getRs1d
        assert(rs1 == 0x12345678, s"RS1: RAW hazard between ID<->MEM stages not solved, read $rs1")
        simRd(r, 0x87654321)
        dut.clockDomain.waitRisingEdge(1) // delay
        simRs2(r)
        dut.clockDomain.waitRisingEdge(1) // wait for read to finish
        rs2 = getRs2d
        assert(rs2 == 0x87654321, s"RS2: RAW hazard between ID<->MEM stages not solved, read $rs2")

        // RAW hazard between ID <-> WB stages is solved with forwarding
        simRd(r, 0x12345678)
        dut.clockDomain.waitRisingEdge(2) // delay
        simRs1(r)
        dut.clockDomain.waitRisingEdge(1) // wait for read to finish
        rs1 = getRs1d
        assert(rs1 == 0x12345678, s"RS1: RAW hazard between ID<->WB stages not solved, read $rs1")
        simRd(r, 0x87654321)
        dut.clockDomain.waitRisingEdge(2) // delay
        simRs2(r)
        dut.clockDomain.waitRisingEdge(1) // wait for read to finish
        rs2 = getRs2d
        assert(rs2 == 0x87654321, s"RS2: RAW hazard between ID<->WB stages not solved, read $rs2")

        // RAW hazard of instructions four slots apart are solved via the regfile
        simRd(r, 0x12345678)
        dut.clockDomain.waitRisingEdge(3) // delay
        simRs1(r)
        dut.clockDomain.waitRisingEdge(1) // wait for read to finish
        rs1 = getRs1d
        // reads default value because regfile is not implemented in harness
        assert(rs1 == 0x00000000, s"RS1: RAW hazard with three delays not solved, read $rs1")
        simRd(r, 0x87654321)
        dut.clockDomain.waitRisingEdge(3) // delay
        simRs2(r)
        dut.clockDomain.waitRisingEdge(1) // wait for read to finish
        rs2 = getRs2d
        // reads default value because regfile is not implemented in harness
        assert(rs2 == 0x00000000, s"RS2: RAW hazard with three delays not solved, read $rs2")

        // simultaneous RAWs, only forwarding the most recent (no delay, use EX)
        simRd(r, 0x11111111)
        simRd(r, 0x22222222)
        simRd(r, 0x33333333)
        simRs1(r)
        dut.clockDomain.waitRisingEdge(1) // wait for read to finish
        rs1 = getRs1d
        assert(rs1 == 0x33333333, s"RS1: simultaneous RAW hazards solved with stale data, read ${rs1.toHexString}")

        // simultaneous RAWs, only forwarding the most recent (one delay, use MEM)
        simRd(r, 0x11111111)
        simRd(r, 0x22222222)
        simRs1(r)
        dut.clockDomain.waitRisingEdge(1) // wait for read to finish
        rs1 = getRs1d
        assert(rs1 == 0x22222222, s"RS1: simultaneous RAW hazards solved with stale data, read ${rs1.toHexString}")

        // RAW hazard between ID <-> WB stages is NOT solved with forwarding
        // when EX stage is halted for one or more clocks.
        simRd(r, 0x12345678)  // write to x1
        dut.io.haltEx #= true // stall EX, prevents forwarding
        simRs1(r)             // read x1, value from EX would be forwarded
        // no forwarding must have been done, pipeline is stalled
        rs1 = getRs1d
        assert(rs1 == 0x00000000, s"RS1: read ${rs1.toHexString}")
        // unstall pipeline and move along, must now forward
        dut.io.haltEx #= false
        simRs1(r) // reapply, otherwise ID forgets stalled instruction
        rs1 = getRs1d
        assert(rs1 == 0x00000000, s"RS1: read ${rs1.toHexString}")
        dut.clockDomain.waitRisingEdge(1)
        rs1 = getRs1d
        assert(rs1 == 0x12345678, s"RS1: read ${rs1.toHexString}")
        dut.clockDomain.waitRisingEdge(1)
        rs1 = getRs1d
        assert(rs1 == 0x00000000, s"RS1: read ${rs1.toHexString}")

        // RAW hazard between ID <-> WB stages is NOT solved with forwarding
        // when MEM stage is halted for one or more clocks.
        simRd(r, 0x12345678)  // write to x1
        dut.clockDomain.waitRisingEdge(1) // move write to MEM
        dut.io.haltMem #= true // stall EX, prevents forwarding
        simRs1(r)              // read x1, value from MEM would be forwarded
        // no forwarding must have been done, pipeline is stalled
        rs1 = getRs1d
        assert(rs1 == 0x00000000, s"RS1: read ${rs1.toHexString}")
        // unstall pipeline and move along, must now forward
        dut.io.haltMem #= false
        simRs1(r) // reapply, otherwise ID forgets stalled instruction
        rs1 = getRs1d
        assert(rs1 == 0x00000000, s"RS1: read ${rs1.toHexString}")
        dut.clockDomain.waitRisingEdge(1)
        rs1 = getRs1d
        assert(rs1 == 0x12345678, s"RS1: read ${rs1.toHexString}")
        dut.clockDomain.waitRisingEdge(1)
        rs1 = getRs1d
        assert(rs1 == 0x00000000, s"RS1: read ${rs1.toHexString}")
      }
    }
  }
}
