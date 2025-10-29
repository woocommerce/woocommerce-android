package com.woocommerce.android.ui.woopos.settings.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosSettingsCategoriesPaneScreen(
    selectedCategory: WooPosSettingsCategory,
    onCategorySelected: (WooPosSettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WooPosSettingsCategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    WooPosSettingsCategoriesPaneScreenContent(
        scrollableCategories = state.scrollableCategories,
        fixedCategories = state.fixedCategories,
        selectedCategory = selectedCategory,
        onCategorySelected = onCategorySelected,
        modifier = modifier
    )
}

@Composable
private fun WooPosSettingsCategoriesPaneScreenContent(
    scrollableCategories: List<WooPosSettingsCategory>,
    fixedCategories: List<WooPosSettingsCategory>,
    selectedCategory: WooPosSettingsCategory,
    onCategorySelected: (WooPosSettingsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(WooPosSpacing.Medium.value)
        ) {
            scrollableCategories.forEach { item ->
                CategoryItem(
                    item = item,
                    isSelected = item == selectedCategory,
                    onClick = {
                        onCategorySelected(item)
                    },
                )

                Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))
            }
        }

        fixedCategories.forEach { item ->
            CategoryItem(
                item = item,
                isSelected = item == selectedCategory,
                onClick = {
                    onCategorySelected(item)
                },
            )
        }
    }
}

@Composable
private fun CategoryItem(
    item: WooPosSettingsCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    WooPosCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        isSelected = isSelected,
    ) {
        Column(
            modifier = Modifier
                .padding(WooPosSpacing.Medium.value)
        ) {
            WooPosText(
                text = stringResource(item.titleRes),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            WooPosText(
                text = stringResource(item.subtitleRes),
                style = WooPosTypography.BodyMedium,
                color = WooPosTheme.colors.onSurfaceVariantHighest,
                modifier = Modifier.padding(top = WooPosSpacing.XSmall.value)
            )
        }
    }
}

@WooPosPreview
@Composable
private fun WooPosSettingsCategoriesPaneScreenPreview() {
    WooPosTheme {
        WooPosSettingsCategoriesPaneScreenContent(
            scrollableCategories = listOf(
                WooPosSettingsCategory.STORE,
                WooPosSettingsCategory.LOCAL_CATALOG,
                WooPosSettingsCategory.HARDWARE
            ),
            fixedCategories = listOf(WooPosSettingsCategory.HELP),
            selectedCategory = WooPosSettingsCategory.STORE,
            onCategorySelected = {}
        )
    }
}
