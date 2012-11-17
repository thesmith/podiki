import AssemblyKeys._

organization := "thesmith"

name := "podiki"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.9.1"

seq(assemblySettings: _*)

seq(webSettings :_*)

jarName in assembly := "podiki.jar"

test in assembly := {}

port in container.Configuration := 8082

resourceGenerators in Compile <+= (resourceManaged, baseDirectory) map { (managedBase, base) => 
  val webappBase = base / "src" / "main" / "webapp" 
  for { 
    (from, to) <- webappBase ** "*" x rebase(webappBase, managedBase / "main" / "webapp") 
  } yield { 
    Sync.copy(from, to) 
    to 
  } 
} 

libraryDependencies ++= Seq(
  "org.scalatra" %% "scalatra" % "2.0.2" withSources(),
  "org.scalatra" %% "scalatra-lift-json" % "2.0.2" withSources(),
  "org.scalatra" %% "scalatra-scalatest" % "2.0.2" % "test" withSources(),
  "ch.qos.logback" % "logback-classic" % "1.0.0" % "runtime",
  "org.eclipse.jetty" % "jetty-server" % "8.0.3.v20111011",
  "org.eclipse.jetty" % "jetty-servlets" % "8.0.3.v20111011",
  "org.eclipse.jetty" % "jetty-webapp" % "8.0.3.v20111011" % "container,compile",
  "org.mortbay.jetty" % "servlet-api" % "3.0.pre4",
  "junit" % "junit" % "4.8.2" % "test",
  "org.mockito" % "mockito-core" % "1.8.5" % "test",
  "com.googlecode.soundlibs" % "mp3spi" % "1.9.5-1",
  "commons-io" % "commons-io" % "2.1",
  "org.apache.httpcomponents" % "httpclient" % "4.1.2",
  "com.top10" %% "scala-redis-client" % "1.7.0-SNAPSHOT" withSources()
)

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Scala-Tools Maven2 Releases Repository" at "http://scala-tools.org/repo-releases"

parallelExecution in Test := false
