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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.Modifier
import com.tunjid.me.scaffold.navigation.PredictiveBackEffects
import com.tunjid.me.scaffold.scaffold.App
import com.tunjid.me.scaffold.ui.theme.AppTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = applicationContext as App
        val meApp = app.meApp
        val appState = meApp.appState

        setContent {
            AppTheme {
                App(
                    modifier = Modifier,
                    appState = meApp.appState,
                )
                PredictiveBackEffects(
                    appState = appState
                )
            }
        }
    }
}
