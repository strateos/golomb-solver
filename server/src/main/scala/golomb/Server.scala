package golomb

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{complete, _}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl._
import StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.StandardRoute
import org.json4s.native.Serialization.write

import scala.concurrent.Promise
import scala.io.StdIn

case class StatusResponse(status: String)

object Server extends App {
  // Returns successful status code with a json payload
  // There are libs to do this if we end up re-inventing the wheel too often
  def completeJson(jsonString: String): StandardRoute = {
    complete(
      HttpResponse(
        status = OK,
        entity = HttpEntity(
          ContentType(MediaTypes.`application/json`),
          jsonString
        )
      )
    )
  }

  override def main(args: Array[String]) {
    implicit val system = ActorSystem("actors")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val formats = org.json4s.DefaultFormats // for writing json

    // The source to broadcast
    val dataSource = Source.queue[String](1000, OverflowStrategy.backpressure)
    val p = Promise[SourceQueueWithComplete[String]]
    val s = dataSource.mapMaterializedValue { m =>
      p.trySuccess(m)
      m
    }

    // get the materialized queue so we can push into it
    var golombActor: ActorRef = null // TODO this is ugly, will cause race condition
    p.future.map { queue =>
      golombActor = system.actorOf(Props(classOf[GolombRulerActor], queue))
    }


    // Go via BroadcastHub to allow multiple clients to connect
    val runnableGraph: RunnableGraph[Source[String, NotUsed]] =
      s.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.right)

    val producer: Source[String, NotUsed] = runnableGraph.run()
    producer.runWith(Sink.ignore) // avoid back pressure on original flow

    // TODO Ignore data sent from client
    val wsHandler: Flow[Message, Message, NotUsed] =
      Flow[Message]
        .merge(producer) // Stream the data we want to the client
        .map(l => TextMessage(l.toString))

    // Just let any client connect. NOTE This is only safe in local dev mode.
    val responseHeaders = List(RawHeader("Access-Control-Allow-Origin", "*"))

    val route = respondWithDefaultHeaders(responseHeaders) {
      path("ping") {
        get {
          completeJson(write(StatusResponse(status = "up")))
        }
      } ~ path("crash") {
        get {
          sys.error("Internal Server Error")
        }
      } ~ path("solve") {
        /**
         * Request to kick off a search for a golomb ruler of a particular order
         * TODO This should really be a POST
         */
        get {
          parameters("order", "timeout") { (order, timeout) =>
            val validatedOrder = order.toInt
            val validatedTimeout = timeout.toInt
            if (validatedOrder <= 0) {
              complete(HttpResponse(BadRequest, entity = "'order' must be non negative integer"))
            } else if (validatedTimeout <= 0) {
              complete(HttpResponse(BadRequest, entity = "'timeout' must be non negative integer"))
            } else {
              golombActor ! StartSolve(validatedOrder, timeout.toInt)
              completeJson(write(StatusResponse(status = "solving")))
            }
          }
        }
      } ~ path("wstest0") {
        get {
          // Just a fun test of how web sockets work
          extractUpgradeToWebSocket { upgrade =>
            val numbers = Source(1 to 10).map(i => TextMessage(i.toString))
            complete(upgrade.handleMessagesWithSinkSource(Sink.ignore, numbers))
          }
        }
      } ~ path("ws") {
        get {
          handleWebSocketMessages(wsHandler)
        }
      }
    }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
