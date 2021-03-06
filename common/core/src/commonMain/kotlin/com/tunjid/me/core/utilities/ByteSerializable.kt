/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tunjid.me.core.utilities

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

/**
 * A declaration for type that can be serialized as bytes.
 * Currently does this using CBOR; Protobufs were evaluated but they're too terse an don't work
 * very well for preserving information for lists or maps
 */
interface ByteSerializable

/**
 * Serializes a [ByteSerializable] to a [ByteArray] and deserializes a [ByteArray] back into
 * its original type
 */
interface ByteSerializer {
    val format: BinaryFormat
}

inline fun <reified T : ByteSerializable> ByteSerializer.fromBytes(bytes: ByteArray): T =
    format.decodeFromByteArray(bytes)

inline fun <reified T : ByteSerializable> ByteSerializer.toBytes(item: T): ByteArray =
    format.encodeToByteArray(value = item)

class DelegatingByteSerializer(
    override val format: BinaryFormat
) : ByteSerializer
