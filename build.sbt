import AssemblyKeys._

organization := "thesmith"

name := "podiki"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.9.1"

seq(assemblySettings: _*)

jarName in assembly := "podiki.jar"

test in assembly := {}

mainClass in (Compile, packageBin) := Some("thesmith.podiki.App")

mainClass in (Compile, run) := Some("thesmith.podiki.App")

resourceGenerators in Compile <+= (resourceManaged, baseDirectory) map { (managedBase, base) => 
  val webappBase = base / "src" / "main" / "webapp" 
  for { 
    (from, to) <- webappBase ** "*" x rebase(webappBase, managedBase / "main" / "webapp") 
  } yield { 
    Sync.copy(from, to) 
    to 
  } 
}

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("redis", "clients", xs @ _*) => MergeStrategy.first
    case x => old(x)
  }
}

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.0" % "runtime",
  "junit" % "junit" % "4.8.2" % "test",
  "org.mockito" % "mockito-core" % "1.8.5" % "test",
  "com.googlecode.soundlibs" % "mp3spi" % "1.9.5-1",
  "commons-io" % "commons-io" % "2.1",
  "org.apache.httpcomponents" % "httpclient" % "4.1.2",
  "com.top10" %% "scala-redis-client" % "1.7.0-SNAPSHOT" withSources(),
  "net.liftweb" %% "lift-json" % "2.4-M4" withSources(),
  "net.liftweb" %% "lift-json-ext" % "2.4-M4" withSources()
)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Scala-Tools Maven2 Releases Repository" at "http://scala-tools.org/repo-releases"

parallelExecution in Test := false
