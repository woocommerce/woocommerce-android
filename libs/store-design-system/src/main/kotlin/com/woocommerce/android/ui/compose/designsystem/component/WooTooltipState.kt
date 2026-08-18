@file:OptIn(ExperimentalMaterial3Api::class)

package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.MutatePriority
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/** State for [WooTooltipBox]. Each tooltip host should own a distinct instance. */
@Stable
class WooTooltipState internal constructor(
    private val materialState: TooltipState,
) {
    private var anchorIsVisible = false
    private var attemptCounter = 0L
    private var currentRequest: PresentationRequest? = null

    /** Whether the popup is currently presented. A retained offscreen request is not visible. */
    val isVisible: Boolean
        get() = shouldPresentPopup

    /** Shows this tooltip persistently. A later Woo tooltip request supersedes this one. */
    suspend fun show() = show(MutatePriority.Default)

    /** Dismisses this tooltip and clears any retained offscreen request. */
    fun dismiss() {
        val request = currentRequest
        currentRequest = null
        attemptCounter++
        materialState.dismiss()
        if (request != null) {
            WooTooltipRequestCoordinator.release(this, request)
            request.completion.complete(Unit)
        }
    }

    internal val hostState: TooltipState = object : TooltipState {
        override val transition: MutableTransitionState<Boolean>
            get() = materialState.transition

        override val isVisible: Boolean
            get() = shouldPresentPopup

        override val isPersistent: Boolean
            get() = materialState.isPersistent

        override suspend fun show(mutatePriority: MutatePriority) {
            this@WooTooltipState.show(mutatePriority)
        }

        override fun dismiss() = this@WooTooltipState.dismiss()

        override fun onDispose() {
            anchorIsVisible = false
            this@WooTooltipState.dismiss()
            materialState.onDispose()
        }
    }

    internal fun onAnchorVisibilityChanged(isVisible: Boolean) {
        if (anchorIsVisible == isVisible) return
        anchorIsVisible = isVisible
        if (!isVisible) pauseOrDismissOffscreenRequest()
    }

    internal suspend fun resumeOffscreenRequest() {
        val request = currentRequest ?: return
        if (!anchorIsVisible || !request.isPaused) return
        request.isPaused = false
        present(request)
    }

    private suspend fun show(mutatePriority: MutatePriority) {
        val request = PresentationRequest(
            priority = mutatePriority,
        )
        replaceRequest(request)
        WooTooltipRequestCoordinator.claim(this, request) { finish(request) }
        try {
            if (anchorIsVisible) {
                present(request)
            } else {
                request.isPaused = mutatePriority == MutatePriority.Default
                if (!request.isPaused) finish(request)
            }
            request.completion.await()
        } finally {
            if (!request.completion.isCompleted) finish(request)
        }
    }

    private suspend fun present(request: PresentationRequest) {
        if (currentRequest !== request || !anchorIsVisible) return
        val attempt = ++attemptCounter
        try {
            supervisorScope {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    materialState.show(request.priority)
                }.join()
            }
        } finally {
            if (currentRequest === request && attemptCounter == attempt && !request.isPaused) {
                finish(request)
            }
        }
    }

    private fun replaceRequest(request: PresentationRequest) {
        currentRequest?.let(::finish)
        currentRequest = request
    }

    private fun pauseOrDismissOffscreenRequest() {
        val request = currentRequest ?: return
        if (request.priority == MutatePriority.Default) {
            request.isPaused = true
            attemptCounter++
            materialState.dismiss()
        } else {
            finish(request)
        }
    }

    private fun finish(request: PresentationRequest) {
        if (currentRequest !== request) return
        currentRequest = null
        attemptCounter++
        materialState.dismiss()
        WooTooltipRequestCoordinator.release(this, request)
        request.completion.complete(Unit)
    }

    private val shouldPresentPopup: Boolean
        get() = materialState.isVisible && anchorIsVisible && currentRequest?.isPaused == false
}

/**
 * Remembers persistent tooltip state. Presentation always enters Material's global coordinator through
 * [WooTooltipState.show].
 */
@Composable
fun rememberWooTooltipState(): WooTooltipState {
    val materialState = rememberTooltipState(isPersistent = true)
    return remember(materialState) { WooTooltipState(materialState) }
}

private class PresentationRequest(
    val priority: MutatePriority,
    val completion: CompletableDeferred<Unit> = CompletableDeferred(),
) {
    var isPaused: Boolean = false
}

private object WooTooltipRequestCoordinator {
    private val lock = Any()
    private var latestRequest: CoordinatedRequest? = null

    fun claim(owner: WooTooltipState, token: Any, onSuperseded: () -> Unit) {
        val previous = synchronized(lock) {
            latestRequest.also {
                latestRequest = CoordinatedRequest(owner, token, onSuperseded)
            }
        }
        if (previous?.owner !== owner || previous.token !== token) {
            previous?.onSuperseded?.invoke()
        }
    }

    fun release(owner: WooTooltipState, token: Any) {
        synchronized(lock) {
            val latest = latestRequest
            if (latest?.owner === owner && latest.token === token) latestRequest = null
        }
    }

    private data class CoordinatedRequest(
        val owner: WooTooltipState,
        val token: Any,
        val onSuperseded: () -> Unit,
    )
}
