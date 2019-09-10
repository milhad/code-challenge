package com.coding.challenge.worker

import cakesolutions.kafka.KafkaProducer.Conf
import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import com.coding.challenge.parser.AccessLogParser
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.common.serialization.StringSerializer
import scala.sys.process._

object LogsKafkaProducer extends LazyLogging {

  var producer: KafkaProducer[String, String] = _

  def main(args: Array[String]): Unit = {
    try {
      logger.info("Starting Logs Producer")
      val conf = ConfigFactory.load

      val KAFKA_BOOTRSTRAP_SERVERS = conf.getString("thinkstep-challenge.kafka.bootstrap-servers")
      val KAFKA_LOGS_TOPIC = conf.getString("thinkstep-challenge.kafka.topic")

      producer = KafkaProducer(
        Conf(new StringSerializer(), new StringSerializer(), bootstrapServers = KAFKA_BOOTRSTRAP_SERVERS)
      )

      val parser = new AccessLogParser()
      val generateLogsCommand = s"flog ${args.mkString(" ")}"
      val logStreamGenerator: Stream[String] = generateLogsCommand.lineStream_!
      logStreamGenerator.filter(_ != null).foreach { log =>
        logger.info(log)

        val parsed = parser.parseRecord(log)

        if (parsed.isDefined) {
          val record = KafkaProducerRecord[String, String](
            KAFKA_LOGS_TOPIC,
            Some("unique_visitors"),
            s"${parsed.get.clientIpAddress}${parsed.get.remoteUser}${parsed.get.userAgent}")

          producer.send(record)
        }
      }

      logger.info("Log generation complete")
    } catch {
      case exception: Exception =>
        logger.error(s"Error during logs generation: ${exception.getMessage}", exception)
    } finally {
      producer.flush()
      producer.close()
    }
  }

  sys.addShutdownHook({
    producer.flush()
    producer.close()
  })
}
