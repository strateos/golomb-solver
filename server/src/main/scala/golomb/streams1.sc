// Playground for testing akka streams

import akka.{NotUsed, Done}
import akka.stream._
import akka.stream.scaladsl._
import scala.concurrent._
import akka.actor.ActorSystem

implicit val system = ActorSystem("main-actor-system")
implicit val executionContext = system.dispatcher
implicit val materializer = ActorMaterializer()

// Simply print 1 to 100
//val source: Source[Int, NotUsed] = Source(1 to 100)
//val done: Future[Done] = source.runWith(Sink.foreach(println))
//done.onComplete(_ => system.terminate())

// Factorials 1 to 100
//val source: Source[Int, NotUsed] = Source(1 to 100)
//val factorials = source.scan(BigInt(1))((acc, next) => acc * next)
//val done: Future[Done] = factorials.runWith(Sink.foreach(println))
//done.onComplete(_ => system.terminate())
