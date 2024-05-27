package curlyrv.ops

import spinal.core._

import scala.collection.mutable.ArrayBuffer

import curlyrv.CpuBase

/** Baseclass of Operation
  *
  * Child classes of this base implement various operations that happen inside
  * the pipelined CPU.
  *
  * @param cpu CPU to which this operation is added
  */
class CpuBaseOps extends Area {

  /** Give this and all child operations a default name.
    * Helps to prevent "zz" named signals from anonymous ops added like
    * "addOp(CpuBaseOps())" during netlist generation.
    */
  setName(getClass.getSimpleName.toLowerCase())

  /** Build additional hardware in the operations.
    *
    * Overwritten by child classes to give them an opportunity to define
    * hardware at an later time in the build process.
    */
  def build(): Unit = {}
}

trait CpuOpsStore {
  val ops = ArrayBuffer[CpuBaseOps]()
  def addOp(op: CpuBaseOps): Unit = { ops += op }
  def buildOps(): Unit = { ops.foreach(_.build()) }
}
