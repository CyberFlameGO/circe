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

package io.circe.derivation

import scala.quoted.*
import scala.deriving.*
import scala.compiletime.constValue

/**
 * Original code by Dmytro Mitin, with slight modifications by Simão Martins.
 * See: https://stackoverflow.com/questions/68421043/type-class-derivation-accessing-default-values
 */

trait Default[T] extends Serializable:
  type Out <: Tuple
  def defaults: Out

  def defaultAt(index: Int): Option[Any] = defaults match
    case _: EmptyTuple           => None
    case defaults: NonEmptyTuple => defaults(index).asInstanceOf[Option[Any]]

object Default:
  transparent inline given mkDefault[T](using mirror: Mirror.Of[T]): Default[T] =
    new Default[T]:
      type Out = Tuple.Map[mirror.MirroredElemTypes, Option]
      lazy val defaults: Out =
        val size = constValue[Tuple.Size[mirror.MirroredElemTypes]]
        getDefaults[T](size).asInstanceOf[Out]

  inline def getDefaults[T](inline s: Int): Tuple = ${ getDefaultsImpl[T]('s) }

  def getDefaultsImpl[T](s: Expr[Int])(using Quotes, Type[T]): Expr[Tuple] =
    import quotes.reflect.*

    val n = s.asTerm.underlying.asInstanceOf[Literal].constant.value.asInstanceOf[Int]

    val companion = TypeRepr.of[T].typeSymbol.companionModule

    val expressions: List[Expr[Option[Any]]] = List.tabulate(n) { i =>
      val termOpt = companion.declaredMethod(s"$$lessinit$$greater$$default$$${i + 1}").headOption.map { s =>
        val select = Ref(companion).select(s)
        TypeRepr.of[T].typeArgs match
          case Nil      => select
          case typeArgs => select.appliedToTypes(typeArgs)
      }

      termOpt match
        case None     => Expr(None)
        case Some(et) => '{ Some(${ et.asExpr }) }
    }
    Expr.ofTupleFromSeq(expressions)
