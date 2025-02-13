/*
 * Copyright 2023 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import cats.kernel.Eq
import cats.kernel.instances.all._
import cats.syntax.eq._
import io.circe.{ Codec, Decoder, Encoder, Json }
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import org.scalacheck.{ Arbitrary, Gen }

object DerivesSuite {
  case class Box[A](a: A) derives Decoder, Encoder.AsObject

  object Box {
    implicit def eqBox[A: Eq]: Eq[Box[A]] = Eq.by(_.a)
    implicit def arbitraryBox[A](implicit A: Arbitrary[A]): Arbitrary[Box[A]] = Arbitrary(A.arbitrary.map(Box(_)))
  }

  case class Qux[A](i: Int, a: A, j: Int) derives Codec.AsObject

  object Qux {
    implicit def eqQux[A: Eq]: Eq[Qux[A]] = Eq.by(q => (q.i, q.a, q.j))

    implicit def arbitraryQux[A](implicit A: Arbitrary[A]): Arbitrary[Qux[A]] =
      Arbitrary(
        for {
          i <- Arbitrary.arbitrary[Int]
          a <- A.arbitrary
          j <- Arbitrary.arbitrary[Int]
        } yield Qux(i, a, j)
      )
  }

  case class Wub(x: Long) derives Codec.AsObject

  object Wub {
    implicit val eqWub: Eq[Wub] = Eq.by(_.x)
    implicit val arbitraryWub: Arbitrary[Wub] = Arbitrary(Arbitrary.arbitrary[Long].map(Wub(_)))
  }

  sealed trait Foo derives Codec.AsObject
  case class Bar(i: Int, s: String) extends Foo
  case class Baz(xs: List[String]) extends Foo
  case class Bam(w: Wub, d: Double) extends Foo derives Codec.AsObject

  object Bar {
    implicit val eqBar: Eq[Bar] = Eq.fromUniversalEquals
    implicit val arbitraryBar: Arbitrary[Bar] = Arbitrary(
      for {
        i <- Arbitrary.arbitrary[Int]
        s <- Arbitrary.arbitrary[String]
      } yield Bar(i, s)
    )

    implicit val decodeBar: Decoder[Bar] = Decoder.forProduct2("i", "s")(Bar.apply)
    implicit val encodeBar: Encoder[Bar] = Encoder.forProduct2("i", "s") {
      case Bar(i, s) => (i, s)
    }
  }

  object Baz {
    implicit val eqBaz: Eq[Baz] = Eq.fromUniversalEquals
    implicit val arbitraryBaz: Arbitrary[Baz] = Arbitrary(
      Arbitrary.arbitrary[List[String]].map(Baz.apply)
    )

    implicit val decodeBaz: Decoder[Baz] = Decoder[List[String]].map(Baz(_))
    implicit val encodeBaz: Encoder[Baz] = Encoder.instance {
      case Baz(xs) => Json.fromValues(xs.map(Json.fromString))
    }
  }

  object Bam {
    implicit val eqBam: Eq[Bam] = Eq.fromUniversalEquals
    implicit val arbitraryBam: Arbitrary[Bam] = Arbitrary(
      for {
        w <- Arbitrary.arbitrary[Wub]
        d <- Arbitrary.arbitrary[Double]
      } yield Bam(w, d)
    )
  }

  object Foo {
    implicit val eqFoo: Eq[Foo] = Eq.fromUniversalEquals

    implicit val arbitraryFoo: Arbitrary[Foo] = Arbitrary(
      Gen.oneOf(
        Arbitrary.arbitrary[Bar],
        Arbitrary.arbitrary[Baz],
        Arbitrary.arbitrary[Bam]
      )
    )
  }

  sealed trait RecursiveAdtExample derives Codec.AsObject
  case class BaseAdtExample(a: String) extends RecursiveAdtExample derives Codec.AsObject
  case class NestedAdtExample(r: RecursiveAdtExample) extends RecursiveAdtExample derives Codec.AsObject

  object RecursiveAdtExample {
    implicit val eqRecursiveAdtExample: Eq[RecursiveAdtExample] = Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveAdtExample] = if (depth < 3)
      Gen.oneOf(
        Arbitrary.arbitrary[String].map(BaseAdtExample(_)),
        atDepth(depth + 1).map(NestedAdtExample(_))
      )
    else Arbitrary.arbitrary[String].map(BaseAdtExample(_))

    implicit val arbitraryRecursiveAdtExample: Arbitrary[RecursiveAdtExample] =
      Arbitrary(atDepth(0))
  }

  case class RecursiveWithOptionExample(o: Option[RecursiveWithOptionExample]) derives Codec.AsObject

  object RecursiveWithOptionExample {
    implicit val eqRecursiveWithOptionExample: Eq[RecursiveWithOptionExample] =
      Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveWithOptionExample] = if (depth < 3)
      Gen.oneOf(
        Gen.const(RecursiveWithOptionExample(None)),
        atDepth(depth + 1)
      )
    else Gen.const(RecursiveWithOptionExample(None))

    implicit val arbitraryRecursiveWithOptionExample: Arbitrary[RecursiveWithOptionExample] =
      Arbitrary(atDepth(0))
  }

  enum Vegetable derives Codec.AsObject:
    case Potato(species: String)
    case Carrot(length: Double)
    case Onion(layers: Int)
    case Turnip
  object Vegetable:
    given Eq[Vegetable] = Eq.fromUniversalEquals
    given Arbitrary[Vegetable.Potato] = Arbitrary(
      Arbitrary.arbitrary[String].map(Vegetable.Potato.apply)
    )
    given Arbitrary[Vegetable.Carrot] = Arbitrary(
      Arbitrary.arbitrary[Double].map(Vegetable.Carrot.apply)
    )
    given Arbitrary[Vegetable.Onion] = Arbitrary(
      Arbitrary.arbitrary[Int].map(Vegetable.Onion.apply)
    )
    given Arbitrary[Vegetable.Turnip.type] = Arbitrary(Gen.const(Vegetable.Turnip))
    given Arbitrary[Vegetable] = Arbitrary(
      Gen.oneOf(
        Arbitrary.arbitrary[Vegetable.Potato],
        Arbitrary.arbitrary[Vegetable.Carrot],
        Arbitrary.arbitrary[Vegetable.Onion],
        Arbitrary.arbitrary[Vegetable.Turnip.type]
      )
    )

  enum RecursiveEnumAdt derives Codec.AsObject:
    case BaseAdtExample(a: String)
    case NestedAdtExample(r: RecursiveEnumAdt)
  object RecursiveEnumAdt:
    given Eq[RecursiveEnumAdt] = Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveEnumAdt] = if (depth < 3)
      Gen.oneOf(
        Arbitrary.arbitrary[String].map(RecursiveEnumAdt.BaseAdtExample(_)),
        atDepth(depth + 1).map(RecursiveEnumAdt.NestedAdtExample(_))
      )
    else Arbitrary.arbitrary[String].map(RecursiveEnumAdt.BaseAdtExample(_))

    given Arbitrary[RecursiveEnumAdt] = Arbitrary(atDepth(0))

  sealed trait ADTWithSubTraitExample derives Codec.AsObject
  sealed trait SubTrait extends ADTWithSubTraitExample
  case class TheClass(a: Int) extends SubTrait

  object ADTWithSubTraitExample:
    given Arbitrary[ADTWithSubTraitExample] = Arbitrary(Arbitrary.arbitrary[Int].map(TheClass.apply))
    given Eq[ADTWithSubTraitExample] = Eq.fromUniversalEquals
}

class DerivesSuite extends CirceMunitSuite {
  import DerivesSuite._

  checkAll("Codec[Box[Wub]]", CodecTests[Box[Wub]].codec)
  checkAll("Codec[Seq[Foo]]", CodecTests[Seq[Foo]].codec)
  checkAll("Codec[Baz]", CodecTests[Baz].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)
  checkAll("Codec[RecursiveAdtExample]", CodecTests[RecursiveAdtExample].codec)
  checkAll("Codec[RecursiveWithOptionExample]", CodecTests[RecursiveWithOptionExample].codec)
  checkAll("Codec[Vegetable]", CodecTests[Vegetable].codec)
  checkAll("Codec[RecursiveEnumAdt]", CodecTests[RecursiveEnumAdt].codec)
  checkAll("Codec[ADTWithSubTraitExample]", CodecTests[ADTWithSubTraitExample].codec)

  test("Nested sums should not be encoded redundantly") {
    import io.circe.syntax._
    val foo: ADTWithSubTraitExample = TheClass(0)
    val expected = Json.obj(
      "TheClass" -> Json.obj(
        "a" -> 0.asJson
      )
    )
    assert(Encoder[ADTWithSubTraitExample].apply(foo) === expected)
  }
}
