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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.common.domain.DomainSuggestionsViewModel
import com.woocommerce.android.ui.common.domain.DomainSuggestionsViewModel.DomainSearchState
import com.woocommerce.android.ui.common.domain.DomainSuggestionsViewModel.DomainSuggestionUi
import com.woocommerce.android.ui.common.domain.DomainSuggestionsViewModel.DomainSuggestionUi.Free
import com.woocommerce.android.ui.common.domain.DomainSuggestionsViewModel.DomainSuggestionUi.FreeWithCredit
import com.woocommerce.android.ui.common.domain.DomainSuggestionsViewModel.DomainSuggestionUi.OnSale
import com.woocommerce.android.ui.common.domain.DomainSuggestionsViewModel.DomainSuggestionUi.Paid
import com.woocommerce.android.ui.common.domain.DomainSuggestionsViewModel.LoadingState.Idle
import com.woocommerce.android.ui.common.domain.DomainSuggestionsViewModel.LoadingState.Loading
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCSearchField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun DomainPickerScreen(viewModel: DomainSuggestionsViewModel, onDomainSelected: (String) -> Unit) {
    viewModel.viewState.observeAsState(DomainSearchState()).value.let { viewState ->
        Scaffold(topBar = {
            ToolbarWithHelpButton(
                onNavigationButtonClick = viewModel::onBackPressed,
                onHelpButtonClick = viewModel::onHelpPressed,
            )
        }) { padding ->
            DomainSearchForm(
                viewModel.domainQuery,
                state = viewState,
                onDomainQueryChanged = viewModel::onDomainQueryChanged,
                onDomainSuggestionSelected = onDomainSelected,
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
    domainQuery: String,
    state: DomainSearchState,
    onDomainQueryChanged: (String) -> Unit,
    onDomainSuggestionSelected: (String) -> Unit,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val textHighlightedColor = colorResource(id = R.color.color_on_surface_high)
    val textColor = colorResource(id = R.color.color_on_surface_medium_selector)

    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .padding(dimensionResource(id = R.dimen.major_125))
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
    ) {
        if (state.freeUrl != null) {
            Text(
                text = stringResource(id = string.domains_search_domains),
                style = MaterialTheme.typography.h5,
            )
            val redirectNotice = stringResource(id = string.domains_redirect_notice)
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
                text = stringResource(id = string.store_creation_domain_picker_title),
                style = MaterialTheme.typography.h5,
            )
            Text(
                text = stringResource(id = R.string.store_creation_domain_picker_subtitle),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
        }

        WCSearchField(
            value = domainQuery,
            onValueChange = onDomainQueryChanged,
            hint = stringResource(id = R.string.store_creation_domain_picker_hint),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(
                    BorderStroke(
                        width = dimensionResource(id = R.dimen.minor_10),
                        color = colorResource(id = R.color.woo_gray_5)
                    ),
                    RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
                ),
            backgroundColor = TextFieldDefaults.outlinedTextFieldColors().backgroundColor(enabled = true).value,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = dimensionResource(id = R.dimen.minor_100))
        ) {
            when {
                state.loadingState == Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                state.domainSuggestionsUi.isEmpty() && domainQuery.isBlank() ->
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
            enabled = state.loadingState == Idle && state.selectedDomain.isNotEmpty(),
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
    keyboardController: SoftwareKeyboardController?
) {
    Column {
        Text(
            text = stringResource(id = R.string.store_creation_domain_picker_suggestions_title).uppercase(),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        suggestions.forEachIndexed { index, suggestion ->
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

@Composable
private fun DomainSuggestionItem(
    domainSuggestion: DomainSuggestionUi,
    modifier: Modifier = Modifier
) {
    val textHighlightedColor = colorResource(id = R.color.color_on_surface_high)
    val textColor = colorResource(id = R.color.color_on_surface_medium_selector)
    val yellowColor = colorResource(id = R.color.color_alert)
    val greenColor = colorResource(id = R.color.color_info)
    Row(
        modifier = modifier
            .padding(
                top = dimensionResource(id = R.dimen.major_75),
                bottom = dimensionResource(id = R.dimen.major_75)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
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

            if (domainSuggestion !is Free) {
                Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))) {
                    @Suppress("KotlinConstantConditions")
                    when (domainSuggestion) {
                        is FreeWithCredit -> {
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
                            val freeWithCredits = stringResource(id = string.domains_free_with_credits)
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = MaterialTheme.typography.body2.toParagraphStyle()) {
                                        withStyle(style = SpanStyle(color = greenColor)) {
                                            append(freeWithCredits)
                                        }
                                    }
                                }
                            )
                        }
                        is OnSale -> {
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
                        }
                        is Paid -> {
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
                        is Free -> Unit
                    }
                }
            }
        }

        if (domainSuggestion.isSelected) {
            Image(
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100)),
                alignment = Alignment.CenterEnd,
                painter = painterResource(id = R.drawable.ic_done_secondary),
                contentDescription = "Selected"
            )
        }
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
fun EmptyDomainPickerPreview() {
    WooThemeWithBackground {
        DomainSearchForm(
            domainQuery = "White Christmas Trees",
            state = DomainSearchState(
                confirmButtonTitle = R.string.domains_select_domain,
                freeUrl = "www.cnn.com",
                loadingState = Idle,
                domainSuggestionsUi = listOf()
            ),
            onDomainQueryChanged = {},
            onContinueClicked = {},
            onDomainSuggestionSelected = {}
        )
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
            domainQuery = "White Christmas Trees",
            state = DomainSearchState(
                confirmButtonTitle = R.string.domains_select_domain,
                freeUrl = "www.cnn.com",
                loadingState = Idle,
                domainSuggestionsUi = listOf(
                    Paid("whitechristmastrees.mywc.mysite", price = "$5.99 / year"),
                    Free("whitechristmastrees.business.mywc.mysite"),
                    Free("whitechristmastreesVeryLongWithLineBreak.business.test", isSelected = true),
                    OnSale(
                        "whitechristmastrees.business.wordpress",
                        price = "$5.99 / year",
                        salePrice = "$5.99",
                        isSelected = true
                    ),
                    FreeWithCredit(
                        "whitechristmastrees.business.wordpress",
                        price = "$5.99 / year",
                        isSelected = true
                    ),
                    Free("whitechristmastrees.business.another"),
                    Free("whitechristmastrees.business.any"),
                    Free("whitechristmastrees.business.domain"),
                    Free("whitechristmastrees.business.site"),
                    Free("whitechristmastrees.business.other"),
                    Free("whitechristmastrees.business.scroll"),
                    Free("whitechristmastrees.business.other"),
                    Free("whitechristmastrees.business.other")
                )
            ),
            onDomainQueryChanged = {},
            onContinueClicked = {},
            onDomainSuggestionSelected = {}
        )
    }
}
