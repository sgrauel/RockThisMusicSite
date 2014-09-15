import sbt._
import play.Project._

object ApplicationBuild extends Build {

  val appName = "portfolioSite"
  val appVersion = "1.0"

  val appDependencies = Seq(

    jdbc,
    anorm,
    "mysql" % "mysql-connector-java" % "5.1.18",
    "org.webjars" %% "webjars-play" % "2.1.0-3",
    "org.webjars" % "bootstrap" % "3.0.3",
    "org.webjars" % "jquery" % "2.1.1",
    "org.webjars" % "restangular" % "1.4.0-2"
  )

  val main = play.Project(appName, appVersion, appDependencies)

}
