package golomb

import akka.actor.Actor
import akka.stream.scaladsl.SourceQueueWithComplete
import ilog.concert._
import ilog.cp._
import org.json4s.native.Serialization.write

/*
  In mathematics, a Golomb ruler is a set of marks at integer positions along
  an imaginary ruler such that no two pairs of marks are the same distance apart.
  The number of marks on the ruler is its order, and the largest distance
  between two of its marks is its length.

  See https://en.wikipedia.org/wiki/Golomb_ruler for more information.

  The goal of this app is to try to find optimal rulers for a given order.
  Optimality means that any other ruler of that order is of the same or greater length.
  Translation and reflection of a Golomb ruler are considered trivial, so the smallest mark is customarily put
  at 0 and the next mark at the smaller of its two possible values.

  Examples:
  * For order 5: 2 optimal and perfect solutions 0 1 4 9 11 ; 0 2 7 8 11
  * For order 10: 1 optimal solution 0 1 6 10 23 26 34 41 53 55
 */
object GolombRuler {
  // Setup CPLEX
  System.loadLibrary("cp_wrap_cpp_java1290")

  // Setup for sending json payloads over the queue
  implicit val formats = org.json4s.DefaultFormats // for writing json
  trait Message
  case class QueueMessage(name: String) extends Message
  case class NewOrderMessage(name: String, data: String) extends Message

  /*
    The problem statement:
    We will be given an order which is an int. This determines the number of marks.

    Known:
      - Number of marks (order)
    Unknown:
      - The value of each mark.
    Constraints:
      - No two pairs of marks can have the same distance apart
      - (bonus) Remove duplicate solutions like reflections/translations
        - first mark can always be initialized at 0 to ensure no translation duplicates
      - (bonus) make it a 'perfect' golomb ruler -- all distances <= the order can be measured
    Objectives:
      - Minimize length (the max of the marks)
   */

  /*
    @param resultsQueue Queue to push events to during solve
    @param timeout Maximum time to spend in solve (seconds)
   */
  def solve(resultsQueue: SourceQueueWithComplete[String], order: Int = 5, timeout: Int = 30): Unit = {
    val model: IloCP = new IloCP()
    println(s"Solving for order $order...")

    // Dvars: All marks
    val marks = model.intVarArray(order, 0, 1000, "marks") // Note name is required for comparison later
    // Constraint: All marks different
    model.add(model.allDiff(marks.asInstanceOf[Array[IloIntExpr]]))

    // Constraint: Pin first mark to 0 to ensure no translation duplicates
    model.add(model.eq(marks.head, 0))

    // Constraint: Ensure no pair of marks has same distance as any other pair
    val deltas = marks.zipWithIndex.flatMap { rowAndIndex =>
      marks.zipWithIndex.flatMap {
        case (_, index2) if index2 == rowAndIndex._2 => None
        case (mark2, _) => Some(model.diff(rowAndIndex._1, mark2))
      }
    }
    model.add(model.allDiff(deltas))

    // Objective: minimize the length
    model.add(
      model.minimize(
        model.max(marks.asInstanceOf[Array[IloIntExpr]])
      )
    )

    // Setup callbacks to report search status
    def postMessage(message: Message): Unit = {
      resultsQueue.offer(write(message))
    }
    class SearchCallback extends IloCP.Callback {
      override def invoke(model: IloCP, i: Int): Unit = {
        if (i == IloCP.Callback.StartSearch) {
          postMessage(QueueMessage(name = "StartSearch"))
        } else if (i == IloCP.Callback.Periodic) {
          val current: Array[Double] = marks.map { m =>
            try {
              model.getValue(m)
            } catch {
              case _: Throwable =>
                0
            }
          }
          val marksStr = current.sorted.mkString(", ")
          val currentOrder: Double = current.max(cmp = Ordering.Double)
          postMessage(NewOrderMessage(name = "NewOrder", data = currentOrder.toString)) // TODO should send a number
          resultsQueue.offer(s"Periodic:$marksStr")
        } else if (i == IloCP.Callback.ObjBound) {
          val bound = model.getObjBound()
          resultsQueue.offer(s"ObjBound:$bound")
        } else if (i == IloCP.Callback.Solution) {
          val markValues = marks.map(model.getValue(_)).sorted.mkString(", ")
          resultsQueue.offer(s"NewSolution:$markValues")
        } else if (i == IloCP.Callback.EndSearch) {
          resultsQueue.offer("EndSearch")
          println("*** EndSearch")
          println("Num solutions: ")
          println(model.getInfo(IloCP.IntInfo.NumberOfSolutions))
          println("SolveTime (seconds): ")
          println(model.getInfo(IloCP.DoubleInfo.SolveTime))
        }
      }
    }
    model.addCallback(new SearchCallback)

    // Solve
    model.setParameter(IloCP.DoubleParam.TimeLimit, timeout)
    if (model.solve()) {
      marks.map(model.getValue(_)).sorted.foreach(println)
      val solutionStr = marks.map(model.getValue(_)).sorted.mkString(", ")
      resultsQueue.offer(s"Final:$solutionStr")
    } else {
      resultsQueue.offer("Final:None")
    }
    model.end()
  }

  def solutionStr(marks: Array[IloIntVar], model: IloCP): String = {
    val current: Array[Double] = marks.map { m =>
      try {
        model.getValue(m)
      } catch {
        case _: Throwable =>
          0 // ugly default, necessary when uninitialized
      }
    }
    current.sorted.mkString(", ")
  }
}

case object GolombStateIdle
case object GolombStateSolving
case class StartSolve(order: Int, timeout: Int)

class GolombRulerActor(resultsQueue: SourceQueueWithComplete[String]) extends Actor {
  var solving: Boolean = false

  def receive = {
    case StartSolve(order, timeout) =>
      if (solving) {
        sender() ! GolombStateSolving
      } else {
        println("*** Actor receive solve")
        solving = true
        sender() ! GolombStateSolving
        GolombRuler.solve(resultsQueue, order, timeout)
        solving = false
      }
    case "test" => println("*** Actor receive test")
    case "state" => {
      println("*** Actor receive state")
      if (solving) {
        sender() ! GolombStateSolving
      } else {
        sender() ! GolombStateIdle
      }
    }
    case _: String => println("Unexpected message received")
  }
}