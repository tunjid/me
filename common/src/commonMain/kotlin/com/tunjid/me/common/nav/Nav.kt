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

package com.tunjid.me.common.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A representation of a navigation node with child nodes [children]
 */
interface Node {
    val id: String

    val children: List<Node> get() = listOf()
}

/**
 * Recursively traverses each node in the tree with [this] as a parent
 */
fun Node.traverse(visit: (Node) -> Unit) {
    if (children.isEmpty()) return visit(this)
    children.forEach { it.traverse(visit) }
}

/**
 * Returns a [List] of all [Node]s in the tree with [this] as a parent
 */
fun Node.flatten(): List<Node> {
    val result = mutableListOf<Node>()
    traverse(result::add)
    return result
}

/**
 * A navigation node that represents a navigation destination on the screen. The UI representing
 * the screen is emitted via the [Render] function.
 */
interface Route : Node {
    @Composable
    fun Render()
}

object Route404 : Route {
    override val id: String
        get() = "404"

    @Composable
    override fun Render() {
        Box {
            Text(
                modifier = Modifier
                    .padding(),
                text = "404"
            )
        }
    }
}

