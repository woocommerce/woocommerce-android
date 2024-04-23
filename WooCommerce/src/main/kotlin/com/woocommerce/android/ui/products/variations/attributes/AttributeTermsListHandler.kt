package com.woocommerce.android.ui.products.variations.attributes

import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class AttributeTermsListHandler @Inject constructor(
    private val repository: ProductDetailRepository
) {
    companion object {
        private const val PAGE_SIZE = 10
        private const val INITIAL_PAGE = 1
    }

    private val mutex = Mutex()
    private var page = INITIAL_PAGE
    private var canLoadMore = true

    suspend fun fetchAttributeTerms(
        remoteAttributeId: Long
    ): List<ProductAttributeTerm> {
        return mutex.withLock {
            page = INITIAL_PAGE
            canLoadMore = true
            loadAttributeTerms(remoteAttributeId)
        }
    }

    suspend fun loadMore(
        remoteAttributeId: Long
    ): List<ProductAttributeTerm> {
        return mutex.withLock {
            if (canLoadMore) {
                loadAttributeTerms(remoteAttributeId)
            } else {
                emptyList()
            }
        }
    }

    private suspend fun loadAttributeTerms(
        remoteAttributeId: Long
    ): List<ProductAttributeTerm> = repository.fetchGlobalAttributeTerms(
        remoteAttributeId,
        page,
        PAGE_SIZE
    ).also {
        if (it.size == PAGE_SIZE) {
            canLoadMore = true
            page++
        } else {
            canLoadMore = false
        }
    }
}
