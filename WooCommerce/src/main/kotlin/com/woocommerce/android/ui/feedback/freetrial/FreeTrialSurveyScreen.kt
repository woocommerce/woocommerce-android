package com.woocommerce.android.ui.feedback.freetrial

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.FreeTrialSurveyState
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOptionType.COLLECTIVE_DECISION
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOptionType.COMPARING_WITH_OTHER_PLATFORMS
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOptionType.OTHER_REASONS
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOptionType.PRICE_IS_SIGNIFICANT_FACTOR
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOptionType.STILL_EXPLORING
import com.woocommerce.android.ui.feedback.freetrial.FreeTrialSurveyViewModel.SurveyOptionUi

@Composable
fun FreeTrialSurveyScreen(viewModel: FreeTrialSurveyViewModel) {
    viewModel.surveyState.observeAsState().value?.let { surveyState ->
        Scaffold(topBar = {
            Toolbar(
                title = { Text("") },
                navigationIcon = Filled.ArrowBack,
                onNavigationButtonClick = viewModel::onArrowBackPressed,
            )
        }) { padding ->
            FreeTrialSurveyContent(
                surveyState = surveyState,
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
    surveyState: FreeTrialSurveyState,
    onSurveyOptionTapped: (SurveyOptionUi) -> Unit,
    onFreeTextChanged: (String) -> Unit,
    onSendTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current

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
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                Title()
            }
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    item { Title() }
                }
                items(surveyState.options.filter { it.optionType != OTHER_REASONS }) { surveyOption ->
                    SurveyItem(
                        surveyOptionUi = surveyOption,
                        onSurveyOptionSelected = onSurveyOptionTapped,
                        focusManager = focusManager,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensionResource(id = R.dimen.major_100))
                    )
                }
            }
        }

        WCOutlinedTextField(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .onFocusEvent { focusState ->
                    if (focusState.isFocused) {
                        onFreeTextChanged(surveyState.freeText)
                    }
                },
            value = surveyState.freeText,
            onValueChange = onFreeTextChanged,
            label = stringResource(id = R.string.free_trial_survey_free_text),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )

        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10)
        )
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
            onClick = onSendTapped,
            enabled = surveyState.options.any { it.isSelected } || surveyState.freeText.isNotBlank()
        ) {
            Text(text = stringResource(id = R.string.send_feedback))
        }
    }
}

@Composable
private fun Title(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.padding(bottom = dimensionResource(id = R.dimen.major_150)),
        text = stringResource(id = R.string.free_trial_survey_screen_title),
        style = MaterialTheme.typography.h5,
    )
}

@Composable
private fun SurveyItem(
    surveyOptionUi: SurveyOptionUi,
    onSurveyOptionSelected: (SurveyOptionUi) -> Unit,
    focusManager: FocusManager,
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
                    id = if (surveyOptionUi.isSelected) {
                        if (isSystemInDarkTheme()) R.color.color_surface else R.color.woo_purple_10
                    } else {
                        R.color.color_surface
                    }
                )
            )
            .clickable {
                focusManager.clearFocus()
                onSurveyOptionSelected(surveyOptionUi)
            }
    ) {
        Text(
            text = stringResource(id = surveyOptionUi.textId),
            color = colorResource(
                id = if (isSystemInDarkTheme() && surveyOptionUi.isSelected) {
                    R.color.color_primary
                } else {
                    R.color.color_on_surface
                }
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
        surveyState = FreeTrialSurveyState(
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
                ),
            )
        ),
        onSurveyOptionTapped = {},
        onFreeTextChanged = {},
        onSendTapped = {},
    )
}
