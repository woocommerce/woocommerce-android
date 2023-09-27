package com.woocommerce.android.ui.products.ai

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

@HiltViewModel
class AddProductWithAIViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    aiRepository: AIRepository,
    buildProductPreviewProperties: BuildProductPreviewProperties,
    categoriesRepository: ProductCategoriesRepository,
    tagsRepository: ProductTagsRepository,
    parameterRepository: ParameterRepository,
    appsPrefsWrapper: AppPrefsWrapper,
    analyticsTracker: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState = savedStateHandle) {
    private val nameSubViewModel = ProductNameSubViewModel(
        savedStateHandle = savedStateHandle,
        analyticsTracker = analyticsTracker,
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
        appsPrefsWrapper = appsPrefsWrapper
    )
    private val previewSubViewModel = ProductPreviewSubViewModel(
        aiRepository = aiRepository,
        buildProductPreviewProperties = buildProductPreviewProperties,
        categoriesRepository = categoriesRepository,
        tagsRepository = tagsRepository,
        parametersRepository = parameterRepository
    ) {
        // TODO keep reference to the product for the saving step
        saveButtonState.value = SaveButtonState.Shown
    }

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
        wireSubViewModels()
    }

    fun onBackButtonClick() {
        if (step.value.order == 1) {
            triggerEvent(Exit)
        } else {
            goToPreviousStep()
        }
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

    fun onProductNameGenerated(productName: String) {
        nameSubViewModel.onProductNameChanged(productName)
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

    @Suppress("MagicNumber")
    private enum class Step(val order: Int) {
        ProductName(1), AboutProduct(2), Preview(3);

        companion object {
            fun getValueForOrder(order: Int) = values().first { it.order == order }
        }
    }
}
