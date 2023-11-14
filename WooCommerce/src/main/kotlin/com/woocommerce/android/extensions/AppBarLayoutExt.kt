package com.woocommerce.android.extensions

import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun AppBarLayout.verticalOffsetChanges(): Flow<Int> {
    return callbackFlow {
        val listener = AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            trySendBlocking(verticalOffset)
        }
        addOnOffsetChangedListener(listener)

        awaitClose {
            removeOnOffsetChangedListener(listener)
        }
    }
}
