package com.tunjid.scaffold.scaffold.configuration

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.tunjid.scaffold.globalui.BackStatus
import com.tunjid.scaffold.globalui.COMPACT
import com.tunjid.scaffold.globalui.PreviewBackStatus
import com.tunjid.scaffold.globalui.isFromLeft
import com.tunjid.scaffold.globalui.isPreviewing
import com.tunjid.scaffold.globalui.progress
import com.tunjid.scaffold.globalui.touchX
import com.tunjid.scaffold.globalui.touchY
import com.tunjid.scaffold.scaffold.rememberUpdatedStateIf
import com.tunjid.treenav.MultiStackNav
import com.tunjid.treenav.compose.PaneScope
import com.tunjid.treenav.compose.PanedNavHostConfiguration
import com.tunjid.treenav.compose.delegated
import com.tunjid.treenav.compose.paneStrategy
import com.tunjid.treenav.compose.threepane.ThreePane
import com.tunjid.treenav.current
import com.tunjid.treenav.pop
import com.tunjid.treenav.strings.Route
import kotlin.math.roundToInt

/**
 * An [PanedNavHostConfiguration] that moves the destination in a [ThreePane.Primary] pane, to
 * to the [ThreePane.TransientPrimary] pane when a predictive back gesture is in progress.
 *
 * @param windowSizeClassState provides the current [WindowSizeClass] of the display.
 * @param backStatusState provides the state of the predictive back gesture.
 */
fun PanedNavHostConfiguration<ThreePane, MultiStackNav, Route>.predictiveBackConfiguration(
    windowSizeClassState: State<WindowSizeClass>,
    backStatusState: State<BackStatus>,
): PanedNavHostConfiguration<ThreePane, MultiStackNav, Route> {
    var lastPrimaryDestination by mutableStateOf<Route?>(null)
    return delegated(
        destinationTransform = { multiStackNav ->
            val current = multiStackNav.current as Route
            lastPrimaryDestination = current
            if (backStatusState.value.isPreviewing) multiStackNav.pop().current as Route
            else current
        },
        strategyTransform = { destination ->
            val originalStrategy = strategyTransform(destination)
            paneStrategy(
                transitions = originalStrategy.transitions,
                paneMapping = paneMapper@{ inner ->
                    val originalMapping = originalStrategy.paneMapper(inner)
                    val isPreviewingBack by remember {
                        derivedStateOf { backStatusState.value.isPreviewing }
                    }
                    if (!isPreviewingBack) return@paneMapper originalMapping

                    // Back is being previewed, therefore the original mapping is already for back.
                    // Pass the previous primary value into transient.
                    val transientDestination = checkNotNull(lastPrimaryDestination) {
                        "Attempted to show last destination without calling destination transform"
                    }
                    val paneMapping = strategyTransform(transientDestination)
                        .paneMapper(transientDestination)
                    val transient = paneMapping[ThreePane.Primary]
                    originalMapping + (ThreePane.TransientPrimary to transient)
                },
                render = paneScope@{ toRender ->
                    val windowSizeClass by windowSizeClassState
                    val backStatus by backStatusState
                    Box(
                        modifier = Modifier.adaptiveModifier(
                            windowSizeClass = windowSizeClass,
                            backStatus = backStatus,
                            adaptivePaneScope = this@paneScope
                        )
                    )
                    {
                        originalStrategy.render.invoke(this@paneScope, toRender)
                    }
                }
            )
        })
}

@Composable
private fun Modifier.adaptiveModifier(
    windowSizeClass: WindowSizeClass,
    backStatus: BackStatus,
    adaptivePaneScope: PaneScope<ThreePane, Route>,
): Modifier = this then with(adaptivePaneScope) {
    when (paneState.pane) {
        ThreePane.Primary, ThreePane.Secondary -> FillSizeModifier
            .background(color = MaterialTheme.colorScheme.surface)
            .run {
                if (windowSizeClass.minWidthDp <= WindowSizeClass.COMPACT.minWidthDp) this
                else clip(RoundedCornerShape(16.dp))
            }

        ThreePane.TransientPrimary -> FillSizeModifier
            .predictiveBackModifier(
                backStatus = backStatus
            )

        else -> FillSizeModifier
    }
}

private val FillSizeModifier = Modifier.fillMaxSize()

// Previews back content as specified by the material motion spec for Android predictive back:
// https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back#motion-specs
@Composable
private fun Modifier.predictiveBackModifier(
    backStatus: BackStatus
): Modifier {
    val configuration = LocalConfiguration.current

    if (backStatus is BackStatus.DragDismiss) {
        return this
    }
    val scale by animateFloatAsState(
        // Deviates from the spec here. The spec says 90% of the pane, I'm doing 85%
        targetValue = 1f - (backStatus.progress * 0.15F),
        label = "back preview modifier scale"
    )

    // TODO: This should not be necessary. Figure out why a frame renders without this
    //  being applied and yet the transient primary container is visible.
    val rememberedBackStatus by rememberUpdatedStateIf(backStatus) {
        it is PreviewBackStatus
    }

    return layout { measurable, constraints ->
        val placeable = measurable.measure(
            constraints.copy(
                maxWidth = (constraints.maxWidth * scale).roundToInt(),
                minWidth = (constraints.minWidth * scale).roundToInt(),
                maxHeight = (constraints.maxHeight * scale).roundToInt(),
                minHeight = (constraints.minHeight * scale).roundToInt(),
            )
        )
        val paneWidth = placeable.width
        val paneHeight = placeable.height

        val scaledWidth = paneWidth * scale
        val spaceOnEachSide = (paneWidth - scaledWidth) / 2
        val margin = (BACK_PREVIEW_PADDING * rememberedBackStatus.progress).dp.roundToPx()

        val xOffset = ((spaceOnEachSide - margin) * when {
            rememberedBackStatus.isFromLeft -> 1
            else -> -1
        }).toInt()

        val maxYShift = ((paneHeight / 20) - BACK_PREVIEW_PADDING)
        val isOrientedHorizontally = paneWidth > paneHeight
        val screenSize = when {
            isOrientedHorizontally -> configuration.screenWidthDp
            else -> configuration.screenHeightDp
        }.dp.roundToPx()
        val touchPoint = when {
            isOrientedHorizontally -> rememberedBackStatus.touchX
            else -> rememberedBackStatus.touchY
        }
        val verticalProgress = (touchPoint / screenSize) - 0.5f
        val yOffset = (verticalProgress * maxYShift).roundToInt()

        layout(placeable.width, placeable.height) {
            placeable.placeRelative(x = xOffset, y = yOffset)
        }
    }
        // Disable interactions in the preview pane
        .pointerInput(Unit) {}
}

@Composable
fun Modifier.predictiveBackBackgroundModifier(
    paneScope: PaneScope<ThreePane, *>
): Modifier {
    if (paneScope.paneState.pane != ThreePane.TransientPrimary)
        return this

    var elevation by remember { mutableStateOf(0.dp) }
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
        ) { value, _ -> elevation = value.dp }
    }
    return background(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation),
        shape = RoundedCornerShape(16.dp)
    )
        .clip(RoundedCornerShape(16.dp))

}

private const val BACK_PREVIEW_PADDING = 8
