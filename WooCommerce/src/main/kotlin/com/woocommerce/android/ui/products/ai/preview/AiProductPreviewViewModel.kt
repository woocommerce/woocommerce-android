package com.woocommerce.android.ui.products.ai.preview

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Image
import com.woocommerce.android.ui.products.ai.AIProductModel
import com.woocommerce.android.ui.products.ai.BuildProductPreviewProperties
import com.woocommerce.android.ui.products.ai.ProductPropertyCard
import com.woocommerce.android.ui.products.ai.components.ImageAction
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AiProductPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val buildProductPreviewProperties: BuildProductPreviewProperties,
    private val generateProductWithAI: GenerateProductWithAI
) : ScopedViewModel(savedStateHandle) {
    private val navArgs by savedStateHandle.navArgs<AiProductPreviewFragmentArgs>()

    private val imageState = savedStateHandle.getStateFlow(viewModelScope, ImageState(navArgs.image))
    private val selectedVariant = savedStateHandle.getStateFlow(viewModelScope, 0)
    private val userEditedName = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = String::class.java,
        key = "name"
    )
    private val userEditedDescription = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = String::class.java,
        key = "description"
    )
    private val userEditedShortDescription = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = String::class.java,
        key = "shortDescription"
    )

    private val generatedProduct = MutableStateFlow<Result<AIProductModel>?>(
        Result.success(AIProductModel.buildDefault("Name", navArgs.productFeatures))
    )

    val state: LiveData<State> = generatedProduct.transformLatest {
        if (it == null) {
            emit(State.Loading)
            return@transformLatest
        }

        when (val product = it.getOrNull()) {
            null -> emit(
                State.Error(
                    onRetryClick = { TODO() },
                    onDismissClick = { triggerEvent(MultiLiveEvent.Event.Exit) }
                )
            )

            else -> emitAll(product.prepareState())
        }
    }.asLiveData()

    init {
        generateProduct()
    }

    private fun AIProductModel.prepareState() = flow {
        val userEditedFields = combine(
            userEditedName,
            userEditedDescription,
            userEditedShortDescription
        ) { name, description, shortDescription ->
            Triple(name, description, shortDescription)
        }

        emitAll(
            combine(
                imageState,
                selectedVariant,
                selectedVariant.map {
                    buildProductPreviewProperties(
                        product = this@prepareState,
                        variant = it,
                    )
                },
                userEditedFields
            ) { imageState, selectedVariant, propertyGroups, (editedName, editedDescription, editedShortDescription) ->
                State.Success(
                    selectedVariant = selectedVariant,
                    product = this@prepareState,
                    propertyGroups = propertyGroups,
                    imageState = imageState,
                    userEditedName = editedName,
                    userEditedDescription = editedDescription,
                    userEditedShortDescription = editedShortDescription
                )
            }
        )
    }

    private fun generateProduct() = launch {
        generatedProduct.value = null
        generatedProduct.value = generateProductWithAI(navArgs.productFeatures)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onFeedbackReceived(positive: Boolean) {
        TODO()
    }

    fun onBackButtonClick() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onImageActionSelected(action: ImageAction) {
        when (action) {
            ImageAction.View -> imageState.value = imageState.value.copy(showImageFullScreen = true)
            ImageAction.Remove -> imageState.value = imageState.value.copy(image = null)
            else -> error("Unsupported action: $action")
        }
    }

    fun onFullScreenImageDismissed() {
        imageState.value = imageState.value.copy(showImageFullScreen = false)
    }

    fun onSelectNextVariant() {
        saveUserEditedFields()
        selectedVariant.update { it + 1 }
    }

    fun onSelectPreviousVariant() {
        saveUserEditedFields()
        selectedVariant.update { it - 1 }
    }

    fun onNameChanged(name: String?) {
        userEditedName.value = name
    }

    fun onDescriptionChanged(description: String?) {
        userEditedDescription.value = description
    }

    fun onShortDescriptionChanged(shortDescription: String?) {
        userEditedShortDescription.value = shortDescription
    }

    private fun saveUserEditedFields() {
        generatedProduct.update { result ->
            result?.map { product ->
                product.copy(
                    names = product.names.toMutableList().apply {
                        set(selectedVariant.value, userEditedName.value ?: get(selectedVariant.value))
                    },
                    descriptions = product.descriptions.toMutableList().apply {
                        set(selectedVariant.value, userEditedDescription.value ?: get(selectedVariant.value))
                    },
                    shortDescriptions = product.shortDescriptions.toMutableList().apply {
                        set(selectedVariant.value, userEditedShortDescription.value ?: get(selectedVariant.value))
                    }
                )
            }
        }
        userEditedName.value = null
        userEditedDescription.value = null
        userEditedShortDescription.value = null
    }

    sealed interface State {
        data object Loading : State
        data class Success(
            val selectedVariant: Int,
            private val product: AIProductModel,
            val propertyGroups: List<List<ProductPropertyCard>>,
            val imageState: ImageState,
            val shouldShowFeedbackView: Boolean = true,
            private val userEditedName: String? = null,
            private val userEditedDescription: String? = null,
            private val userEditedShortDescription: String? = null
        ) : State {
            val variantsCount = minOf(product.names.size, product.descriptions.size, product.shortDescriptions.size)
            val shouldShowVariantSelector = variantsCount > 1

            val name: TextFieldState = TextFieldState(
                value = userEditedName ?: product.names[selectedVariant],
                isValueEditedManually = userEditedName != null
            )
            val description: TextFieldState = TextFieldState(
                value = userEditedDescription ?: product.descriptions[selectedVariant],
                isValueEditedManually = userEditedDescription != null
            )
            val shortDescription: TextFieldState = TextFieldState(
                value = userEditedShortDescription ?: product.shortDescriptions[selectedVariant],
                isValueEditedManually = userEditedShortDescription != null
            )
        }

        data class Error(
            val onRetryClick: () -> Unit,
            val onDismissClick: () -> Unit
        ) : State
    }

    @Parcelize
    data class ImageState(
        val image: Image?,
        val showImageFullScreen: Boolean = false
    ) : Parcelable

    data class TextFieldState(
        val value: String,
        val isValueEditedManually: Boolean
    )
}
