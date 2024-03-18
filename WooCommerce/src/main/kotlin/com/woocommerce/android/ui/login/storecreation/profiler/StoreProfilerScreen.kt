@file:OptIn(ExperimentalComposeUiApi::class)

package com.woocommerce.android.ui.login.storecreation.profiler

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.ProgressIndicator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCSearchField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.profiler.BaseStoreProfilerViewModel.StoreProfilerOptionUi
import com.woocommerce.android.ui.login.storecreation.profiler.BaseStoreProfilerViewModel.StoreProfilerState

@Composable
fun StoreProfilerScreen(viewModel: BaseStoreProfilerViewModel) {
    viewModel.storeProfilerState.observeAsState().value?.let { state ->
        Scaffold(topBar = {
            Toolbar(
                title = { Text("") },
                navigationIcon = Filled.ArrowBack,
                onNavigationButtonClick = viewModel::onArrowBackPressed,
                actions = {
                    TextButton(onClick = viewModel::onSkipPressed) {
                        Text(text = stringResource(id = R.string.skip))
                    }
                }
            )
        }) { padding ->
            when {
                state.isLoading -> ProgressIndicator()
                else -> ProfilerContent(
                    profilerStepContent = state,
                    onContinueClicked = viewModel::onMainButtonClicked,
                    onCategorySelected = viewModel::onOptionSelected,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    modifier = Modifier
                        .background(MaterialTheme.colors.surface)
                        .padding(padding)
                )
            }
        }
    }
}

@Composable
private fun ProfilerContent(
    profilerStepContent: StoreProfilerState,
    onCategorySelected: (StoreProfilerOptionUi) -> Unit,
    onContinueClicked: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
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
                HeaderContent(profilerStepContent, onSearchQueryChanged)
            }
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    item {
                        HeaderContent(profilerStepContent, onSearchQueryChanged)
                    }
                }
                if (profilerStepContent.options.isEmpty()) {
                    item {
                        Text(
                            modifier = Modifier
                                .align(alignment = Alignment.CenterHorizontally)
                                .padding(top = dimensionResource(id = R.dimen.major_100)),
                            text = stringResource(id = R.string.store_creation_profiler_options_search_empty)
                        )
                    }
                }
                itemsIndexed(profilerStepContent.options) { index, category ->
                    if (index == 0) {
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
                    }

                    ProfilerOptionItem(
                        category = category,
                        onCategorySelected = onCategorySelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = dimensionResource(id = R.dimen.major_100))
                    )
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
            onClick = onContinueClicked,
            enabled = profilerStepContent.options.any { it.isSelected }
        ) {
            Text(text = profilerStepContent.mainButtonText)
        }
    }
}

@Composable
private fun HeaderContent(
    profilerStepContent: StoreProfilerState,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(bottom = dimensionResource(id = R.dimen.major_100)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
    ) {
        Text(
            text = stringResource(id = R.string.store_creation_store_profiler_industries_header).uppercase(),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        Text(
            text = profilerStepContent.title,
            style = MaterialTheme.typography.h5,
        )
        Text(
            text = profilerStepContent.description,
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        if (profilerStepContent.isSearchableContent) {
            SearchBar(
                profilerStepContent.searchQuery,
                onSearchQueryChanged
            )
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WCSearchField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        hint = stringResource(id = string.store_creation_store_profiler_industries_search_hint),
        modifier = modifier
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
}

@Composable
private fun ProfilerOptionItem(
    category: StoreProfilerOptionUi,
    onCategorySelected: (StoreProfilerOptionUi) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = dimensionResource(id = if (category.isSelected) R.dimen.minor_25 else R.dimen.minor_10),
                color = colorResource(
                    if (category.isSelected) R.color.color_primary else R.color.divider_color
                ),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
            )
            .clip(shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)))
            .background(
                color = colorResource(
                    id = if (category.isSelected) {
                        if (isSystemInDarkTheme()) R.color.color_surface else R.color.woo_purple_10
                    } else {
                        R.color.color_surface
                    }
                )
            )
            .clickable { onCategorySelected(category) }
    ) {
        Text(
            text = category.name,
            color = colorResource(
                id = if (isSystemInDarkTheme() && category.isSelected) {
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
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun CategoriesContentPreview() {
    WooThemeWithBackground {
        ProfilerContent(
            profilerStepContent = StoreProfilerState(
                storeName = "White Christmas Tree",
                title = "What’s your business about?",
                description = "Choose a category that defines your business the best.",
                options = listOf(
                    StoreProfilerOptionUi(
                        name = "Art & Photography",
                        key = "",
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        name = "Books & Magazines",
                        key = "",
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        name = "Electronics and Software",
                        key = "",
                        isSelected = true
                    ),
                    StoreProfilerOptionUi(
                        name = "Construction & Industrial",
                        key = "",
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        name = "Design & Marketing",
                        key = "",
                        isSelected = false
                    ),
                    StoreProfilerOptionUi(
                        name = "Fashion and Apparel",
                        key = "",
                        isSelected = false,
                    )
                ),
                isSearchableContent = true,
                isLoading = false,
                searchQuery = "",
                mainButtonText = "Continue"
            ),
            onContinueClicked = {},
            onCategorySelected = {},
            onSearchQueryChanged = {},
        )
    }
}
