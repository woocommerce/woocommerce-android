package com.woocommerce.android.ui.products.variations.attributes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
        emptyList<ProductAttributeTerm>()
    )
    val termsListState = termsFlow.asLiveData()

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
        termsFlow.update { it.toMutableList().apply { addAll(newTerms) } }
        loadingStateFlow.update { LoadingState.Idle }
    }

    enum class LoadingState {
        Idle, Loading, Appending
    }
}
