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
    val opcode = in port Bits(7 bits)
    val rs1, rs2, rd = in port UInt(5 bits)
    val rdd = in port SInt(XLEN bits)
    val rs1d, rs2d = out port SInt(XLEN bits)
  }.simPublic()

  // DUT
  // input: Payload(RS1/2, RD, RDD, WB)
  // output: Payload(RS1D/2D)
  val regfileOps = RegfileOps(this)
  addOp(regfileOps)
  regfileOps.regfile.simPublic()

  // build dummy pipeline around DUT to set and get payloads
  val decoder = new decode.Area {
    OPCODE := io.opcode
    RS1 := io.rs1
    RS2 := io.rs2
    RD := io.rd
    RDD := io.rdd
  }
  val executer = new execute.Area {
    io.rs1d := RS1D
    io.rs2d := RS2D
  }
  addInstruction(M"0000001", M"000", M"0000000", InstructionTypes.instBType)
  addInstruction(M"0000010", M"000", M"0000000", InstructionTypes.instIType)
  addInstruction(M"0000100", M"000", M"0000000", InstructionTypes.instJType)
  addInstruction(M"0001000", M"000", M"0000000", InstructionTypes.instRType)
  addInstruction(M"0010000", M"000", M"0000000", InstructionTypes.instSType)
  addInstruction(M"0100000", M"000", M"0000000", InstructionTypes.instUType)
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

      // Get/set values directly in the registerfile.
      def getReg(reg: Int): Int = { dut.regfileOps.regfile.getBigInt(reg).toInt }
      def setReg(reg: Int, value: BigInt): Unit = { dut.regfileOps.regfile.setBigInt(reg, value) }

      // Simulate sequence to read a register through rs1.
      def simRs1(reg: Int): Unit = {
        dut.io.rs1 #= reg
        dut.clockDomain.waitRisingEdge(1)
        dut.io.rs1 #= 0
      }
      def getRs1d: Int = dut.io.rs1d.toInt
      def readRs1(reg: Int): Int = {
        simRs1(reg)
        dut.clockDomain.waitRisingEdge(1)
        getRs1d
      }

      // Simulate sequence to read a register through rs2.
      def simRs2(reg: Int): Unit = {
        dut.io.rs2 #= reg
        dut.clockDomain.waitRisingEdge(1)
        dut.io.rs2 #= 0
      }
      def getRs2d: Int = dut.io.rs2d.toInt
      def readRs2(reg: Int): Int = {
        simRs2(reg)
        dut.clockDomain.waitRisingEdge(1)
        getRs2d
      }

      // Simulate sequence to write a register.
      def simRd(reg: Int, value: BigInt): Unit = {
        dut.io.rd #= reg
        dut.io.rdd #= value
        dut.io.opcode #= 0x08 // R-Type
        dut.clockDomain.waitRisingEdge(1)
        dut.io.rd #= 0
        dut.io.rdd #= 0
        dut.io.opcode #= 0x01 // B-Type
      }
      def writeRd(reg: Int, value: BigInt): Unit = {
        simRd(reg, value)
        dut.clockDomain.waitRisingEdge(4) // execute, memory, writeback, sync
      }

      // x0 reads always as zero
      val rs1d = readRs1(0)
      assert(rs1d == 0, s"RS1(reg x0): read $rs1d, expected 0")
      val rs2d = readRs2(0)
      assert(rs2d == 0, s"RS2(reg x0): read $rs2d, expected 0")

      // can read registers 1 to 31 through rs1
      for (i <- 1 to 31) {
        val rs1d = readRs1(i)
        val ex = getReg(i)
        assert(rs1d == ex, s"RS1(reg x$i): read $rs1d, expected $ex")
      }

      // can read registers 1 to 31 through rs2
      for (i <- 1 to 31) {
        val rs2d = readRs2(i)
        val ex = getReg(i)
        assert(rs2d == ex, s"RS2(reg x$i): read $rs2d, expected $ex")
      }

      // can write to register x0, but either read returns 0
      writeRd(0, 0xabababab)
      assert(readRs1(0) == 0, s"x0 is expected to be constant zero")
      assert(readRs2(0) == 0, s"x0 is expected to be constant zero")

      // can write to registers 1 to 31
      for (i <- 1 to 31) {
        writeRd(i, 0xabababab)
        val rdd = getReg(i)
        assert(rdd == 0xabababab, s"RD(reg x$i): write 0xabababab, but read $rdd")
      }

      // can write to and then read from registers 1 to 31
      for (i <- 1 to 31) {
        writeRd(i, i)
        val rs1 = readRs1(i)
        assert(rs1 == i, s"RD+RS1(reg x$i): write $i, but read $rs1")
        val rs2 = readRs2(i)
        assert(rs2 == i, s"RD+RS2(reg x$i): write $i, but read $rs2")
      }

      for (i <- 1 to 31) {
        simRd(i, 0x05050505)
        simRs1(i)
        assert(getRs1d != 0x05050505, s"RD+RS1(reg x$i): should not read 0x05050505 yet")
        simRs1(i)
        assert(getRs1d != 0x05050505, s"RD+RS1(reg x$i): should not read 0x05050505 yet")
        simRs1(i)
        assert(getRs1d != 0x05050505, s"RD+RS1(reg x$i): should not read 0x05050505 yet")
        simRs1(i)
        assert(getRs1d != 0x05050505, s"RD+RS1(reg x$i): should not read 0x05050505 yet")
        simRs1(i)
        assert(getRs1d == 0x05050505, s"RD+RS1(reg x$i): should now read 0x05050505")
      }
    }
  }
}

