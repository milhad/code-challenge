# Code challenge

Repository contains services written in Scala for counting distinct (unique) visitors in last X intervals from Apache Logs.

## Problem description

Build a system that will provide user with insights of real time unique visitors for their website. System should be able to consume apache logs and present user with information on how many unique visits occurred during past 5 seconds, 1 minute, 5 minutes, 30 minutes, 1 hour and 1 day.

# Solution

Resulting solution comprises of 2 services:
1. Logs generator - Generates fake logs in Apache combined format and streams them to Kafka
2. Logs analyzer - Consumes log stream from Kafka and uses Apache Spark to perform distinct counts over a windowed data sets in the stream.

## Data flow

Fake Apache logs are generated, parsed and unique user IDs are extracted from the logs. **Unique ID is composed by combining IP address, username and user agent from the log entry.** Combination of those three parameters is considered unique in terms of visit.
Unique IDs are sent to Kafka topic for further processing. Kafka has been chosen as it is one of the supported input sources for Apache Spark.
Another worker is responsible for initiating Spark Streaming context, consuming Kafka payloads and streaming them into Spark for performing near real-time analytics (counting unique visits over a few different periods).

## Services

### Logs generator

This service is responsible for generating fake logs for processing. It uses a tool called `flog` ([https://github.com/mingrammer/flog](https://github.com/mingrammer/flog)) to generate logs in **apache_combined** format, parse them, extract Unique user IDs from each log entry and publish the results to Kafka topic.

### Logs analyzer

The role of this service is to consume User IDs of visitors from Kafka, stream them to Spark and use Spark functions to calculate unique visits over defined periods of time. Once the count is calculated it is saved to PostgreSQL data table.

# Used tools

To complete the task, I have used the following tools:

- `flog` logs generator, install from: [https://github.com/mingrammer/flog](https://github.com/mingrammer/flog)
- Apache Kafka v2.3.0, install from: [https://kafka.apache.org/downloads](https://kafka.apache.org/downloads)
- Apache Spark v2.4.4, install from: [https://spark.apache.org/downloads.html](https://spark.apache.org/downloads.html)
- PostgreSQL v10, install from [https://www.postgresql.org/download/](https://www.postgresql.org/download/)

Services are coded using Scala v2.12.6 ([https://www.scala-lang.org/download/](https://www.scala-lang.org/download/)) and SBT v1.2.8 ([https://www.scala-sbt.org/download.html](https://www.scala-sbt.org/download.html))

# How to run the services

First things first, make sure to have all the tools installed and running on your system. Clone this repository to your computer, and navigate to project folder. On the following path `src/main/resources/db/CreateDatabase.sql`is a SQL script which will prepare the database assets needed to store the results in PostgreSQL table. Use the tool of your choice to execute the script (DataGrip, pgAdmin, Valentina Studio, etc).

Note: first part of the script which creates the DB can be executed on any schema, however, make sure you are switched to the newly created database (`unique_visitors_db`), when executing the second part of the script. The reason for this is that PostgreSQL doesn't support `use database` statements to switch database in code, so it has to be done manually.

Once the database is ready, open your terminal, navigate to the project folder and execute the following statemens:

`> sbt clean compile`

To run **Logs generator** service, execute:

`> sbt "runMain com.coding.challenge.worker.LogsKafkaProducer -f apache_combined -n 5"`

This will start the service, generate 5 logs in **apache_combined** format (due to the `-f apache_combined -n 5` command arguments) and exit. For different command line options check documentation for `flog` on [https://github.com/mingrammer/flog#usage](https://github.com/mingrammer/flog#usage)

To run **Logs analyzer** service, execute:

`> sbt "runMain com.coding.challenge.worker.LogsAnalyzerSpark"`

This command will start the service, continually read the stream of data from Kafka, calculate unique visits using Spark, and update the latest counts in PostgreSQL database.
