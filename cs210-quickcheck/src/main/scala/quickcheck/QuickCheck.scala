package quickcheck

import org.scalacheck._
import Arbitrary._
import Gen._
import Prop.{BooleanOperators => _, _}

abstract class QuickCheckHeap extends Properties("Heap") with IntHeap {

  lazy val genHeap: Gen[H] = 
    //oneOf(const(empty),
      for 
        k <- arbitrary[Int]
        m <- oneOf(const(empty), genHeap)
      yield insert(k, m)
    //)

  implicit lazy val arbHeap: Arbitrary[H] = Arbitrary(genHeap)

  property("gen1") = forAll { (h: H) =>
    val m = if isEmpty(h) then 0 else findMin(h)
    findMin(insert(m, h)) == m
  }

  property("min2") = forAll { (n1: Int, n2: Int) =>
    val h = insert(n2, insert(n1, empty))
    if (n1 < n2) then findMin(h) == n1
    else findMin(h) == n2
    }
  
  property("del") = forAll { (n: Int) =>
    val h = empty
    deleteMin(insert(n, h)) == empty
    }

  property("sort") = forAll { (h: H) =>
    def aux(h: H, ls: List[Int]): List[Int] =
      if isEmpty(h) then ls
      else findMin(h) :: aux(deleteMin(h), ls) 
    val l = aux(h, List())
    l.sorted(ord) == l
  }

  property("minmeld") = forAll { (h1: H, h2: H) =>
    def aux(h1: H, h2: H): Boolean =
      if (isEmpty(h1) && isEmpty(h2)) then true
      else {
        val m1 = findMin(h1)
        val m2 = findMin(h2)
        m1 == m2 && aux(deleteMin(h1), deleteMin(h2))
      }
    aux(meld(h1, h2),
      meld(deleteMin(h1), insert(findMin(h1), h2)))
  }
}
