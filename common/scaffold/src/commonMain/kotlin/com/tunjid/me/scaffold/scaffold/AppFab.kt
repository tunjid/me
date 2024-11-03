package com.tunjid.scaffold.scaffold

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tunjid.scaffold.countIf
import com.tunjid.scaffold.globalui.bottomNavSize
import com.tunjid.scaffold.globalui.keyboardSize
import com.tunjid.scaffold.globalui.slices.FabState

/**
 * Common motionally intelligent Floating Action button shared amongst nav routes in the app
 *
 */
@Composable
internal fun BoxScope.AppFab(
    state: FabState,
    onClicked: () -> Unit,
) {
    val enabled = state.enabled
    val windowSizeClass = state.windowSizeClass
    val position by animateDpAsState(
        when {
            state.fabVisible -> -with(LocalDensity.current) {
                16.dp + state.snackbarOffset + when {
                    state.keyboardSize > 0 -> state.keyboardSize.toDp()

                    else -> (windowSizeClass.bottomNavSize() countIf state.bottomNavVisible) +
                            state.navBarSize.toDp()
                }
            }

            else -> windowSizeClass.bottomNavSize()
        }
    )

    Button(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = (-16).dp, y = position)
            .wrapContentHeight(),
        enabled = enabled,
        onClick = { if (enabled) onClicked() },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary
        ),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 16.dp
        ),
        shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                FabIcon(state.icon)
                if (state.extended) Spacer(modifier = Modifier.width(8.dp))
                AnimatedContent(targetState = state.text) { text ->
                    Text(text = text)
                }
            }
        }
    )
}

@Composable
private fun FabIcon(icon: ImageVector) {
    val rotationAnimatable = remember { Animatable(initialValue = 0f) }
    val animationSpec = remember {
        spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh,
            visibilityThreshold = 0.1f
        )
    }

    Icon(
        modifier = Modifier.rotate(rotationAnimatable.value),
        imageVector = icon,
        contentDescription = null
    )

    LaunchedEffect(icon) {
        rotationAnimatable.animateTo(targetValue = 30f, animationSpec = animationSpec)
        rotationAnimatable.animateTo(targetValue = 0f, animationSpec = animationSpec)
    }
}
