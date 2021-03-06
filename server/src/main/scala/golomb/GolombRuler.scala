package golomb

import akka.actor.Actor
import akka.stream.scaladsl.SourceQueueWithComplete
import ilog.concert._
import ilog.cp.IloCP.{IntInfo}
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

  // Messages sent over the result queue
  // TODO Better type safety. Also de-dup need of `name` field.
  type Solution = Array[Double]

  trait Message

  case class QueueMessage(name: String) extends Message

  case class NewOrderMessage(name: String, data: Double) extends Message

  case class ObjBoundMessage(name: String, data: Double) extends Message

  case class PeriodicMessage(name: String, data: CPInfoMetrics) extends Message

  case class NewSolutionMessage(name: String, data: Solution) extends Message

  case class EndSearch(name: String) extends Message

  case class FinalMessage(name: String, data: Boolean) extends Message

  case class GapMessage(name: String, data: Double) extends Message

  // CPLEX runtime metrics
  case class CPInfoMetrics(
                            numberOfChoicePoints: Int,
                            numberOfFails: Int,
                            numberOfBranches: Int,
                            memoryUsage: Int,
                            numberOfSolutions: Int
                          )

  /**
   * Returns a data structure containing various information about a given model.
   *
   * @param  model The CP model to extract information from
   * @return CPInfoMetrics Information about the CP model
   */
  def getModelMetrics(model: IloCP): CPInfoMetrics = {
    CPInfoMetrics(
      model.getInfo(IntInfo.NumberOfChoicePoints),
      model.getInfo(IntInfo.NumberOfFails),
      model.getInfo(IntInfo.NumberOfBranches),
      model.getInfo(IntInfo.MemoryUsage),
      model.getInfo(IntInfo.NumberOfSolutions)
    )
  }

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
    @param order        The number of marks
    @param timeout      Maximum time to spend in solve (seconds)
    @return             Array[Double] representing the list of mark positions
   */
  def solve(
             resultsQueue: Option[SourceQueueWithComplete[String]],
             order: Int = 5,
             timeout: Int = 30
           ): Option[Array[Double]] = {

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
    // https://developer.ibm.com/docloud/blog/2019/12/17/new-callback-functionality-in-cp-optimizer/
    // TODO use getObjValue to get current objective value
    // TODO fetch cp metrics, like NumberOfSolutions, MemoryUsage, NumberOfFails, ...
    def postMessage(message: Message): Unit = {
      resultsQueue.foreach(_.offer(write(message)))
    }

    class SearchCallback extends IloCP.Callback {
      override def invoke(model: IloCP, i: Int): Unit = {
        if (i == IloCP.Callback.StartSearch) {
          postMessage(QueueMessage(name = "StartSearch"))
        } else if (i == IloCP.Callback.Periodic) {
          postMessage(PeriodicMessage(name = "Periodic", data = getModelMetrics(model)))
        } else if (i == IloCP.Callback.ObjBound) {
          val bound = model.getObjBound()
          postMessage(ObjBoundMessage(name = "ObjBound", data = bound))
        } else if (i == IloCP.Callback.Solution) {
          val marksArray = marks.map(model.getValue(_)).sorted
          postMessage(NewSolutionMessage(name = "NewSolution", data = marksArray))
          postMessage(GapMessage(name = "Gap", data = model.getObjGap()))
        } else if (i == IloCP.Callback.EndSearch) {
          postMessage(EndSearch(name = "EndSearch"))
        }
      }
    }
    model.addCallback(new SearchCallback)

    // Solve
    model.setParameter(IloCP.DoubleParam.TimeLimit, timeout)
    if (model.solve()) {
      val marksSorted = marks.map(model.getValue(_)).sorted
      model.end()
      postMessage(FinalMessage(name = "Final", data = true)) // update client
      Some(marksSorted)
    } else {
      postMessage(FinalMessage(name = "Final", data = false))
      None
    }
  }
}

case object GolombStateIdle

case object GolombStateSolving

case class StartSolve(order: Int, timeout: Int)

class GolombRulerActor(resultsQueue: Option[SourceQueueWithComplete[String]]) extends Actor {
  var solving: Boolean = false

  def receive = {
    case StartSolve(order, timeout) =>
      if (solving) {
        sender() ! GolombStateSolving
      } else {
        solving = true
        sender() ! GolombStateSolving
        GolombRuler.solve(resultsQueue, order, timeout)
        solving = false
      }
    case "state" => {
      if (solving) {
        sender() ! GolombStateSolving
      } else {
        sender() ! GolombStateIdle
      }
    }
    case _: String => println("Unexpected message received")
  }
}