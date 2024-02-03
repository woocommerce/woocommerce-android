package com.woocommerce.android.ui.blaze.creation.targets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Hidden
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Inactive
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.NoResults
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Ready
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Results
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Results.SearchItem
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Searching
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SelectionItem
import com.woocommerce.android.ui.compose.component.MultiSelectAllItemsButton
import com.woocommerce.android.ui.compose.component.MultiSelectList
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCSearchField
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews

@Composable
fun BlazeCampaignTargetSelectionScreen(viewModel: TargetSelectionViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        TargetSelectionScreen(
            state = state,
            onBackPressed = viewModel::onBackPressed,
            onSaveTapped = viewModel::onSaveTapped,
            onItemTapped = viewModel::onItemToggled,
            onSearchItemTapped = viewModel::onSearchItemTapped,
            onAllButtonTapped = viewModel::onAllButtonTapped,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onSearchActiveStateChanged = viewModel::onSearchActiveStateChanged
        )
    }
}

@Composable
private fun TargetSelectionScreen(
    state: TargetSelectionViewState,
    onBackPressed: () -> Unit,
    onSaveTapped: () -> Unit,
    onItemTapped: (SelectionItem) -> Unit,
    onSearchItemTapped: (SearchItem) -> Unit,
    onAllButtonTapped: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchActiveStateChanged: (Boolean) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    Scaffold(
        topBar = {
            Toolbar(
                title = state.title,
                onNavigationButtonClick = onBackPressed,
                navigationIcon = Filled.ArrowBack,
                actions = {
                    if (state.searchState is Hidden || state.searchState is Inactive) {
                        TextButton(onClick = onSaveTapped, enabled = state.isSaveButtonEnabled) {
                            Text(stringResource(id = string.save).uppercase())
                        }
                    }
                }
            )
        },
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .clickable(indication = null, interactionSource = interactionSource) { focusManager.clearFocus() } // Clear focus when clicked outside
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
        ) {
            if (state.searchState != Hidden) {
                val searchQuery = remember { mutableStateOf(state.searchQuery) }
                val newLineRegex = Regex("[\n\r]")

                WCSearchField(
                    value = state.searchQuery,
                    onValueChange = { newValue: String ->
                        if (newValue.contains(newLineRegex)) {
                            searchQuery.value = newValue.replace(newLineRegex, "")
                        } else {
                            onSearchQueryChanged(newValue)
                        }
                    },
                    hint = stringResource(string.blaze_campaign_preview_target_location_search_hint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = dimensionResource(id = dimen.major_100),
                            vertical = dimensionResource(id = dimen.minor_100)
                        )
                        .onFocusChanged { state ->
                            onSearchActiveStateChanged(state.isFocused)
                        }
                        .focusRequester(focusRequester), // Request focus
                    keyboardOptions = KeyboardOptions(autoCorrect = false),
                )
            }

            when (state.searchState) {
                is Searching, is Results, is NoResults, is Ready -> {
                    SearchList(
                        state = state,
                        focusManager = focusManager,
                        onSearchItemTapped = onSearchItemTapped,
                        modifier = Modifier.weight(1f)
                    )
                }
                else -> {
                    MultiSelectList(
                        items = state.items,
                        selectedItems = state.selectedItems,
                        titleFormatter = { title },
                        onItemToggled = onItemTapped,
                        isAllButtonToggled = state.isAllButtonToggled,
                        allItemsButton = MultiSelectAllItemsButton(
                            text = stringResource(id = string.blaze_campaign_preview_target_default_value),
                            onClicked = onAllButtonTapped
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchList(
    state: TargetSelectionViewState,
    focusManager: FocusManager,
    onSearchItemTapped: (SearchItem) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state.searchState) {
        is Searching -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(dimensionResource(id = dimen.progress_bar_small))

                )
            }
        }
        is Results -> {
            LazyColumn(
                modifier = modifier
            ) {
                items(state.searchState.resultItems) { item ->
                    SearchListItem(
                        item = item,
                        onItemTapped = {
                            focusManager.clearFocus()
                            onSearchItemTapped(it)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        is NoResults -> {
            val image = painterResource(id = R.drawable.search_failed_illustration)
            val message = stringResource(id = string.blaze_campaign_creation_location_search_failed_message)
            ImageWithMessage(modifier, image, message)
        }
        else -> {
            val image = painterResource(id = R.drawable.search_illustration)
            val message = stringResource(id = string.blaze_campaign_creation_location_search_message)
            ImageWithMessage(modifier, image, message)
        }
    }
}

@Composable
private fun ImageWithMessage(modifier: Modifier, image: Painter, message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Image(
            painter = image,
            contentDescription = null,
            modifier = Modifier
                .padding(dimensionResource(id = dimen.major_100))
                .fillMaxWidth()
        )
        Text(
            text = message,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = dimensionResource(id = dimen.major_100))
        )
    }
}

@Composable
private fun SearchListItem(
    item: SearchItem,
    onItemTapped: (SearchItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.major_100)),
        modifier = modifier
            .clickable { onItemTapped(item) }
            .padding(horizontal = dimensionResource(id = dimen.major_100))
            .heightIn(min = dimensionResource(id = dimen.major_300))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = item.title
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.caption,
                    color = colorResource(id = color.color_on_surface_medium)
                )
            }
        }

        if (item.type != null) {
            Text(
                text = item.type,
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = color.color_on_surface_medium)
            )
        }
    }
}

@LightDarkThemePreviews
@Composable
fun PreviewTargetSelectionScreen() {
    TargetSelectionScreen(
        state = TargetSelectionViewState(
            items = listOf(
                SelectionItem("1", "Item 1"),
                SelectionItem("2", "Item 2"),
                SelectionItem("3", "Item 3"),
                SelectionItem("4", "Item 4"),
                SelectionItem("5", "Item 5"),
                SelectionItem("6", "Item 6"),
                SelectionItem("7", "Item 7"),
                SelectionItem("8", "Item 8"),
                SelectionItem("9", "Item 9")
            ),
            selectedItems = listOf(
                SelectionItem("4", "Item 4"),
                SelectionItem("5", "Item 5"),
                SelectionItem("8", "Item 8"),
                SelectionItem("9", "Item 9")
            ),
            title = "Title",
            searchQuery = "",
            searchState = Searching
        ),
        onBackPressed = { /*TODO*/ },
        onSaveTapped = { /*TODO*/ },
        onItemTapped = {},
        onSearchItemTapped = {},
        onAllButtonTapped = {},
        onSearchQueryChanged = {},
        onSearchActiveStateChanged = {}
    )
}
