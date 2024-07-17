package curlyrv

import org.scalatest.funsuite.AnyFunSuite

import spinal.core._
import spinal.core.formal._
import spinal.idslplugin.Location

/** Custom testsuite for formal tests.
  *
  * In the context of an AnyFunSuite, methods assert and assume methods would
  * resolve to org.scalatest.funsuite.assert/assume. We provide our own such
  * that we can use the formal versions of assert/assume.
  */
class FormalFunSuite extends AnyFunSuite {
  def assert(assertion: Bool)(implicit loc: Location) = {
    spinal.core.assert(assertion)
  }
  def assert(property: FormalProperty)(implicit loc: Location) = {
    spinal.core.assert(property)
  }

  def assume(assertion: Bool)(implicit loc: Location) = {
    spinal.core.assume(assertion)
  }
  def assume(property: FormalProperty)(implicit loc: Location) = {
    spinal.core.assume(property)
  }
}
