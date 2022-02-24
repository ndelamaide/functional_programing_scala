course := "progfun2"
assignment := "quickcheck"

scalaVersion := "0.27.0-RC1"
scalacOptions ++= Seq("-language:implicitConversions", "-deprecation")
libraryDependencies += "org.scalameta" %% "munit" % "0.7.12"
libraryDependencies += ("org.scalacheck" %% "scalacheck" % "1.14.2").withDottyCompat(scalaVersion.value)

testSuite := "quickcheck.QuickCheckSuite"
val MUnitFramework = new TestFramework("munit.Framework")
testFrameworks += MUnitFramework
// Decode Scala names
testOptions += Tests.Argument(MUnitFramework, "-s")
