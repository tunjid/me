package com.tunjid.me.common.di

import com.tunjid.me.scaffold.globalui.GlobalUiStateHolder
import com.tunjid.me.scaffold.lifecycle.LifecycleStateHolder
import com.tunjid.me.scaffold.navigation.NavigationStateHolder
import com.tunjid.me.scaffold.scaffold.MeAppState

interface MeApp {
    val appState: MeAppState
    val navStateHolder: NavigationStateHolder
    val globalUiStateHolder: GlobalUiStateHolder
    val lifecycleStateHolder: LifecycleStateHolder
}