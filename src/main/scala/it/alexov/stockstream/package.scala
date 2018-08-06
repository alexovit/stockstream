package it.alexov

import java.time.Instant
import java.util.Date

import io.circe.{Encoder, Json}
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

package object stockstream {

  case class Candle(ticker: String, timestamp: Instant, open: Double, high: Double, low: Double, close: Double, volume: Long)
  object Candle {
    def apply(ticker: String, buffer: List[TickerValue],date: Instant): Candle = {
      val open = buffer.find(_.timestamp == buffer.map(_.timestamp).min).map(_.value).head
      val close = buffer.find(_.timestamp == buffer.map(_.timestamp).max).map(_.value).head
      val low = buffer.map(_.value).min
      val high = buffer.map(_.value).max
      val volume = buffer.map(_.volume).sum
      Candle(ticker,date,open,high,low,close,volume)
    }
  }

  private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC)
  implicit val encodeCandle: Encoder[Candle] = new Encoder[Candle] {
    final def apply(a: Candle): Json = Json.obj(
      ("ticker", Json.fromString(a.ticker)),
      ("timestamp", Json.fromString(fmt.format(a.timestamp))),
      ("open", Json.fromDoubleOrNull(a.open)),
      ("high", Json.fromDoubleOrNull(a.high)),
      ("low", Json.fromDoubleOrNull(a.low)),
      ("close", Json.fromDoubleOrNull(a.close)),
      ("volume", Json.fromLong(a.volume))
    )
  }

  case class TickerValue(ticker: String, timestamp: Instant, value: Double, volume: Long)

  case class ServerConfig(host: String, port:Int)

  case class Config(upstream: ServerConfig, downstream: ServerConfig)

}
