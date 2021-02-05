package com.woocommerce.android.ui.products.variations.attributes

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.store.WCProductStore

class AddAttributeViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedState, dispatchers) {
    private val _globalAttributeList = MutableLiveData<List<Product.Attribute>>()
    val globalAttributeList: LiveData<List<Product.Attribute>> = _globalAttributeList

    val viewStateLiveData = LiveDataDelegate(savedState,
        ViewState()
    )

    private var viewState by viewStateLiveData

    fun start() {
        loadGlobalAttributes()
    }

    fun onItemClick(attribute: Product.Attribute) {
        // TODO
    }

    private fun loadGlobalAttributes() {
        // TODO
    }

    fun isEmpty() = _globalAttributeList.value?.isEmpty() ?: true

    @Parcelize
    data class ViewState(
        val isRefreshing: Boolean? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<AddAttributeViewModel>
}
