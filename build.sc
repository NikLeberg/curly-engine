import mill._, scalalib._

import $file.^.SpinalHDL.build
import ^.SpinalHDL.build.{core => spinalCore}
import ^.SpinalHDL.build.{lib => spinalLib}
import ^.SpinalHDL.build.{idslplugin => spinalIdslplugin}

val spinalVers = "1.10.2a"
val scalaVers = "2.12.18"

object curlyrv extends RootModule with SbtModule {
  def scalaVersion = scalaVers
  def sources = T.sources(
    this.millSourcePath / "hw" / "spinal"
  )

  def idslplugin = spinalIdslplugin(scalaVers)
  def moduleDeps = Seq(
    spinalCore(scalaVers),
    spinalLib(scalaVers),
    idslplugin
  )
  def scalacOptions = super.scalacOptions() ++ idslplugin.pluginOptions()

  // def ivyDeps = Agg(
  //   ivy"com.github.spinalhdl::spinalhdl-core:$spinalVers",
  //   ivy"com.github.spinalhdl::spinalhdl-lib:$spinalVers"
  // )
  // def scalacPluginIvyDeps = Agg(ivy"com.github.spinalhdl::spinalhdl-idsl-plugin:$spinalVers")

  object test extends SbtModuleTests with TestModule.ScalaTest {
    def sources = T.sources(
      this.millSourcePath / "test" / "spinal"
    )
    def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.2.18"
    )
    def testOne(args: String*) = T.command {
      super.runMain("org.scalatest.run", args: _*)
    }
  }
}
