package com.tunjid.me.common.di

import com.tunjid.me.scaffold.lifecycle.LifecycleStateHolder
import com.tunjid.me.scaffold.scaffold.MeAppState

interface MeApp {
    val appState: MeAppState
    val lifecycleStateHolder: LifecycleStateHolder
}