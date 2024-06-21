package com.woocommerce.android.ui.products.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class AddProductWithAIViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    aiRepository: AIRepository,
    private val productDetailRepository: ProductDetailRepository,
    buildProductPreviewProperties: BuildProductPreviewProperties,
    generateProductWithAI: GenerateProductWithAI,
    private val productCategoriesRepository: ProductCategoriesRepository,
    private val productTagsRepository: ProductTagsRepository,
    private val appsPrefsWrapper: AppPrefsWrapper,
    tracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState = savedStateHandle) {
    private val nameSubViewModel = ProductNameSubViewModel(
        savedStateHandle = savedStateHandle,
        tracker = tracker,
        onDone = { name ->
            aboutSubViewModel.updateProductName(name)
            previewSubViewModel.updateName(name)
            goToNextStep()
        }
    )
    private val aboutSubViewModel = AboutProductSubViewModel(
        savedStateHandle = savedStateHandle,
        onDone = { result ->
            result.let { (productFeatures, selectedAiTone) ->
                previewSubViewModel.updateKeywords(productFeatures)
                previewSubViewModel.updateTone(selectedAiTone)
            }
            goToNextStep()
        },
        appsPrefsWrapper = appsPrefsWrapper,
        tracker = tracker
    )
    private val previewSubViewModel = ProductPreviewSubViewModel(
        aiRepository = aiRepository,
        buildProductPreviewProperties = buildProductPreviewProperties,
        generateProductWithAI = generateProductWithAI,
        tracker = tracker
    ) {
        product = it
        saveButtonState.value = SaveButtonState.Shown
    }

    private lateinit var product: Product
    private val step = savedStateHandle.getStateFlow(viewModelScope, Step.ProductName)
    private val saveButtonState = MutableStateFlow(SaveButtonState.Hidden)

    private val subViewModels = listOf<AddProductWithAISubViewModel<*>>(
        nameSubViewModel,
        aboutSubViewModel,
        previewSubViewModel
    )

    val state = combine(step, saveButtonState) { step, saveButtonState ->
        State(
            progress = step.order.toFloat() / Step.values().size,
            subViewModel = subViewModels[step.ordinal],
            isFirstStep = step.ordinal == 0,
            saveButtonState = saveButtonState
        )
    }.asLiveData()

    init {
        appsPrefsWrapper.aiProductCreationIsFirstAttempt = true
        wireSubViewModels()
    }

    fun onBackButtonClick() {
        if (step.value.order == 1) {
            triggerEvent(Exit)
        } else {
            appsPrefsWrapper.aiProductCreationIsFirstAttempt = false
            goToPreviousStep()
        }
    }

    fun onSaveButtonClick() {
        AnalyticsTracker.track(AnalyticsEvent.PRODUCT_CREATION_AI_SAVE_AS_DRAFT_BUTTON_TAPPED)
        require(::product.isInitialized)
        viewModelScope.launch {
            saveButtonState.value = SaveButtonState.Loading
            val (success, productId) = saveProduct()
            if (!success) {
                triggerEvent(ShowSnackbar(R.string.error_generic))
                saveButtonState.value = SaveButtonState.Shown
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_CREATION_AI_SAVE_AS_DRAFT_FAILED)
            } else {
                triggerEvent(NavigateToProductDetailScreen(productId))
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_CREATION_AI_SAVE_AS_DRAFT_SUCCESS)
            }
        }
    }

    @Suppress("ReturnCount")
    private suspend fun saveProduct(): Pair<Boolean, Long> {
        // Create missing categories
        val missingCategories = product.categories.filter { it.remoteCategoryId == 0L }
        val createdCategories = missingCategories
            .takeIf { it.isNotEmpty() }?.let { productCategories ->
                WooLog.d(
                    tag = WooLog.T.PRODUCTS,
                    message = "Create the missing product categories ${productCategories.map { it.name }}"
                )
                productCategoriesRepository.addProductCategories(productCategories)
            }?.getOrElse {
                WooLog.e(WooLog.T.PRODUCTS, "Failed to add product categories", it)
                return Pair(false, 0L)
            }

        // Create missing tags
        val missingTags = product.tags.filter { it.remoteTagId == 0L }
        val createdTags = missingTags
            .takeIf { it.isNotEmpty() }?.let { productTags ->
                WooLog.d(
                    tag = WooLog.T.PRODUCTS,
                    message = "Create the missing product tags ${productTags.map { it.name }}"
                )
                productTagsRepository.addProductTags(productTags.map { it.name })
            }?.getOrElse {
                WooLog.e(WooLog.T.PRODUCTS, "Failed to add product tags", it)
                return Pair(false, 0L)
            }

        product = product.copy(
            categories = product.categories - missingCategories + createdCategories.orEmpty(),
            tags = product.tags - missingTags + createdTags.orEmpty()
        )

        return productDetailRepository.addProduct(product)
    }

    private fun goToNextStep() {
        require(step.value.order < Step.values().size)
        step.value = Step.getValueForOrder(step.value.order + 1)
    }

    private fun goToPreviousStep() {
        require(step.value.order > 1)
        step.value = Step.getValueForOrder(step.value.order - 1)
    }

    private fun wireSubViewModels() {
        // Notify the sub view models when the user navigates to their screen
        step.scan<Step, Step?>(null) { previousStep, newStep ->
            previousStep?.let { subViewModels[it.ordinal].onStop() }
            subViewModels[newStep.ordinal].onStart()

            newStep
        }.launchIn(viewModelScope)

        // Hide the save button when the user leaves the preview screen
        step.filter { it != Step.Preview }
            .onEach { saveButtonState.value = SaveButtonState.Hidden }
            .launchIn(viewModelScope)

        // Handle SubViewModel events
        subViewModels.forEach { subViewModel ->
            addCloseable(subViewModel)

            subViewModel.events
                .onEach {
                    triggerEvent(it)
                }.launchIn(viewModelScope)
        }
    }

    fun onProductPackageScanned(title: String, description: String, keywords: List<String>) {
        val features = keywords.joinToString()
        nameSubViewModel.onProductNameChanged(title)
        aboutSubViewModel.updateProductName(title)
        aboutSubViewModel.onProductFeaturesUpdated(features)
        previewSubViewModel.updateName(title)
        previewSubViewModel.updateProductDescription(description)
        previewSubViewModel.updateKeywords(features)
        step.value = Step.Preview
    }

    fun onProductNameGenerated(productName: String) {
        nameSubViewModel.onProductNameChanged(productName)
    }

    fun updateProductPreview(regularPrice: BigDecimal) {
        previewSubViewModel.updatePrice(regularPrice)
    }

    data class State(
        val progress: Float,
        val subViewModel: AddProductWithAISubViewModel<*>,
        val isFirstStep: Boolean,
        val saveButtonState: SaveButtonState
    )

    enum class SaveButtonState {
        Hidden, Shown, Loading
    }

    data class NavigateToProductDetailScreen(val productId: Long) : MultiLiveEvent.Event()
    data class EditPrice (val suggestedPrice: BigDecimal) : MultiLiveEvent.Event()

    @Suppress("MagicNumber")
    private enum class Step(val order: Int) {
        ProductName(1), AboutProduct(2), Preview(3);

        companion object {
            fun getValueForOrder(order: Int) = values().first { it.order == order }
        }
    }
}
