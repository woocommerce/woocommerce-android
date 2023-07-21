package com.woocommerce.android.ui.feedback.freetrial

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class FreeTrialSurveyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle) {
    val surveyOptions: LiveData<List<SurveyOption>> =
        MutableStateFlow(
            listOf(
                SurveyOption(
                    isSelected = false,
                    text = R.string.free_trial_survey_option1,
                ),
                SurveyOption(
                    isSelected = false,
                    text = R.string.free_trial_survey_option2,
                ),
                SurveyOption(
                    isSelected = false,
                    text = R.string.free_trial_survey_option3,
                ),
                SurveyOption(
                    isSelected = false,
                    text = R.string.free_trial_survey_option4,
                ),
                SurveyOption(
                    isSelected = false,
                    text = R.string.free_trial_survey_option5,
                ),
            )
        ).asLiveData()

    data class SurveyOption(
        val isSelected: Boolean,
        @StringRes val text: Int,
        val extraText: String? = null,
    )
}
