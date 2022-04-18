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

package com.tunjid.me.scaffold.nav

data class RouteParams(
    val route: String,
    val pathArgs: Map<String, String> = mapOf(),
    val queryArgs: Map<String, List<String>> = mapOf(),
)

interface RouteParser<T : AppRoute> {
    val patterns: List<String>
    val pathKeys: List<String>
    fun route(params: RouteParams): T
}

fun <T : AppRoute> routeParser(
    routePattern: String,
    routeMapper: (RouteParams) -> T
) = object : RouteParser<T> {

    init {
        pathKeys = mutableListOf()
        val basicPattern = routePattern.replace(regex = pathRegex){
            pathKeys.add(it.value.replace(regex = pathArgRegex, replacement = ""))
            "(.*?)"
        }
        val queryPattern = "$basicPattern?(.*?)"
        patterns = listOf(basicPattern, queryPattern)
    }

    override val pathKeys: List<String>

    override val patterns: List<String>

    override fun route(params: RouteParams): T = routeMapper(params)
}

private val pathRegex = "\\{.*?}".toRegex()
private val pathArgRegex = "[{}]".toRegex()
