package pg.config

import com.typesafe.config.Config

case class DbConfig(config: Config) {
  val url: String = config.getString("url")
  val driver: String = config.getString("driver")
  val numThread: Int = config.getInt("numThread")
  val queueSize: Int = config.getInt("queueSize")
}
