package com.woocommerce.android.ui.feedback.freetrial

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.FREE_TRIAL_SURVEY_SENT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.FREE_TEXT_KEY
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.SURVEY_KEY
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOptionType.COLLECTIVE_DECISION
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOptionType.COMPARING_WITH_OTHER_PLATFORMS
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOptionType.OTHER_REASONS
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOptionType.PRICE_IS_SIGNIFICANT_FACTOR
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOptionType.STILL_EXPLORING
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.utils.putIfNotEmpty
import javax.inject.Inject

@HiltViewModel
class FreeTrialSurveyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val _surveyState = savedState.getStateFlow(
        scope = this,
        initialValue = FreeTrialSurveyState(
            options = listOf(
                SurveyOptionUi(
                    isSelected = false,
                    textId = R.string.free_trial_survey_option1,
                    optionType = STILL_EXPLORING
                ),
                SurveyOptionUi(
                    isSelected = false,
                    textId = R.string.free_trial_survey_option2,
                    optionType = COMPARING_WITH_OTHER_PLATFORMS
                ),
                SurveyOptionUi(
                    isSelected = false,
                    textId = R.string.free_trial_survey_option3,
                    optionType = PRICE_IS_SIGNIFICANT_FACTOR
                ),
                SurveyOptionUi(
                    isSelected = false,
                    textId = R.string.free_trial_survey_option4,
                    optionType = COLLECTIVE_DECISION
                ),
                SurveyOptionUi(
                    isSelected = false,
                    textId = R.string.free_trial_survey_free_text,
                    optionType = OTHER_REASONS
                )
            )
        )
    )
    val surveyState = _surveyState.asLiveData()

    fun onArrowBackPressed() {
        triggerEvent(Exit)
    }

    fun onSurveyOptionTapped(surveyOptionUi: SurveyOptionUi) {
        _surveyState.update { currentState ->
            currentState.copy(
                options = updateSelectedItem(
                    currentState,
                    surveyOptionUi.optionType
                ),
                freeText = ""
            )
        }
    }

    private fun updateSelectedItem(
        surveyState: FreeTrialSurveyState,
        type: SurveyOptionType
    ) = surveyState.options.map {
        if (type == it.optionType) it.copy(isSelected = true)
        else it.copy(isSelected = false)
    }

    fun onSendTapped() {
        val selectedOption = _surveyState.value.options.first { it.isSelected }
        analyticsTrackerWrapper.track(
            stat = FREE_TRIAL_SURVEY_SENT,
            properties = mutableMapOf(
                SURVEY_KEY to selectedOption.optionType.name.lowercase()
            ).putIfNotEmpty(FREE_TEXT_KEY to _surveyState.value.freeText)
        )
        triggerEvent(Exit)
    }

    fun freeTextEntered(freeText: String) {
        _surveyState.update { currentState ->
            currentState.copy(
                options = updateSelectedItem(
                    currentState,
                    OTHER_REASONS
                ),
                freeText = freeText
            )
        }
    }

    @Parcelize
    data class FreeTrialSurveyState(
        val options: List<SurveyOptionUi>,
        val freeText: String = ""
    ) : Parcelable

    @Parcelize
    data class SurveyOptionUi(
        val optionType: SurveyOptionType,
        @StringRes val textId: Int,
        val isSelected: Boolean,
    ) : Parcelable

    enum class SurveyOptionType {
        STILL_EXPLORING,
        COMPARING_WITH_OTHER_PLATFORMS,
        PRICE_IS_SIGNIFICANT_FACTOR,
        COLLECTIVE_DECISION,
        OTHER_REASONS,
    }
}
