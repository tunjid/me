package com.tunjid.me.core.ui

import androidx.compose.runtime.Composable

/**
 * Mirror of Android platform back handler API
 */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)