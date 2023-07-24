package com.woocommerce.android.ui.feedback.freetrial

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOption.COLLECTIVE_DECISION
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOption.COMPARING_WITH_OTHER_PLATFORMS
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOption.EVALUATING_HOW_TO_INTEGRATE_SERVICE
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOption.OTHER_REASONS
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOption.STILL_EXPLORING
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class FreeTrialSurveyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _surveyOptionsUi = savedState.getStateFlow(
        scope = this,
        initialValue = listOf(
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
                optionType = COLLECTIVE_DECISION
            ),
            SurveyOptionUi(
                isSelected = false,
                textId = R.string.free_trial_survey_option4,
                optionType = EVALUATING_HOW_TO_INTEGRATE_SERVICE
            ),
            SurveyOptionUi(
                isSelected = false,
                textId = R.string.free_trial_survey_option5,
                optionType = OTHER_REASONS
            ),
        )
    )
    val surveyOptionsUi = _surveyOptionsUi.asLiveData()

    fun onArrowBackPressed() {
        triggerEvent(Exit)
    }

    fun onSurveyOptionTapped(surveyOptionUi: SurveyOptionUi) {
        _surveyOptionsUi.update { surveyOptions ->
            surveyOptions.map {
                if (surveyOptionUi.textId == it.textId) it.copy(isSelected = true)
                else it.copy(isSelected = false)
            }
        }
    }

    fun onSendTapped() {
        TODO()
    }

    fun freeTextEntered(freeText: String) {
        _surveyOptionsUi.update { surveyOptions ->
            surveyOptions.map {
                if (it.optionType == OTHER_REASONS) {
                    it.copy(freeText = freeText)
                } else it
            }
        }
    }

    data class SurveyOptionUi(
        val isSelected: Boolean,
        @StringRes val textId: Int,
        val optionType: SurveyOption,
        val freeText: String = "",
    )

    enum class SurveyOption {
        STILL_EXPLORING,
        COMPARING_WITH_OTHER_PLATFORMS,
        COLLECTIVE_DECISION,
        EVALUATING_HOW_TO_INTEGRATE_SERVICE,
        OTHER_REASONS,
    }
}
