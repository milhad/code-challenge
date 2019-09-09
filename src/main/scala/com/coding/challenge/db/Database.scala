package com.coding.challenge.db

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import scala.concurrent.ExecutionContext

object Database extends LazyLogging {
  private val conf: Config = ConfigFactory.load
  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private val databaseHost = conf.getString("thinkstep-challenge.database.host")
  private val databasePort = conf.getString("thinkstep-challenge.database.port")
  private val databaseName = conf.getString("thinkstep-challenge.database.db-name")

  val config = new HikariConfig()
  config.setDriverClassName("org.postgresql.Driver");
  config.setJdbcUrl(s"jdbc:postgresql://$databaseHost:$databasePort/$databaseName")
  config.setUsername(conf.getString("thinkstep-challenge.database.username"))
  config.setPassword(conf.getString("thinkstep-challenge.database.password"))
  config.addDataSourceProperty("cachePrepStmts", "true")
  config.addDataSourceProperty("prepStmtCacheSize", "250")
  config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
  private val ds = new HikariDataSource(config)

  def getDsInstance: HikariDataSource = ds

  def updateCount(scope: String, newValue: Long): Int = {
    val connection = ds.getConnection()
    connection.setAutoCommit(false)

    try {
      val sqlUpdate = s"UPDATE unique_visitors.visits_count SET unq_count = $newValue, last_update_date = now() WHERE scope = '$scope';"
      val statement = connection.prepareStatement(sqlUpdate)
      val rowsAffected = statement.executeUpdate()
      connection.commit()
      rowsAffected
    } catch {
      case ex: Throwable =>
        logger.error(s"Error updating counts in DB: ${ex.getMessage}", ex)
        0
    } finally {
      connection.close()
    }
  }
}
