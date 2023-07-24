package com.woocommerce.android.ui.feedback.freetrial

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOption.COLLECTIVE_DECISION
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOption.COMPARING_WITH_OTHER_PLATFORMS
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOption.EVALUATING_HOW_TO_INTEGRATE_SERVICE
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOption.OTHER_REASONS
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOption.STILL_EXPLORING
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOptionUi

@Composable
fun FreeTrialSurveyScreen(viewModel: FreeTrialSurveyViewModel) {
    viewModel.surveyOptionsUi.observeAsState().value?.let { surveyOptions ->
        Scaffold(topBar = {
            Toolbar(
                title = { Text("") },
                navigationIcon = Filled.ArrowBack,
                onNavigationButtonClick = viewModel::onArrowBackPressed,
            )
        }) { padding ->
            FreeTrialSurveyContent(
                surveyOptions = surveyOptions,
                onSurveyOptionTapped = viewModel::onSurveyOptionTapped,
                onFreeTextChanged = viewModel::freeTextEntered,
                onSendTapped = viewModel::onSendTapped,
                modifier = Modifier
                    .background(MaterialTheme.colors.surface)
                    .padding(padding)
            )
        }
    }
}

@Composable
fun FreeTrialSurveyContent(
    surveyOptions: List<SurveyOptionUi>,
    onSurveyOptionTapped: (SurveyOptionUi) -> Unit,
    onFreeTextChanged: (String) -> Unit,
    onSendTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    end = dimensionResource(id = R.dimen.major_100)
                )
        ) {
            val configuration = LocalConfiguration.current
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                HeaderContent()
            }
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    item {
                        HeaderContent()
                    }
                }
                items(surveyOptions) { surveyOption ->
                    if (surveyOption.optionType == OTHER_REASONS) {
                        WCOutlinedTextField(
                            modifier = Modifier.padding(top = dimensionResource(id = dimen.major_100)),
                            value = surveyOption.freeText,
                            onValueChange = onFreeTextChanged,
                            label = stringResource(id = surveyOption.textId),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    } else {
                        SurveyItem(
                            surveyOptionUi = surveyOption,
                            onSurveyOptionSelected = onSurveyOptionTapped,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = dimensionResource(id = R.dimen.major_100))
                        )
                    }
                }
            }
        }
        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10)
        )
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
            onClick = onSendTapped,
            enabled = surveyOptions.any { it.isSelected }
        ) {
            Text(text = stringResource(id = R.string.send_feedback))
        }
    }
}

@Composable
private fun HeaderContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(bottom = dimensionResource(id = R.dimen.major_150)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
    ) {
        Text(
            text = stringResource(id = R.string.free_trial_survey_screen_title),
            style = MaterialTheme.typography.h5,
        )
        Text(
            text = stringResource(id = R.string.local_notification_survey_after_24_hours_description),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
    }
}

@Composable
private fun SurveyItem(
    surveyOptionUi: SurveyOptionUi,
    onSurveyOptionSelected: (SurveyOptionUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .border(
                width = dimensionResource(id = if (surveyOptionUi.isSelected) R.dimen.minor_25 else R.dimen.minor_10),
                color = colorResource(
                    if (surveyOptionUi.isSelected) R.color.color_primary else R.color.divider_color
                ),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
            )
            .clip(shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)))
            .background(
                color = colorResource(
                    id = if (surveyOptionUi.isSelected)
                        if (isSystemInDarkTheme()) R.color.color_surface else R.color.woo_purple_10
                    else R.color.color_surface
                )
            )
            .clickable { onSurveyOptionSelected(surveyOptionUi) }
    ) {
        Text(
            text = stringResource(id = surveyOptionUi.textId),
            color = colorResource(
                id = if (isSystemInDarkTheme() && surveyOptionUi.isSelected) R.color.color_primary
                else R.color.color_on_surface
            ),
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_100),
                top = dimensionResource(id = R.dimen.major_75),
                bottom = dimensionResource(id = R.dimen.major_75),
                end = dimensionResource(id = R.dimen.major_100),
            )
        )
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun SurveyScreenPreview() {
    FreeTrialSurveyContent(
        surveyOptions = listOf(
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
        ),
        onSurveyOptionTapped = {},
        onFreeTextChanged = {},
        onSendTapped = {},
    )
}
