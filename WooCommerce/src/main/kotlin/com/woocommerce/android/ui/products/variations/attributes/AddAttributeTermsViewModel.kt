package com.woocommerce.android.ui.products.variations.attributes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddAttributeTermsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val termsListHandler: AttributeTermsListHandler
) : ScopedViewModel(savedState) {
    private val termsFlow = savedState.getStateFlow(
        this,
        emptyMap<Int, ProductAttributeTerm>()
    )
    val termsListState = termsFlow
        .map { it.map { (_, term) -> term } }
        .asLiveData()

    private val loadingStateFlow = MutableStateFlow(LoadingState.Idle)
    val loadingState = loadingStateFlow.asLiveData()

    fun onLoadMore(remoteAttributeId: Long) = launch {
        loadingStateFlow.update { LoadingState.Appending }
        publishChangesFrom(termsListHandler.loadMore(remoteAttributeId))
    }

    fun onFetchAttributeTerms(remoteAttributeId: Long) = launch {
        loadingStateFlow.update { LoadingState.Loading }
        publishChangesFrom(termsListHandler.fetchAttributeTerms(remoteAttributeId))
    }

    private fun publishChangesFrom(newTerms: List<ProductAttributeTerm>) {
        termsFlow.update { termsMap ->
            termsMap.toMutableMap().apply {
                putAll(newTerms.associateBy { it.remoteId })
            }
        }
        loadingStateFlow.update { LoadingState.Idle }
    }

    fun resetGlobalAttributeTerms() {
        termsFlow.update { emptyMap() }
    }

    enum class LoadingState {
        Idle, Loading, Appending
    }
}
