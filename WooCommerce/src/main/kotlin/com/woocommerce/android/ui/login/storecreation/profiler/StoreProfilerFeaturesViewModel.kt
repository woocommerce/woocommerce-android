package com.woocommerce.android.ui.login.storecreation.profiler

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.NewStore.ProfilerData
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreProfilerFeaturesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val resourceProvider: ResourceProvider,
    storeProfilerRepository: StoreProfilerRepository
) : BaseStoreProfilerViewModel(savedStateHandle, newStore, storeProfilerRepository) {
    override val hasSearchableContent: Boolean
        get() = false
    override val isMultiChoice: Boolean = true

    init {
        analyticsTracker.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_FEATURES
            )
        )
        launch {
            profilerOptions.update {
                val options = listOf(
                    StoreProfilerOptionUi(
                        key = AnalyticsTracker.VALUE_FEATURES_PRODUCT_MANAGEMENT_AND_INVENTORY,
                        name = resourceProvider.getString(R.string.store_profiler_features_product_management),
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        key = AnalyticsTracker.VALUE_FEATURES_SALES_AND_ANALYTICS,
                        name = resourceProvider.getString(R.string.store_profiler_features_sales_analytics),
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        key = AnalyticsTracker.VALUE_FEATURES_PAYMENT_OPTIONS,
                        name = resourceProvider.getString(R.string.store_profiler_features_payment_options),
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        key = AnalyticsTracker.VALUE_FEATURES_IN_PERSON_PAYMENTS,
                        name = resourceProvider.getString(R.string.store_profiler_features_ipp),
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        key = AnalyticsTracker.VALUE_FEATURES_SCALE_AS_BUSINESS_GROWS,
                        name = resourceProvider.getString(R.string.store_profiler_features_scale_business),
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        key = AnalyticsTracker.VALUE_FEATURES_CUSTOMIZATION_OPTIONS_FOR_STORE_DESIGN,
                        name = resourceProvider.getString(R.string.store_profiler_features_store_customization),
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        key = AnalyticsTracker.VALUE_FEATURES_ACCESS_PLUGIN_AND_EXTENSIONS,
                        name = resourceProvider.getString(R.string.store_profiler_features_plugins_and_extensions),
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        key = AnalyticsTracker.VALUE_FEATURES_OTHER,
                        name = resourceProvider.getString(R.string.store_profiler_features_other),
                        isSelected = false
                    ),
                )
                options.map { option ->
                    option.copy(isSelected = option.key == newStore.data.profilerData?.featuresKey)
                }
            }
        }
    }

    override fun getProfilerStepTitle(): String =
        resourceProvider.getString(R.string.store_profiler_features_title)

    override fun getProfilerStepDescription(): String =
        resourceProvider.getString(R.string.store_profiler_features_description)

    override fun getMainButtonText(): String =
        resourceProvider.getString(R.string.store_profiler_features_main_button)

    override fun saveStepAnswer() {
        val selectedOption = profilerOptions.value.firstOrNull { it.isSelected }
        newStore.update(
            profilerData = (newStore.data.profilerData ?: ProfilerData())
                .copy(featuresKey = selectedOption?.key)
        )
        triggerEvent(NavigateToNextStep)
    }

    override fun moveForward() {
        // TODO("Navigate to store creation loading")
    }

    override fun onSkipPressed() {
        super.onSkipPressed()
        analyticsTracker.track(
            AnalyticsEvent.SITE_CREATION_PROFILER_QUESTION_SKIPPED,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_FEATURES
            )
        )
        triggerEvent(NavigateToNextStep)
    }
}
