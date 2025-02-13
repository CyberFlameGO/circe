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

package io.circe.generic.encoding

import io.circe.{ Encoder, JsonObject }
import shapeless.{ LabelledGeneric, Lazy }

abstract class DerivedAsObjectEncoder[A] extends Encoder.AsObject[A]

object DerivedAsObjectEncoder {
  implicit def deriveEncoder[A, R](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprAsObjectEncoder[R]]
  ): DerivedAsObjectEncoder[A] = new DerivedAsObjectEncoder[A] {
    final def encodeObject(a: A): JsonObject = encode.value.encodeObject(gen.to(a))
  }
}
