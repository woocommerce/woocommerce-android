package com.woocommerce.android.ui.products.variations.attributes

import com.woocommerce.android.ui.products.ProductDetailRepository
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AttributeTermsListHandler @Inject constructor(
    private val repository: ProductDetailRepository
) {
    companion object {
        private const val PAGE_SIZE = 20
        private const val INITIAL_PAGE = 1
    }

    private val mutex = Mutex()
    private var page = INITIAL_PAGE
    private var canLoadMore = true

    suspend fun fetchAttributeTerms(
        remoteAttributeId: Long
    ) {
        return mutex.withLock {
            page = INITIAL_PAGE
            canLoadMore = true
            repository.fetchGlobalAttributeTerms(
                remoteAttributeId = remoteAttributeId,
                page = page,
                pageSize = PAGE_SIZE
            ).also {
                if (it.size == PAGE_SIZE) {
                    canLoadMore = true
                    page++
                }
            }
        }
    }
}
