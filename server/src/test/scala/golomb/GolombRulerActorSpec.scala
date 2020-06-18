package golomb

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{FlatSpec, Matchers, WordSpecLike}

class GolombRulerActorSpec
  extends TestKit(ActorSystem("GolombRulerActorSpec"))
    with Matchers
    with WordSpecLike
    with ImplicitSender {

  "starts in idle state" in {
    implicit val system = ActorSystem("actors")
    val golombActor = system.actorOf(Props(classOf[GolombRulerActor], None))
    golombActor ! "state"
    expectMsg(GolombStateIdle)
  }

  "starts solving" in {
    implicit val system = ActorSystem("actors")
    val golombActor = system.actorOf(Props(classOf[GolombRulerActor], None))
    golombActor ! "state"
    expectMsg(GolombStateIdle)
    golombActor ! StartSolve(order = 1, timeout = 5)
    expectMsg(GolombStateSolving)
  }
}