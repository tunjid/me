package com.tunjid.me

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.tunjid.me.nav.pop
import com.tunjid.me.ui.scaffold.Root
import com.tunjid.mutator.accept

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext as App

        onBackPressedDispatcher.addCallback(this) {
            app.appDeps.navMutator.accept { pop() }
        }

        setContent {
            Root(appDeps = app.appDeps)
        }
    }
}