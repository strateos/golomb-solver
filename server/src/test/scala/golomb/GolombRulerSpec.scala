package golomb

import org.scalatest._

class GolombRulerSpec extends FlatSpec with Matchers {
  behavior of "Very small orders should always produce optimal results"

  "GolombRuler" should "solve with order 1" in {
    val order = 1
    val timeout = 10
    val marks = GolombRuler.solve(None, order, timeout).get
    // The only valid optimal ruler of order 1 is [0]
    assert(marks === Array(0))
  }

  "GolombRuler" should "solve with order 2" in {
    val order = 2
    val timeout = 10
    val marks = GolombRuler.solve(None, order, timeout).get
    // The only valid optimal ruler of order 2 is [0,1]
    assert(marks === Array(0, 1))
  }

  "GolombRuler" should "solve with order 3" in {
    val order = 3
    val timeout = 10
    val marks = GolombRuler.solve(None, order, timeout).get
    // The only valid optimal ruler of order 2 is [0,1,3]
    assert(marks === Array(0, 1, 3))
  }

  behavior of "orders that have multiple optimal solutions"

  // Need to first stop allowing duplicate rulers based on reflection
  // for this to work. Eg sometimes we'll produce [0.0, 2.0, 7.0, 10.0, 11.0]
  // which is equivalent to the canonical solution [0.0, 1.0, 4.0, 9.0, 11.0] under reflection
  "GolombRuler" should "solve with order 5" ignore {
    val order = 5
    val timeout = 20
    val marks = GolombRuler.solve(None, order, timeout).get
    // Only two optimal solutions exist
    assert(
      Seq(
        Array(0.0, 1.0, 4.0, 9.0, 11.0),
        Array(0.0, 2.0, 7.0, 8.0, 11.0)
      ).contains(marks)
    )
  }
}
