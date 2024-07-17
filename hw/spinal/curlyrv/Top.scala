package curlyrv

import spinal.core._
import spinal.core.sim._
import spinal.core.formal._

object Config {
  def spinal = SpinalConfig(
    targetDirectory = "hw/gen",
    defaultConfigForClockDomains = ClockDomainConfig(
      resetActiveLevel = LOW
    ),
    onlyStdLogicVectorAtTopLevelIo = true,
    verbose = true
  )

  def sim = SimConfig.withConfig(spinal).withVcdWave // .withGhdl

  def formal = FormalConfig.withConfig(spinal.includeFormal).withDebug
}

case class Top() extends Component {
  val cpu = Cpu(CpuConfig())
}

object TopLevelVhdl extends App {
  Config.spinal.generateVhdl(Top()).printPruned()
}
