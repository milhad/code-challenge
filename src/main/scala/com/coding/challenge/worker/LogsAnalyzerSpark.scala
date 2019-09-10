package com.coding.challenge.worker

import com.coding.challenge.db.Database
import com.coding.challenge.utils.Utils
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Minutes, Seconds, StreamingContext}

// Note that applications should define a main() method instead of extending scala.App.
// Subclasses of scala.App may not work correctly.
object LogsAnalyzerSpark extends LazyLogging {

  private var ssc: StreamingContext = _
  private var kafkaStream: InputDStream[ConsumerRecord[String, String]] = _

  def main(args: Array[String]): Unit = {
    try {
      val conf = ConfigFactory.load
      val KAFKA_BOOTRSTRAP_SERVERS = conf.getString("thinkstep-challenge.kafka.bootstrap-servers")
      val KAFKA_LOGS_TOPIC = conf.getString("thinkstep-challenge.kafka.topic")

      val sparkConf = new SparkConf().setAppName("ThingStepCodingChallenge").setMaster("local[2]")
      ssc = new StreamingContext(sparkConf, Seconds(1))

      // setup spark streaming from kafka
      val kafkaParams = Map[String, Object](
        "bootstrap.servers" -> KAFKA_BOOTRSTRAP_SERVERS,
        "key.deserializer" -> classOf[StringDeserializer],
        "value.deserializer" -> classOf[StringDeserializer],
        "group.id" -> "apache_logs_group_id",
        "auto.offset.reset" -> "latest",
        "enable.auto.commit" -> (false: java.lang.Boolean)
      )

      val topics = Array(KAFKA_LOGS_TOPIC)
      kafkaStream = KafkaUtils.createDirectStream[String, String](
        ssc,
        PreferConsistent,
        Subscribe[String, String](topics, kafkaParams)
      )

      val logStream = kafkaStream.map(record => (record.key, record.value))

      val windowed5sec = logStream.window(Seconds(5), Seconds(1)).map(c => c._2)
      val windowed1min = logStream.window(Minutes(1), Seconds(5)).map(c => c._2)
      val windowed5min = logStream.window(Minutes(5), Minutes(1)).map(c => c._2)
      val windowed30min = logStream.window(Minutes(30), Minutes(1)).map(c => c._2)
      val windowed1hr = logStream.window(Minutes(60), Minutes(5)).map(c => c._2)
      val windowed1day = logStream.window(Minutes(60 * 24), Minutes(60)).map(c => c._2)

      //setup executions
      windowed5sec.foreachRDD { rdd =>
        val uniqueCount = rdd.distinct().count()
        logger.info(s"Updating new count for last 5 seconds: $uniqueCount")
        Database.updateCount(Utils.SEGMENTS.LAST_5_SECONDS, uniqueCount)
      }

      windowed1min.foreachRDD { rdd =>
        val uniqueCount = rdd.distinct().count()
        logger.info(s"Updating new count for last 1 minutes: $uniqueCount")
        Database.updateCount(Utils.SEGMENTS.LAST_1_MINUTE, uniqueCount)
      }

      windowed5min.foreachRDD { rdd =>
        val uniqueCount = rdd.distinct().count()
        logger.info(s"Updating new count for last 5 minutes: $uniqueCount")
        Database.updateCount(Utils.SEGMENTS.LAST_5_MINUTES, uniqueCount)
      }

      windowed30min.foreachRDD { rdd =>
        val uniqueCount = rdd.distinct().count()
        logger.info(s"Updating new count for last 30 minutes: $uniqueCount")
        Database.updateCount(Utils.SEGMENTS.LAST_30_MINUTES, uniqueCount)
      }

      windowed1hr.foreachRDD { rdd =>
        val uniqueCount = rdd.countApproxDistinct() // we will count approximate number for large data sets
        logger.info(s"Updating new count for last 1 hour: $uniqueCount")
        Database.updateCount(Utils.SEGMENTS.LAST_1_HOUR, uniqueCount)
      }

      windowed1day.foreachRDD { rdd =>
        val uniqueCount = rdd.countApproxDistinct() // we will count approximate number for large data sets
        logger.info(s"Updating new count for last 1 day: $uniqueCount")
        Database.updateCount(Utils.SEGMENTS.LAST_1_DAY, uniqueCount)
      }

      // start executions
      ssc.start()
      ssc.awaitTermination()

    } catch {
      case exception: Exception =>
        logger.error(s"Error in processing logs stream: ${exception.getMessage}", exception)
    } finally {
      kafkaStream.stop()
      ssc.stop(stopSparkContext = true, stopGracefully = true)
    }
  }

  sys.addShutdownHook({
    kafkaStream.stop()
    ssc.stop(stopSparkContext = true, stopGracefully = true)
  })
}
