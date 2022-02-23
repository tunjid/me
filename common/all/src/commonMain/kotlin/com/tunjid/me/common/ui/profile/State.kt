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

package com.tunjid.me.common.ui.profile

import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.ui.FormField
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class State(
    @Transient
    val signedInUser: com.tunjid.me.core.model.User? = null,
    val fields: List<FormField> = listOf(
        FormField(
            id = "First Name",
            value = "",
        ),
        FormField(
            id = "LastName",
            value = "",
        )
    )
) : ByteSerializable

sealed class Action {
    data class FieldChanged(
        val field: FormField
    ): Action()
}