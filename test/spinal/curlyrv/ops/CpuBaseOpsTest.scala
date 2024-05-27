package curlyrv.ops

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfter

import spinal.core._

import curlyrv.{CpuBase, CpuConfig}

case class CpuOpsStoreHarness() extends CpuOpsStore
case class CpuBaseOpsHarness() extends CpuBaseOps {
  var buildCalled: Boolean = false
  override def build(): Unit = { buildCalled = true }
}

class CpuOpsStoreTest extends AnyFunSuite with BeforeAndAfter {
  var storeHarness: CpuOpsStoreHarness = null
  var opHarness: CpuBaseOpsHarness = null

  before {
    storeHarness = CpuOpsStoreHarness()
    opHarness = CpuBaseOpsHarness()
  }

  test("CpuOpsStore.addOp can add op") {
    assert(storeHarness.ops.length == 0)
    storeHarness.addOp(opHarness)
    assert(storeHarness.ops.length == 1)
  }

  test("CpuOpsStore.buildOps calls build of op") {
    storeHarness.addOp(opHarness)
    assert(!opHarness.buildCalled)
    storeHarness.buildOps()
    assert(opHarness.buildCalled)
  }
}
