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

package com.tunjid.me.data.local.models

import com.tunjid.me.common.data.UserEntity
import com.tunjid.me.core.model.User
import com.tunjid.me.core.model.UserId

internal fun UserEntity.toExternalModel() = User(
    id = UserId(id),
    firstName = first_name,
    lastName = last_name,
    fullName = full_name,
    imageUrl = image_url,
)
