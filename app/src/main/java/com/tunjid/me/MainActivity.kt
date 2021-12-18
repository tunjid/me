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

package com.tunjid.me

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.lifecycle.lifecycleScope
import com.tunjid.me.globalui.insetMutations
import com.tunjid.me.nav.pop
import com.tunjid.me.ui.scaffold.Root
import com.tunjid.me.ui.theme.AppTheme
import com.tunjid.mutator.accept
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext as App

        onBackPressedDispatcher.addCallback(this) {
            app.appDeps.navMutator.accept { pop() }
        }

        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Root(appDeps = app.appDeps)
                }
            }
        }

        lifecycleScope.launch {
            insetMutations().collect {
                app.appDeps.globalUiMutator.accept(it)
            }
        }
    }
}
