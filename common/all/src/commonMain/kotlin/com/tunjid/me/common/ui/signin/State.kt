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

package com.tunjid.me.common.ui.signin

import com.tunjid.me.core.utilities.ByteSerializable
import com.tunjid.me.core.model.SessionRequest
import com.tunjid.me.common.ui.common.FormField
import kotlinx.serialization.Serializable

@Serializable
data class State(
    val isSignedIn: Boolean = false,
    val isSubmitting: Boolean = false,
    val fields: List<FormField> = listOf(
        FormField(
            id = "username",
            value = "",
        ),
        FormField(
            id = "password",
            value = "",
        )
    )
) : ByteSerializable

val State.submitButtonEnabled: Boolean get() = !isSignedIn && !isSubmitting

val State.sessionRequest: com.tunjid.me.core.model.SessionRequest
    get() = fields.associateBy { it.id }.let { formMap ->
        com.tunjid.me.core.model.SessionRequest(
            username = formMap.getValue("username").value,
            password = formMap.getValue("password").value,
        )
    }

sealed class Action {
    data class FieldChanged(val field: FormField) : Action()
    data class Submit(val request: com.tunjid.me.core.model.SessionRequest) : Action()
}