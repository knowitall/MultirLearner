name := "MultirLearner"

version := "1.0"

scalaVersion := "2.10.1"

javaOptions += "-Xmx12G"

fork in run := true

libraryDependencies ++= Seq(
  "edu.stanford.nlp" % "stanford-corenlp" % "1.3.5"
)

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
