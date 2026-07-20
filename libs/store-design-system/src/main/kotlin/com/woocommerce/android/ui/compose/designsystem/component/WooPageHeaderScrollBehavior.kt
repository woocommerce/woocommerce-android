package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Defaults for [WooPageHeader]. Attach [WooPageHeaderScrollBehavior.nestedScrollConnection] to the caller's
 * scrolling container when using [exitUntilCollapsedScrollBehavior].
 */
@OptIn(ExperimentalMaterial3Api::class)
object WooPageHeaderDefaults {
    @Composable
    fun exitUntilCollapsedScrollBehavior(
        canScroll: () -> Boolean = { true },
    ): WooPageHeaderScrollBehavior {
        val materialScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = rememberTopAppBarState(),
            canScroll = canScroll,
            snapAnimationSpec = null,
        )
        return remember(materialScrollBehavior) {
            WooPageHeaderScrollBehavior(materialScrollBehavior)
        }
    }
}

/**
 * Adds programmatic expansion to Material 3's exit-until-collapsed behavior for a collapsible [WooPageHeader].
 *
 * Attach [nestedScrollConnection] to the container that owns the scrolling body. Material 3 remains responsible for
 * ordinary nested scrolling, direct header dragging, and decay settling. Meaningful nested user input and both fling
 * boundaries cancel an active expansion before the event is delegated to Material 3.
 */
@Stable
@OptIn(ExperimentalMaterial3Api::class)
class WooPageHeaderScrollBehavior internal constructor(
    private val materialScrollBehavior: TopAppBarScrollBehavior,
    private val expansionAnimator: WooPageHeaderExpansionAnimator = DefaultWooPageHeaderExpansionAnimator,
) {
    private var activeExpansion: Job? = null

    val nestedScrollConnection: NestedScrollConnection = WooPageHeaderNestedScrollConnection(
        delegate = materialScrollBehavior.nestedScrollConnection,
        cancelExpansion = ::cancelActiveExpansion,
    )

    @Composable
    internal fun RenderMediumTopAppBar(
        title: @Composable () -> Unit,
        actions: @Composable RowScope.() -> Unit,
        colors: TopAppBarColors,
    ) {
        MediumTopAppBar(
            title = title,
            actions = actions,
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = colors,
            scrollBehavior = materialScrollBehavior,
        )
    }

    /**
     * Animates the header from its current height to fully expanded and resets its accumulated content offset. Call
     * from a main/UI-confined coroutine, such as one launched from `rememberCoroutineScope()` or
     * `viewLifecycleOwner.lifecycleScope`; the underlying state and expansion ownership are not thread-safe.
     *
     * Calls follow a deliberate newest-wins policy. Each call owns a child animation job, so a newer call supersedes
     * the active expansion without canceling the older caller's surrounding job. The superseded [expand] call returns
     * normally, while the newest invocation owns subsequent writes and the final state reset.
     *
     * Material 3's direct header drag does not pass through [nestedScrollConnection], and a private Material settle
     * that has already started can overlap an expansion. In those rare overlaps the state remains within Material's
     * valid bounds, but a later interaction or [expand] call may be needed to correct the visual state.
     */
    suspend fun expand() = coroutineScope {
        lateinit var expansion: Job
        expansion = launch(start = CoroutineStart.LAZY) {
            expansionAnimator.animate(materialScrollBehavior.state.heightOffset) { value ->
                writeIfActiveOwner(expansion) {
                    materialScrollBehavior.state.heightOffset = value
                }
            }
            writeIfActiveOwner(expansion) {
                materialScrollBehavior.state.heightOffset = 0f
                materialScrollBehavior.state.contentOffset = 0f
            }
        }

        cancelActiveExpansion()
        activeExpansion = expansion
        try {
            expansion.join()
        } finally {
            expansion.cancel()
            if (activeExpansion === expansion) {
                activeExpansion = null
            }
        }
    }

    private fun cancelActiveExpansion() {
        activeExpansion?.cancel()
        activeExpansion = null
    }

    private inline fun writeIfActiveOwner(
        owner: Job,
        write: () -> Unit,
    ) {
        if (owner.isActive && activeExpansion === owner) {
            write()
        }
    }
}

private class WooPageHeaderNestedScrollConnection(
    private val delegate: NestedScrollConnection,
    private val cancelExpansion: () -> Unit,
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && available.y != 0f) {
            cancelExpansion()
        }
        return delegate.onPreScroll(available, source)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source == NestedScrollSource.UserInput && (consumed.y != 0f || available.y != 0f)) {
            cancelExpansion()
        }
        return delegate.onPostScroll(consumed, available, source)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        cancelExpansion()
        return delegate.onPreFling(available)
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        cancelExpansion()
        return delegate.onPostFling(consumed, available)
    }
}

internal fun interface WooPageHeaderExpansionAnimator {
    suspend fun animate(initialValue: Float, onFrame: (Float) -> Unit)
}

private object DefaultWooPageHeaderExpansionAnimator : WooPageHeaderExpansionAnimator {
    override suspend fun animate(initialValue: Float, onFrame: (Float) -> Unit) {
        AnimationState(initialValue = initialValue).animateTo(
            targetValue = 0f,
            animationSpec = spring(),
        ) {
            onFrame(value)
        }
    }
}
