package com.woocommerce.android.ui.feedback.freetrial

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class FreeTrialSurveyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _surveyOptionsUi = MutableStateFlow(
        listOf(
            SurveyOptionUi(
                isSelected = false,
                textId = R.string.free_trial_survey_option1,
            ),
            SurveyOptionUi(
                isSelected = false,
                textId = R.string.free_trial_survey_option2,
            ),
            SurveyOptionUi(
                isSelected = false,
                textId = R.string.free_trial_survey_option3,
            ),
            SurveyOptionUi(
                isSelected = false,
                textId = R.string.free_trial_survey_option4,
            ),
            SurveyOptionUi(
                isSelected = false,
                textId = R.string.free_trial_survey_option5,
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

    data class SurveyOptionUi(
        val isSelected: Boolean,
        @StringRes val textId: Int,
    )
}
