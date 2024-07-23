package com.woocommerce.android.ui.products.components

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Component
import com.woocommerce.android.ui.products.ComponentOptions
import com.woocommerce.android.ui.products.GetComponentOptions
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ComponentDetailsViewModel @Inject constructor(
    private val getComponentOptions: GetComponentOptions,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: ComponentDetailsFragmentArgs by savedState.navArgs()

    private val _componentDetails = MutableLiveData<ComponentDetails>()
    val componentDetails: LiveData<ComponentDetails> = _componentDetails

    private val _componentOptions = MutableLiveData<ComponentOptions>()
    val componentOptions: LiveData<ComponentOptions> = _componentOptions

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val componentDetailsViewStateData = LiveDataDelegate(savedState, ComponentDetailsViewState())
    private var componentDetailsViewState by componentDetailsViewStateData

    private val component = navArgs.component

    init {
        _componentDetails.value = ComponentDetails(component)
        launch {
            componentDetailsViewState = componentDetailsViewState.copy(isSkeletonShown = true)
            loadComponentOptions()
            componentDetailsViewState = componentDetailsViewState.copy(isSkeletonShown = false)
        }
    }

    fun pullToRefresh() {
        launch {
            componentDetailsViewState = componentDetailsViewState.copy(isRefreshing = true)
            loadComponentOptions()
            componentDetailsViewState = componentDetailsViewState.copy(isRefreshing = false)
        }
    }

    private suspend fun loadComponentOptions() {
        val componentOptions = getComponentOptions(component)
        _componentOptions.value = componentOptions
    }
}

@Parcelize
data class ComponentDetailsViewState(
    val isRefreshing: Boolean = false,
    val isSkeletonShown: Boolean = false
) : Parcelable

@Parcelize
data class ComponentDetails(
    val title: String,
    val description: String,
    val imageUrl: String?
) : Parcelable {
    constructor(component: Component) : this(
        imageUrl = component.thumbnailUrl,
        title = component.title,
        description = component.description
    )
}
