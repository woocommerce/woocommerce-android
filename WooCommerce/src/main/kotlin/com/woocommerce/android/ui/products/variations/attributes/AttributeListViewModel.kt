package com.woocommerce.android.ui.products.variations.attributes

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AttributeListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val attributeListRepository: AttributeListRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState, dispatchers) {
    private var remoteProductId = 0L

    private val _attributeList = MutableLiveData<List<ProductAttribute>>()
    val attributeList: LiveData<List<ProductAttribute>> = _attributeList

    val viewStateLiveData = LiveDataDelegate(savedState,
        ViewState()
    )

    private var viewState by viewStateLiveData

    private var loadingJob: Job? = null

    fun start(remoteProductId: Long) {
        loadAttributes(remoteProductId)
    }

    fun refreshAttributes(remoteVariationId: Long) {
        viewState = viewState.copy(isRefreshing = true)
        loadAttributes(remoteVariationId)
    }

    override fun onCleared() {
        super.onCleared()
        attributeListRepository.onCleanup()
    }

    fun onItemClick(attribute: ProductAttribute) {
        // TODO
    }

    private fun loadAttributes(remoteProductId: Long) {
        if (loadingJob?.isActive == true) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading attributes")
            return
        }

        this.remoteProductId = remoteProductId

        loadingJob = launch {
            fetchAttributes(remoteProductId)
        }
    }

    fun isEmpty() = _attributeList.value?.isEmpty() ?: true

    private suspend fun fetchAttributes(remoteVariationId: Long) {
        if (networkStatus.isConnected()) {
            val fetchedAttributes = attributeListRepository.fetchStoreAttributes()
            if (fetchedAttributes.isNullOrEmpty()) {
                viewState = viewState.copy(isEmptyViewVisible = true)
            } else {
                _attributeList.value = fetchedAttributes
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
        viewState = viewState.copy(
                isSkeletonShown = false,
                isRefreshing = false
        )
    }

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<AttributeListViewModel>
}
