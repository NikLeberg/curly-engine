package curlyrv.ops

import spinal.core._
import spinal.lib._

import curlyrv.{Cpu, CpuConfig}

case class IFetchBus(cpuConfig: CpuConfig) extends Bundle with IMasterSlave {
  import cpuConfig._
  val pcValid = Bool
  val pc = UInt(XLEN bits)
  val instValid = Bool
  val inst = Bits(XLEN bits)

  override def asMaster(): Unit = {
    out(pcValid, pc)
    in(instValid, inst)
  }
}

case class IFetchOps(cpu: Cpu, iBus: IFetchBus) extends CpuBaseOps {
  import cpu.pipes._
  import cpu.config._

  val fetcher = new fetch.Area {
    iBus.pc := PC
    iBus.pcValid := isValid
  }

  val decoder = new decode.Area {
    IR := iBus.inst
    haltWhen(!iBus.instValid)
  }
}
