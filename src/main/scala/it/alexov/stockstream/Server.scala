package it.alexov.stockstream

import java.io.IOException
import java.nio.ByteOrder
import java.time.{Instant, LocalDateTime}
import java.util.concurrent.TimeUnit
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream._
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.scaladsl.{Concat, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Tcp}
import akka.util.{ByteString, Timeout}
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try
import io.circe.syntax._
import io.circe.generic.auto._
import akka.pattern._
import it.alexov.stockstream.CacheActor.CacheActorCommand

class Server(implicit system: ActorSystem, materializer: ActorMaterializer, log: LoggingAdapter, serverConfig: Config) {

  implicit val askTimeout = Timeout(500,TimeUnit.MILLISECONDS)

  val cacheActor = system.actorOf(CacheActor.props())

  private def toNextMinute = {
   val nextMinute = LocalDateTime.now.withSecond(0).withNano(0).plusMinutes(1)
   FiniteDuration(java.time.Duration.between(LocalDateTime.now,nextMinute).toNanos,TimeUnit.NANOSECONDS)
  }

  def upstreamConverter(byteString: ByteString): Try[TickerValue] = Try({
    implicit val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
    val iter = byteString.iterator
    val msgLength = iter.getShort
    if(iter.len != msgLength) {
      log.error("Broken message received")
      throw new IOException("Broken message received")
    }
    val ts = iter.getLong
    val tickerLength = iter.getShort
    val ticker = iter.getByteString(tickerLength).utf8String
    val value = iter.getDouble
    val volume = iter.getInt
    val result = TickerValue(ticker,Instant.ofEpochMilli(ts),value,volume)
    log.info(result.toString)
    result
  })

  def downstreamConverter(candle: Candle): String = candle.asJson.toString

  def downstreamFlow: Flow[ByteString,ByteString,_] = Flow.fromGraph(GraphDSL.create(Source.actorRef[ByteString](10000, OverflowStrategy.fail)) { implicit builder ⇒ wsChannelSource ⇒
    import GraphDSL.Implicits._

    val empty = builder.add(Flow[ByteString].to(Sink.ignore))
    val firstTen = builder.add(Source.single(CacheActor.MinuteCandle(10)))
    val minuteCandle = builder.add(Source.tick(toNextMinute, 1 minute, CacheActor.MinuteCandle(1)))
    val toResult = builder.add(Flow[Seq[Candle]].map(_.map(downstreamConverter))
      .map(_.mkString("\n"))
      .map(ByteString(_))
    )
    val merge = builder.add(Concat[CacheActor.CacheActorCommand](2))
    val ask = Flow[CacheActorCommand].ask[Seq[Candle]](cacheActor)

    wsChannelSource ~> Sink.ignore
    firstTen ~> merge ~> ask ~> toResult
    minuteCandle ~> merge
    FlowShape(empty.in, toResult.out)
  })

  def upstreamFlow(upstream: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]]): RunnableGraph[NotUsed] =  Source(List(ByteString.empty))
    .via(upstream)
    .map(upstreamConverter)
    .filter(_.isSuccess)
    .map(t => CacheActor.Update(t.get))
    .to(Sink.actorRef(cacheActor,""))

  def run(): Future[Done] = {
    val upstream: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
      Tcp().outgoingConnection(serverConfig.upstream.host, serverConfig.upstream.port)
    val downstream: Source[IncomingConnection, Future[ServerBinding]] =
      Tcp().bind(serverConfig.downstream.host, serverConfig.downstream.port)

    upstreamFlow(upstream).run()
    downstream.runForeach{ conn =>
      conn.handleWith(downstreamFlow)
    }
  }

}
