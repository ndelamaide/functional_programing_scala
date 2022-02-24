course := "progfun2"
assignment := "interpreter"

scalaVersion := "3.0.0-M1"
scalacOptions ++= Seq("-deprecation")
libraryDependencies += "org.scalameta" %% "munit" % "0.7.17"
libraryDependencies += ("org.scalacheck" %% "scalacheck" % "1.14.2").withDottyCompat(scalaVersion.value)

testSuite := "interpreter.RecursiveLanguageSuite"
val MUnitFramework = new TestFramework("munit.Framework")
testFrameworks += MUnitFramework
// Decode Scala names
testOptions += Tests.Argument(MUnitFramework, "-s")
