package com.tunjid.me.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.tunjid.me.common.di.MeApp
import com.tunjid.me.scaffold.adaptive.AdaptiveContentState

@Composable
fun MeApp.adaptiveContentState(): AdaptiveContentState {
    val scope = rememberCoroutineScope()
    val saveableStateHolder = rememberSaveableStateHolder()
    return remember {
        adaptiveContentStateCreator(
            scope,
            saveableStateHolder
        )
    }
}