package it.alexov.stockstream

import java.time._
import akka.actor.{Actor, ActorLogging, Props}
import scala.concurrent.duration._

class CacheActor extends Actor with ActorLogging {

  import context.dispatcher

  val cleanScheduler = context.system.scheduler.schedule(30 seconds, 20 minutes, self, CacheActor.Clean)
  var valueCache: scala.collection.mutable.ListBuffer[TickerValue] = new scala.collection.mutable.ListBuffer[TickerValue]()

  def getCandleInterval(start: Instant, end:Instant): Seq[Candle] = valueCache
    .filter(v => start.isBefore(v.timestamp) && end.isAfter(v.timestamp))
    .groupBy(_.ticker)
    .map(tuple => {
      val (ticker, buffer) = tuple
      Candle(ticker,buffer.toList,start)
    }).toSeq

  def lastCandles(ref:Instant, num: Int): Seq[Candle] = (1 to num).reverse.zip((0 until num).reverse).flatMap(t => {
    val (start,end) = t
    val result = getCandleInterval(ref.minusSeconds(60*start),ref.minusSeconds(60*end))
    result
  })

  def secondStart = {
    val date = Instant.now()
    val zone = ZoneId.systemDefault()
    LocalDateTime.ofInstant(date, zone).withSecond(0).toInstant(zone.getRules.getOffset(date))
  }

  override def receive: Receive = {
    case CacheActor.Update(value) =>
      valueCache += value
    case CacheActor.MinuteCandle(num) =>
      sender() ! lastCandles(secondStart,num)
    case CacheActor.Clean =>
      valueCache = valueCache.filter(_.timestamp.isBefore(secondStart.minusSeconds(20*60)))
  }

}

object CacheActor {

  def props() = Props(classOf[CacheActor])

  sealed trait CacheActorCommand
  case class Update(value:TickerValue) extends CacheActorCommand
  case class MinuteCandle(last: Int) extends CacheActorCommand
  case class Clean() extends CacheActorCommand

}