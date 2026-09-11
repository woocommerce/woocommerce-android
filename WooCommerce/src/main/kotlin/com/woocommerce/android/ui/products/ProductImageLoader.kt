package com.woocommerce.android.ui.products

import com.woocommerce.android.tools.ProductImageMap
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

interface ProductImageTarget {
    fun show(imageUrl: String?)
}

class ProductImageLoader @AssistedInject constructor(
    private val imageMap: ProductImageMap,
    @Assisted private val target: ProductImageTarget,
    @Assisted private val scope: CoroutineScope,
) {
    private var imageJob: Job? = null

    fun load(remoteProductId: Long) {
        imageJob?.cancel()
        target.show(null)
        imageJob = scope.launch {
            target.show(imageMap.get(remoteProductId))
        }
    }

    fun cancel() {
        imageJob?.cancel()
        imageJob = null
    }

    @AssistedFactory
    interface Factory {
        fun create(target: ProductImageTarget, scope: CoroutineScope): ProductImageLoader
    }
}
