package com.tunjid.me.common.di

import androidx.compose.runtime.saveable.SaveableStateHolder
import com.tunjid.me.feature.ScreenStateHolderCache
import com.tunjid.me.scaffold.di.AdaptiveRouter
import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.lifecycle.LifecycleStateHolder
import com.tunjid.me.scaffold.navigation.NavigationStateHolder
import com.tunjid.me.scaffold.adaptive.AdaptiveContentState
import kotlinx.coroutines.CoroutineScope

interface MeApp {
    val adaptiveRouter: AdaptiveRouter
    val navStateHolder: NavigationStateHolder
    val globalUiStateHolder: GlobalUiStateHolder
    val lifecycleStateHolder: LifecycleStateHolder
    val screenStateHolderCache: ScreenStateHolderCache
    val adaptiveContentStateCreator: (CoroutineScope, SaveableStateHolder) -> AdaptiveContentState
}