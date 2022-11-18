package com.woocommerce.android.ui.login.storecreation.domainpicker

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCSearchField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainPickerViewModel.DomainPickerState
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainPickerViewModel.DomainSuggestionUi
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainPickerViewModel.LoadingState.Idle
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainPickerViewModel.LoadingState.Loading

@Composable
fun DomainPickerScreen(viewModel: DomainPickerViewModel) {
    viewModel.viewState.observeAsState(DomainPickerState()).value.let { viewState ->
        Scaffold(topBar = {
            Toolbar(
                onArrowBackPressed = viewModel::onBackPressed,
                onSkipPressed = viewModel::onSkipPressed
            )
        }) {
            DomainSearchForm(
                state = viewState,
                onDomainQueryChanged = viewModel::onDomainChanged,
                onDomainSuggestionSelected = viewModel::onDomainSuggestionSelected,
                onContinueClicked = viewModel::onContinueClicked,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun Toolbar(
    onArrowBackPressed: () -> Unit,
    onSkipPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = {},
        navigationIcon = {
            IconButton(onClick = onArrowBackPressed) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        actions = {
            TextButton(onClick = onSkipPressed) {
                Text(text = stringResource(id = R.string.store_creation_domain_picker_skip_button))
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}

@Composable
private fun DomainSearchForm(
    state: DomainPickerState,
    onDomainQueryChanged: (String) -> Unit,
    onDomainSuggestionSelected: (String) -> Unit,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_125)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
    ) {
        Text(
            text = stringResource(id = R.string.store_creation_domain_picker_title),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(id = R.string.store_creation_domain_picker_subtitle),
            style = MaterialTheme.typography.body1,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        WCSearchField(
            value = state.domain,
            onValueChange = onDomainQueryChanged,
            hint = stringResource(id = R.string.store_creation_domain_picker_hint),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(
                    BorderStroke(
                        width = dimensionResource(id = R.dimen.minor_10),
                        color = colorResource(id = R.color.gray_5)
                    ),
                    RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
                ),
            backgroundColor = TextFieldDefaults.outlinedTextFieldColors().backgroundColor(enabled = true).value
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.loadingState == Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.domainSuggestionsUi.isNotEmpty()) {
                DomainSuggestionList(
                    suggestions = state.domainSuggestionsUi,
                    onDomainSuggestionSelected = onDomainSuggestionSelected
                )
            }
        }
        WCColoredButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinueClicked,
        ) {
            Text(text = stringResource(id = R.string.continue_button))
        }
    }
}

@Composable
private fun DomainSuggestionList(
    suggestions: List<DomainSuggestionUi>,
    onDomainSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
    ) {
        Text(
            text = stringResource(id = R.string.store_creation_domain_picker_suggestions_title).uppercase(),
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        LazyColumn {
            itemsIndexed(suggestions) { index, suggestion ->
                DomainSuggestionItem(
                    domainSuggestion = suggestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDomainSuggestionSelected(suggestion.domain) }
                )
                if (index < suggestions.lastIndex)
                    Divider(
                        color = colorResource(id = R.color.divider_color),
                        thickness = dimensionResource(id = R.dimen.minor_10)
                    )
            }
        }
    }
}

@Composable
private fun DomainSuggestionItem(
    domainSuggestion: DomainSuggestionUi,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(
            top = dimensionResource(id = R.dimen.major_75),
            bottom = dimensionResource(id = R.dimen.major_75)
        )
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = MaterialTheme.typography.body2.toParagraphStyle()) {
                    withStyle(style = SpanStyle(color = colorResource(id = R.color.color_on_surface_medium_selector))) {
                        append(domainSuggestion.domain.substringBefore("."))
                    }
                    if (domainSuggestion.isSelected) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(".${domainSuggestion.domain.substringAfter(delimiter = ".")}")
                        }
                    } else {
                        withStyle(style = SpanStyle(color = colorResource(id = R.color.color_on_surface_high))) {
                            append(".${domainSuggestion.domain.substringAfter(delimiter = ".")}")
                        }
                    }
                }
            }
        )
        if (domainSuggestion.isSelected) {
            Image(
                modifier = Modifier
                    .padding(start = dimensionResource(id = R.dimen.major_100))
                    .weight(1f),
                alignment = Alignment.CenterEnd,
                painter = painterResource(id = R.drawable.ic_done_secondary),
                contentDescription = "Selected"
            )
        }
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun DomainPickerPreview() {
    WooThemeWithBackground {
        DomainSearchForm(
            state = DomainPickerState(
                loadingState = Idle,
                domain = "White Christmas Tress",
                domainSuggestionsUi = listOf(
                    DomainSuggestionUi("whitechristmastrees.mywc.mysite"),
                    DomainSuggestionUi("whitechristmastrees.business.mywc.mysite", isSelected = true),
                    DomainSuggestionUi("whitechristmastreesVeryLongWithLineBreak.business.test"),
                    DomainSuggestionUi("whitechristmastrees.business.wordpress"),
                    DomainSuggestionUi("whitechristmastrees.business.more"),
                    DomainSuggestionUi("whitechristmastrees.business.another"),
                    DomainSuggestionUi("whitechristmastrees.business.any"),
                    DomainSuggestionUi("whitechristmastrees.business.domain"),
                    DomainSuggestionUi("whitechristmastrees.business.site"),
                    DomainSuggestionUi("whitechristmastrees.business.other"),
                    DomainSuggestionUi("whitechristmastrees.business.scroll"),
                    DomainSuggestionUi("whitechristmastrees.business.other"),
                    DomainSuggestionUi("whitechristmastrees.business.other")
                )
            ),
            onDomainQueryChanged = {},
            onContinueClicked = {},
            onDomainSuggestionSelected = {}
        )
    }
}
