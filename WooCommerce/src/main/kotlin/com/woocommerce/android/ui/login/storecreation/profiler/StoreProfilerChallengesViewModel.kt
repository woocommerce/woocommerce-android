package com.woocommerce.android.ui.login.storecreation.profiler

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CHALLENGE_FINDING_CUSTOMERS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CHALLENGE_MANAGING_INVENTORY
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CHALLENGE_OTHER
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CHALLENGE_SETTING_UP_ONLINE_STORE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CHALLENGE_SHIPPING_AND_LOGISTICS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.NewStore.ProfilerData
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreProfilerChallengesViewModel @Inject constructor(
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
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_CHALLENGES
            )
        )
        launch {
            profilerOptions.update {
                val options = listOf(
                    StoreProfilerOptionUi(
                        key = VALUE_CHALLENGE_SETTING_UP_ONLINE_STORE,
                        name = resourceProvider.getString(R.string.store_profiler_challenge_setting_up_online_store),
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        key = VALUE_CHALLENGE_FINDING_CUSTOMERS,
                        name = resourceProvider.getString(R.string.store_profiler_challenge_finding_customers),
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        key = VALUE_CHALLENGE_MANAGING_INVENTORY,
                        name = resourceProvider.getString(R.string.store_profiler_challenge_managing_inventory),
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        key = VALUE_CHALLENGE_SHIPPING_AND_LOGISTICS,
                        name = resourceProvider.getString(R.string.store_profiler_challenge_shipping_and_logistics),
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        key = VALUE_CHALLENGE_OTHER,
                        name = resourceProvider.getString(R.string.store_profiler_challenge_other),
                        isSelected = false
                    ),
                )
                options.map { option ->
                    option.copy(isSelected = option.key == newStore.data.profilerData?.challengeKey)
                }
            }
        }
    }

    override fun getProfilerStepTitle(): String =
        resourceProvider.getString(R.string.store_profiler_challenge_title)

    override fun getProfilerStepDescription(): String =
        resourceProvider.getString(R.string.store_profiler_challenge_description)

    override fun getMainButtonText(): String =
        resourceProvider.getString(R.string.continue_button)

    override fun saveStepAnswer() {
        val selectedOption = profilerOptions.value.firstOrNull { it.isSelected }

        newStore.update(
            profilerData = (newStore.data.profilerData ?: ProfilerData())
                .copy(challengeKey = selectedOption?.key)
        )
    }

    override fun moveForward() {
        triggerEvent(NavigateToNextStep)
    }

    override fun onSkipPressed() {
        super.onSkipPressed()
        analyticsTracker.track(
            AnalyticsEvent.SITE_CREATION_PROFILER_QUESTION_SKIPPED,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_CHALLENGES
            )
        )
        // TODO("Navigate to the next step: Features interested in")
    }
}
