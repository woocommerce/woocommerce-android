package com.woocommerce.android.extensions

import androidx.core.widget.NestedScrollView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

@OptIn(ExperimentalCoroutinesApi::class)
fun NestedScrollView.scrollChanges() = callbackFlow {
    val listener = NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
        println("scroll $scrollY $oldScrollY ${v.isInTouchMode} ${v.isPressed}")
        trySend(scrollY)
    }

    setOnScrollChangeListener(listener)

    awaitClose { setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?) }
}