class RegfileOpsFormal extends FormalFunSuite {
  test("RegfileOps.Formal") {
    Config.formal.withGhdl
      .withBMC(10)
      .withCover(10)
      .doVerify(new Component {
        setDefinitionName("RegfileOpsFormal")
        val dut = FormalDut(RegfileOpsHarness())
        anyseq(dut.io.opcode)
        anyseq(dut.io.rs1)
        anyseq(dut.io.rs2)
        anyseq(dut.io.rd)
        anyseq(dut.io.rdd)

        assumeInitial(dut.clockDomain.isResetActive)
        val initDone = !initstate() && !dut.clockDomain.isResetActive
        val seqInit = initDone.repeat("+")

        // Register x0 is always read as zero.
        assert(dut.io.rs1 === 0 |=> dut.io.rs1d === 0)
        cover(dut.io.rs1 === 0)
        assert(dut.io.rs2 === 0 |=> dut.io.rs2d === 0)
        cover(dut.io.rs2 === 0)

        // Values written can be read back.
        val anyRsNum = cloneOf(dut.io.rd)
        anyconst(anyRsNum)
        assume(anyRsNum =/= 0)
        val anyRsData = cloneOf(dut.io.rdd)
        anyseq(anyRsData)

        val seqWriteZero =
          (dut.io.rd === anyRsNum) :-> (dut.io.rdd === 0) :-> (dut.io.opcode === B"0001000")
        val seqWriteData =
          (dut.io.rd === anyRsNum) :-> (dut.io.rdd === anyRsData) :-> (dut.io.opcode === B"0001000")
        // Always write a zero first to ensure solver cannot just start with the
        // expected value in the registerfile at initialzation (for cover).
        val seqWrite = seqWriteZero :=> seqWriteData
        // Wait for the pipelined write to arrive in WB (IF, ID, EX, MEM, WB).
        val seqWaitAndReadRs1 = True.repeat("*4") :=> (dut.io.rs1 === anyRsNum)
        val seqWaitAndReadRs2 = True.repeat("*4") :=> (dut.io.rs2 === anyRsNum)
        val seqReadDataRs1 = (dut.io.rs1d === past(anyRsData, 5))
        val seqReadDataRs2 = (dut.io.rs2d === past(anyRsData, 5))

        assert((seqInit :-> seqWrite :-> seqWaitAndReadRs1) |=> seqReadDataRs1)
        cover(seqInit :-> seqWrite :-> seqWaitAndReadRs1 :=> seqReadDataRs1)
        assert((seqInit :-> seqWrite :-> seqWaitAndReadRs2) |=> seqReadDataRs2)
        cover(seqInit :-> seqWrite :-> seqWaitAndReadRs2 :=> seqReadDataRs2)

        // Can only write with enabled writeback.
        // Writeback is enabled with R-Type but not with B-Type.
        assert((seqInit :-> (dut.io.opcode === B"0000001")).next(4, stable(dut.regfileOps.regfile(1))))
        cover(seqInit :-> (dut.io.opcode === B"0000001") :-> True.repeat("*4") :-> stable(dut.regfileOps.regfile(1)))
        cover(seqInit :-> (dut.io.opcode === B"0001000") :-> True.repeat("*4") :-> !stable(dut.regfileOps.regfile(1)))
      })
  }
}
