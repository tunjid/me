package com.tunjid.me

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.tunjid.me.ui.scaffold.Root

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext as App
        setContent {
            Root(appDeps = app.appDeps)
        }
    }
}