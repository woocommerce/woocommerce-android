package com.woocommerce.android.ui.login.storecreation.profiler

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.profiler.StoreProfilerViewModel.StoreCategoryUi

@Composable
fun StoreProfilerCategoryScreen(viewModel: StoreProfilerViewModel) {
    viewModel.storeProfilerState.observeAsState().value?.let { state ->
        Scaffold(topBar = {
            Toolbar(
                onArrowBackPressed = viewModel::onArrowBackPressed,
                onSkipPressed = viewModel::onSkipPressed
            )
        }) {
            CategoriesContent(
                storeName = state.storeName,
                storeCategories = state.categories,
                onContinueClicked = viewModel::onContinueClicked,
                onCategorySelected = viewModel::onCategorySelected,
                modifier = Modifier.background(MaterialTheme.colors.surface)
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
                Text(text = stringResource(id = R.string.skip))
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}

@Composable
private fun CategoriesContent(
    storeName: String,
    storeCategories: List<StoreCategoryUi>,
    onCategorySelected: (StoreCategoryUi) -> Unit,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(dimensionResource(id = R.dimen.major_100)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        ) {
            Text(
                text = storeName.uppercase(),
                style = MaterialTheme.typography.caption,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
            Text(
                text = stringResource(id = R.string.store_creation_store_categories_title),
                style = MaterialTheme.typography.h5,
            )
            Text(
                text = stringResource(id = R.string.store_creation_store_categories_subtitle),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
            CategoryList(
                categories = storeCategories,
                onCategorySelected = onCategorySelected,
                modifier = Modifier
                    .fillMaxWidth()
            )
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
        ) {
            Text(text = stringResource(id = R.string.continue_button))
        }
    }
}

@Composable
private fun CategoryList(
    categories: List<StoreCategoryUi>,
    onCategorySelected: (StoreCategoryUi) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(categories) { _, category ->
            CategoryItem(
                category = category,
                onCategorySelected = onCategorySelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(id = R.dimen.major_100))
            )
        }
    }
}

@Composable
private fun CategoryItem(
    category: StoreCategoryUi,
    onCategorySelected: (StoreCategoryUi) -> Unit,
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
                    id = if (category.isSelected)
                        if (isSystemInDarkTheme()) R.color.color_on_primary_surface else R.color.woo_purple_10
                    else R.color.color_surface
                )
            )
            .clickable { onCategorySelected(category) }
    ) {
        Text(
            text = category.name,
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
        CategoriesContent(
            storeName = "White Christmas Tress",
            storeCategories = CATEGORIES,
            onContinueClicked = {},
            onCategorySelected = {}
        )
    }
}

// TODO remove when this are available from API
val CATEGORIES = listOf(
    StoreCategoryUi(
        name = "Art & Photography",
        isSelected = false
    ),
    StoreCategoryUi(
        name = "Books & Magazines",
        isSelected = true
    ),
    StoreCategoryUi(
        name = "Electronics and Software",
        isSelected = false
    ),
    StoreCategoryUi(
        name = "Construction & Industrial",
        isSelected = false
    ),
    StoreCategoryUi(
        name = "Design & Marketing",
        isSelected = false
    ),
    StoreCategoryUi(
        name = "Fashion and Apparel",
        isSelected = false
    ),
    StoreCategoryUi(
        name = "Food and Drink",
        isSelected = false
    ),
    StoreCategoryUi(
        name = "Books & Magazines 2",
        isSelected = false
    ),
    StoreCategoryUi(
        name = "Electronics and Software 2",
        isSelected = false
    ),
    StoreCategoryUi(
        name = "Design & Marketing 2",
        isSelected = false
    ),
    StoreCategoryUi(
        name = "Fashion and Apparel 2",
        isSelected = false
    ),
)
