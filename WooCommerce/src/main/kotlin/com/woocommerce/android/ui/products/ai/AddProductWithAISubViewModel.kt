package com.woocommerce.android.ui.products.ai

import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.io.Closeable

sealed interface AddProductWithAISubViewModel<T> : Closeable {
    val events: Flow<Event>
        get() = emptyFlow()
    val onDone: (T) -> Unit
}
