package com.woocommerce.android.ui.products.components

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Component
import com.woocommerce.android.model.QueryType
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ComponentDetailsViewModel @Inject constructor(
    private val resources: ResourceProvider,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: ComponentDetailsFragmentArgs by savedState.navArgs()

    private val _componentDetails = MutableLiveData<ComponentDetails>()
    val componentDetails: LiveData<ComponentDetails> = _componentDetails

    private val _componentOptions = MutableLiveData<ComponentOptions>()
    val componentOptions: LiveData<ComponentOptions> = _componentOptions

    val componentDetailsViewStateData = LiveDataDelegate(savedState, ComponentDetailsViewState(isSkeletonShown = true))
    private var componentDetailsViewState by componentDetailsViewStateData

    init {
        val component = navArgs.component
        _componentDetails.value = ComponentDetails(component)
        launch {
            componentDetailsViewState = componentDetailsViewState.copy(isSkeletonShown = true)
            delay(1000)

            val options = List(4) { n ->
                ComponentOption(
                    id = n.toLong(),
                    title = "Option # $n"
                )
            }

            val componentType = if (component.queryType == QueryType.CATEGORY) {
                resources.getString(R.string.component_type_categories)
            } else {
                resources.getString(R.string.component_type_products)
            }

            _componentOptions.value = ComponentOptions(
                type = componentType,
                options = options,
                default = options.first()
            )
            componentDetailsViewState = componentDetailsViewState.copy(isSkeletonShown = false)
        }
    }
}

@Parcelize
data class ComponentDetailsViewState(val isSkeletonShown: Boolean? = null) : Parcelable

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

@Parcelize
data class ComponentOption(
    val id: Long,
    val title: String,
    val shouldDisplayImage: Boolean = false,
    val imageUrl: String? = null,
) : Parcelable

@Parcelize
data class ComponentOptions(
    val type: String,
    val options: List<ComponentOption>,
    val default: ComponentOption?
) : Parcelable
