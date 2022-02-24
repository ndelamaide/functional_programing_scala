course := "progfun1"
assignment := "forcomp"

scalaVersion := "0.27.0-RC1"

scalacOptions ++= Seq("-language:implicitConversions", "-deprecation")

libraryDependencies += "org.scalameta" %% "munit" % "0.7.12"

testSuite := "forcomp.AnagramsSuite"
val MUnitFramework = new TestFramework("munit.Framework")
testFrameworks += MUnitFramework
// Decode Scala names
testOptions += Tests.Argument(MUnitFramework, "-s")
