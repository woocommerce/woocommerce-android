@file:OptIn(ExperimentalComposeUiApi::class)

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
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
                onNavigationButtonClick = viewModel::onBackPressed,
                onActionButtonClick = viewModel::onHelpPressed,
            )
        }) { padding ->
            DomainSearchForm(
                state = viewState,
                onDomainQueryChanged = viewModel::onDomainChanged,
                onDomainSuggestionSelected = viewModel::onDomainSuggestionSelected,
                onContinueClicked = viewModel::onContinueClicked,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}

@Composable
private fun DomainSearchForm(
    state: DomainPickerState,
    onDomainQueryChanged: (String) -> Unit,
    onDomainSuggestionSelected: (String) -> Unit,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val textHighlightedColor = colorResource(id = R.color.color_on_surface_high)
    val textColor = colorResource(id = R.color.color_on_surface_medium_selector)

    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_125)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
    ) {
        Text(
            text = stringResource(id = R.string.store_creation_domain_picker_title),
            style = MaterialTheme.typography.h5,
        )
        if (state.freeUrl != null) {
            val redirectNotice = stringResource(id = R.string.domains_redirect_notice)
            Text(
                text = buildAnnotatedString {
                    withStyle(style = MaterialTheme.typography.body2.toParagraphStyle()) {
                        withStyle(style = SpanStyle(color = textColor)) {
                            append(redirectNotice)
                        }
                        append(" ")
                        withStyle(style = SpanStyle(color = textHighlightedColor, fontWeight = FontWeight.Bold)) {
                            append(state.freeUrl)
                        }
                    }
                },
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
        } else {
            Text(
                text = stringResource(id = R.string.store_creation_domain_picker_subtitle),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
        }

        WCSearchField(
            value = state.domainQuery,
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
            backgroundColor = TextFieldDefaults.outlinedTextFieldColors().backgroundColor(enabled = true).value,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = dimensionResource(id = R.dimen.minor_100))
        ) {
            when {
                state.loadingState == Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.domainSuggestionsUi.isEmpty() && state.domainQuery.isBlank() ->
                    ShowEmptyImage(modifier = Modifier.align(Alignment.Center))
                state.domainSuggestionsUi.isEmpty() ->
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(id = R.string.store_creation_domain_picker_empty_suggestions)
                    )
                else -> DomainSuggestionList(
                    suggestions = state.domainSuggestionsUi,
                    onDomainSuggestionSelected = onDomainSuggestionSelected,
                    keyboardController = keyboardController
                )
            }
        }
        WCColoredButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinueClicked,
            enabled = state.loadingState == Idle
        ) {
            Text(text = stringResource(id = state.confirmButtonTitle))
        }
    }
}

@Composable
fun ShowEmptyImage(modifier: Modifier) {
    Image(
        modifier = modifier,
        painter = painterResource(R.drawable.domain_example), contentDescription = null
    )
}

@Composable
private fun DomainSuggestionList(
    suggestions: List<DomainSuggestionUi>,
    onDomainSuggestionSelected: (String) -> Unit,
    keyboardController: SoftwareKeyboardController?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
    ) {
        Text(
            text = stringResource(id = R.string.store_creation_domain_picker_suggestions_title).uppercase(),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        LazyColumn {
            itemsIndexed(suggestions) { index, suggestion ->
                DomainSuggestionItem(
                    domainSuggestion = suggestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            keyboardController?.hide()
                            onDomainSuggestionSelected(suggestion.domain)
                        }
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
    val textHighlightedColor = colorResource(id = R.color.color_on_surface_high)
    val textColor = colorResource(id = R.color.color_on_surface_medium_selector)
    val yellowColor = colorResource(id = R.color.color_alert)
    Row(
        modifier = modifier
            .padding(
                top = dimensionResource(id = R.dimen.major_75),
                bottom = dimensionResource(id = R.dimen.major_75)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = MaterialTheme.typography.body2.toParagraphStyle()) {
                        withStyle(style = SpanStyle(color = textColor)) {
                            append(domainSuggestion.domain.substringBefore("."))
                        }
                        if (domainSuggestion.isSelected) {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(".${domainSuggestion.domain.substringAfter(delimiter = ".")}")
                            }
                        } else {
                            withStyle(style = SpanStyle(color = textHighlightedColor)) {
                                append(".${domainSuggestion.domain.substringAfter(delimiter = ".")}")
                            }
                        }
                    }
                }
            )

            if (domainSuggestion.price != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))) {
                    if (domainSuggestion.salePrice != null) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = MaterialTheme.typography.body2.toParagraphStyle()) {
                                    withStyle(style = SpanStyle(color = yellowColor)) {
                                        append(domainSuggestion.salePrice)
                                    }
                                }
                            }
                        )
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = MaterialTheme.typography.body2.toParagraphStyle()) {
                                    withStyle(
                                        style = SpanStyle(
                                            color = textColor,
                                            textDecoration = TextDecoration.LineThrough
                                        )
                                    ) {
                                        append(domainSuggestion.price)
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = MaterialTheme.typography.body2.toParagraphStyle()) {
                                    withStyle(style = SpanStyle(color = textColor)) {
                                        append(domainSuggestion.price)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

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
                confirmButtonTitle = R.string.domains_select_domain,
                freeUrl = "www.cnn.com",
                loadingState = Idle,
                domainQuery = "White Christmas Tress",
                domainSuggestionsUi = listOf(
                    DomainSuggestionUi("whitechristmastrees.mywc.mysite", price = "$5.99 / year"),
                    DomainSuggestionUi("whitechristmastrees.business.mywc.mysite"),
                    DomainSuggestionUi("whitechristmastreesVeryLongWithLineBreak.business.test", isSelected = true),
                    DomainSuggestionUi(
                        "whitechristmastrees.business.wordpress",
                        price = "$5.99 / year",
                        salePrice = "$5.99",
                        isSelected = true
                    ),
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
