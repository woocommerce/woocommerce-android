package com.woocommerce.android.util

import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import kotlin.coroutines.resume

suspend inline fun <PAYLOAD, reified EVENT: Any> Dispatcher.dispatchAndAwait(
    action: Action<PAYLOAD>
): EVENT = suspendCancellableCoroutine { continuation ->
    val listener = object : Any() {
        @Subscribe(threadMode = ThreadMode.MAIN)
        @Suppress("unused")
        fun onEvent(event: EVENT) {
            // Since generic types are suppressed at runtime, this listener will be registered using the type Object
            // But using the reified EVENT type, we can compare the class and ignore unwanted events
            if (event::class != EVENT::class) return

            unregister(this)
            if (!continuation.isActive) {
                WooLog.w(WooLog.T.UTILS, "Listener for ${EVENT::class} invoked after cancellation")
                return
            }
            continuation.resume(event)
        }
    }
    register(listener)
    dispatch(action)

    continuation.invokeOnCancellation {
        unregister(listener)
    }
}
