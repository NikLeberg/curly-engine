# SpinalHDL Base Project

This repository is a base project to help Spinal users set-up project without knowledge about Scala and SBT.


## If it is your are learning SpinalHDL

You can follow the tutorial on the [Getting Started](https://spinalhdl.github.io/SpinalDoc-RTD/master/SpinalHDL/Getting%20Started/index.html) page.

More specifically:

* instructions to install tools can be found on the [Install and setup](https://spinalhdl.github.io/SpinalDoc-RTD/master/SpinalHDL/Getting%20Started/Install%20and%20setup.html#install-and-setup) page
* instructions to get this repository locally are available in the [Create a SpinalHDL project](https://spinalhdl.github.io/SpinalDoc-RTD/master/SpinalHDL/Getting%20Started/Install%20and%20setup.html#create-a-spinalhdl-project) section.


### TL;DR Things have already been set up in my environment, how do I run things to try SpinalHDL?

Once in the `SpinalTemplateSbt` directory, when tools are installed, the commands below can be run to use `sbt`.

```sh
// To generate the Verilog from the example
sbt "runMain curlyrv.MyTopLevelVerilog"

// To generate the VHDL from the example
sbt "runMain curlyrv.MyTopLevelVhdl"

// To run the testbench
sbt "runMain curlyrv.MyTopLevelSim"
```

* The example hardware description is into `hw/spinal/projectname/MyTopLevel.scala`
* The testbench is into `hw/spinal/projectname/MyTopLevelSim.scala`

When you really start working with SpinalHDL, it is recommended (both for comfort and efficiency) to use an IDE, see the [Getting started](https://spinalhdl.github.io/SpinalDoc-RTD/master/SpinalHDL/Getting%20Started/index.html).


## If you want to create a new project from this template

### Change project structure

You can change the project structure as you want. The only restrictions (from Scala environment) are (let's say your actual project name is `myproject`):

* you must have a `myproject` folder and files in it must start with `package myproject`
* if you have a file in a subfolder `myproject/somepackage/MyElement.scala` it must start with `package myproject.somepackage`.
* `sbt` and `mill` must be run right in the folder containing their configurations (recommended to not move these files)

Once the project structure is modified, update configurations:

* In `build.sbt` and/or `build.sc` (see above) replace `/ "hw" / "spinal"` by the new path to the folder containing the `myproject` folder.
* In the spinal configuration file (if you kept it, by default it is in `projectname/Config.scala`) change the path in `targetDirectory = "hw/gen"` to the directory where you want generated files to be written. If you don't use a config or if it doesn't contain this element, generated files will be written in the root directory.


### Update this README

Of course you can replace/modify this file to help people with your own project!
