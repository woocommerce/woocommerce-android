package com.woocommerce.android.ui.compose.component

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R

@Composable
fun SearchLayoutWithParams(
    state: SearchLayoutWithParamsState,
    paramsFillWidth: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onSearchTypeSelected: (Int) -> Unit
) {
    val isFocused = remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val searchQuery = remember { mutableStateOf(state.searchQuery) }
    val newLineRegex = Regex("[\n\r]")

    Column {
        WCSearchField(
            value = state.searchQuery,
            onValueChange = { newValue: String ->
                if (newValue.contains(newLineRegex)) {
                    searchQuery.value = newValue.replace(newLineRegex, "")
                } else {
                    onSearchQueryChanged(newValue)
                }
            },
            hint = stringResource(id = state.hint),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.minor_100)
                )
                .onFocusChanged { isFocused.value = it.isFocused }
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(autoCorrect = false),
        )
        if (isFocused.value || state.areSearchTypesAlwaysVisible) {
            if (paramsFillWidth) {
                SearchParamsRowFillWidth(
                    supportedSearchTypes = state.supportedSearchTypes,
                    onSearchTypeSelected = onSearchTypeSelected,
                )
            } else {
                SearchParamsRowScrollable(
                    supportedSearchTypes = state.supportedSearchTypes,
                    onSearchTypeSelected = onSearchTypeSelected,
                )
            }
        }
    }
    LaunchedEffect(state.isSearchFocused) {
        if (state.isSearchFocused) {
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }
}

@Composable
private fun SearchParamsRowScrollable(
    supportedSearchTypes: List<SearchLayoutWithParamsState.SearchType>,
    onSearchTypeSelected: (Int) -> Unit,
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = spacedBy(dimensionResource(id = R.dimen.minor_100))
    ) {
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
        supportedSearchTypes.forEach { searchType ->
            WCSelectableChip(
                onClick = { onSearchTypeSelected(searchType.labelResId) },
                text = stringResource(id = searchType.labelResId),
                isSelected = searchType.isSelected
            )
        }
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
    }
}

@Composable
private fun SearchParamsRowFillWidth(
    supportedSearchTypes: List<SearchLayoutWithParamsState.SearchType>,
    onSearchTypeSelected: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.minor_100)),
        horizontalArrangement = Arrangement.Center,
    ) {
        supportedSearchTypes.forEach { searchType ->
            WCSelectableChip(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.minor_100))
                    .weight(1f),
                onClick = { onSearchTypeSelected(searchType.labelResId) },
                text = stringResource(id = searchType.labelResId),
                isSelected = searchType.isSelected
            )
        }
    }
}

data class SearchLayoutWithParamsState(
    @StringRes val hint: Int,
    val searchQuery: String,
    val isSearchFocused: Boolean,
    val areSearchTypesAlwaysVisible: Boolean,
    val supportedSearchTypes: List<SearchType>,
) {
    data class SearchType(
        @StringRes val labelResId: Int,
        val isSelected: Boolean = false
    )
}

@Preview
@Composable
fun SearchLayoutPreviewScrollable() {
    SearchLayoutWithParams(
        state = SearchLayoutWithParamsState(
            hint = R.string.product_selector_search_hint,
            searchQuery = "",
            isSearchFocused = false,
            areSearchTypesAlwaysVisible = true,
            supportedSearchTypes = listOf(
                SearchLayoutWithParamsState.SearchType(
                    labelResId = R.string.product_search_all,
                ),
                SearchLayoutWithParamsState.SearchType(
                    labelResId = R.string.product_search_all,
                ),
                SearchLayoutWithParamsState.SearchType(
                    labelResId = R.string.product_search_sku,
                    isSelected = true,
                ),
                SearchLayoutWithParamsState.SearchType(
                    labelResId = R.string.product_visibility_public,
                ),
            )
        ),
        paramsFillWidth = false,
        onSearchQueryChanged = {},
        onSearchTypeSelected = {},
    )
}

@Preview
@Composable
fun SearchLayoutPreviewFillMaxWidth() {
    SearchLayoutWithParams(
        state = SearchLayoutWithParamsState(
            hint = R.string.product_selector_search_hint,
            searchQuery = "",
            isSearchFocused = false,
            areSearchTypesAlwaysVisible = true,
            supportedSearchTypes = listOf(
                SearchLayoutWithParamsState.SearchType(
                    labelResId = R.string.product_search_sku,
                    isSelected = true,
                ),
                SearchLayoutWithParamsState.SearchType(
                    labelResId = R.string.product_visibility_public,
                ),
            )
        ),
        paramsFillWidth = true,
        onSearchQueryChanged = {},
        onSearchTypeSelected = {},
    )
}
