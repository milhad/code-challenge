package com.coding.challenge.parser

import java.util.regex.Pattern
import java.text.SimpleDateFormat
import java.util.Locale
import scala.util.control.Exception._
import java.util.regex.Matcher
import scala.util.{Try, Success, Failure}

/**
  * A sample record:
  * 94.102.63.11 - - [21/Jul/2009:02:48:13 -0700] "GET / HTTP/1.1" 200 18209 "http://acme.com/foo.php" "Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.0)"
  *
  * I put this code in the 'class' so (a) the pattern could be pre-compiled and (b) the user can create
  * multiple instances of this parser, in case they want to work in a multi-threaded way.
  * I don't know that this is necessary, but I think it is for this use case.
  *
  */

@SerialVersionUID(100L)
class AccessLogParser extends Serializable {

  // 227.13.11.84 - - [02/09/2019:23:46:23 +0200] "GET /applications" 205 93244 "https://www.nationalgenerate.info/revolutionary" "Mozilla/5.0 (Macintosh; PPC Mac OS X 10_9_10 rv:3.0) Gecko/1931-11-08 Firefox/35.0"
  private val ddd = "\\d{1,3}"                      // at least 1 but not more than 3 times (possessive)
  private val ip = s"($ddd\\.$ddd\\.$ddd\\.$ddd)?"  // like `123.456.7.89`
  private val client = "(\\S+)"                     // '\S' is 'non-whitespace character'
  private val user = "(\\S+)"
  private val dateTime = "(\\[.+?\\])"              // like `[21/Jul/2009:02:48:13 -0700]`
  private val request = "\"(.*?)\""                 // any number of any character, reluctant
  private val status = "(\\d{3})"
  private val bytes = "(\\S+)"                      // this can be a "-"
  private val referer = "\"(.*?)\""
  private val agent = "\"(.*?)\""
  private val regex = s"$ip $client $user $dateTime $request $status $bytes $referer $agent"
  private val p = Pattern.compile(regex)

  /**
    * note: group(0) is the entire record that was matched (skip it)
    * @param record Assumed to be an Apache access log combined record.
    * @return An AccessLogRecord instance wrapped in an Option.
    */
  def parseRecord(record: String): Option[AccessLogRecord] = {
    val matcher = p.matcher(record)
    if (matcher.find) {
      Some(buildAccessLogRecord(matcher))
    } else {
      None
    }
  }

  /**
    * Same as parseRecord, but returns a "Null Object" version of an AccessLogRecord
    * rather than an Option.
    *
    * @param record Assumed to be an Apache access log combined record.
    * @return An AccessLogRecord instance. This will be a "Null Object" version of an
    * AccessLogRecord if the parsing process fails. All fields in the Null Object
    * will be empty strings.
    */
  def parseRecordReturningNullObjectOnFailure(record: String): AccessLogRecord = {
    val matcher = p.matcher(record)
    if (matcher.find) {
      buildAccessLogRecord(matcher)
    } else {
      AccessLogParser.nullObjectAccessLogRecord
    }
  }

  private def buildAccessLogRecord(matcher: Matcher) = {
    AccessLogRecord(
      matcher.group(1),
      matcher.group(2),
      matcher.group(3),
      parseDateField(matcher.group(4)),
      matcher.group(5),
      matcher.group(6),
      matcher.group(7),
      matcher.group(8),
      matcher.group(9))
  }

  /**
    * @param A String that looks like "[21/08/2009:02:48:13 -0700]"
    */
  private def parseDateField(field: String): Option[java.util.Date] = {
    val dateRegex = """\[(\d{2})\/(\d{2})\/(\d{4}):(\d{2}):(\d{2}):(\d{2})\s([\+\-]\d{4})]"""
    val datePattern = Pattern.compile(dateRegex)
    val dateMatcher = datePattern.matcher(field)
    if (dateMatcher.find) {
      val dateString = field.substring(1, field.length - 1) //dateMatcher.group(1)
      // println("***** DATE STRING" + dateString)
      // HH is 0-23; kk is 1-24
      val dateFormat = new SimpleDateFormat("dd/MM/yyyy:HH:mm:ss Z", Locale.ENGLISH)
      allCatch.opt(dateFormat.parse(dateString))  // return Option[Date]
    } else {
      None
    }
  }
}

/**
  * A sample record:
  * 94.102.63.11 - - [21/Jul/2009:02:48:13 -0700] "GET / HTTP/1.1" 200 18209 "http://acme.com/foo.php" "Mozilla/4.0 (compatible; MSIE 5.01; Windows NT 5.0)"
  */
object AccessLogParser {

  val nullObjectAccessLogRecord = AccessLogRecord("", "", "", None, "", "", "", "", "")

  /**
    * @param A String like "GET /the-uri-here HTTP/1.1"
    * @return A Tuple3(requestType, uri, httpVersion). requestType is GET, POST, etc.
    *
    * Returns a Tuple3 of three blank strings if the method fails.
    */
  def parseRequestField(request: String): Option[Tuple3[String, String, String]] = {
    val arr = request.split(" ")
    if (arr.size == 3) Some((arr(0), arr(1), arr(2))) else None
  }

}




