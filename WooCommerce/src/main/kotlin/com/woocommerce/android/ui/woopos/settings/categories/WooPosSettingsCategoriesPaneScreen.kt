package com.woocommerce.android.ui.woopos.settings.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.util.ext.isWooPosPhoneLayout

@Composable
fun WooPosSettingsCategoriesPaneScreen(
    selectedCategory: WooPosSettingsCategory,
    onCategorySelected: (WooPosSettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WooPosSettingsCategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isPhoneLayout = remember(context, configuration) { context.isWooPosPhoneLayout() }

    WooPosSettingsCategoriesPaneScreenContent(
        modifier = modifier,
        scrollableCategories = state.scrollableCategories,
        fixedCategories = state.fixedCategories,
        selectedCategory = selectedCategory,
        onCategorySelected = onCategorySelected,
        isSelectable = !isPhoneLayout,
        showBottomSpacer = isPhoneLayout,
    )
}

@Composable
internal fun WooPosSettingsCategoriesPaneScreenContent(
    modifier: Modifier = Modifier,
    scrollableCategories: List<WooPosSettingsCategory>,
    fixedCategories: List<WooPosSettingsCategory>,
    selectedCategory: WooPosSettingsCategory,
    onCategorySelected: (WooPosSettingsCategory) -> Unit,
    isSelectable: Boolean = true,
    showBottomSpacer: Boolean = false,
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
                    isSelected = isSelectable && item == selectedCategory,
                    onClick = {
                        onCategorySelected(item)
                    },
                )

                Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))
            }
        }

        fixedCategories.forEach { item ->
            FixedCategoryItem(
                item = item,
                onClick = { onCategorySelected(item) }
            )
        }

        if (showBottomSpacer) {
            Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))
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
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            item.subtitleRes?.let {
                WooPosText(
                    text = stringResource(item.subtitleRes),
                    style = WooPosTypography.BodyMedium,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                    modifier = Modifier.padding(top = WooPosSpacing.XSmall.value)
                )
            }
        }
    }
}

@Composable
private fun FixedCategoryItem(
    item: WooPosSettingsCategory,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = WooPosSpacing.Medium.value,
                vertical = WooPosSpacing.Large.value
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item.icon?.let {
            Icon(
                imageVector = ImageVector.vectorResource(item.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(WooPosIconSize.Medium.value)
            )

            Spacer(modifier = Modifier.size(WooPosSpacing.Small.value))
        }

        WooPosText(
            text = stringResource(item.titleRes),
            style = WooPosTypography.BodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsCategoriesPaneScreenPreview() {
    WooPosTheme {
        WooPosSettingsCategoriesPaneScreenContent(
            scrollableCategories = listOf(
                WooPosSettingsCategory.STORE,
                WooPosSettingsCategory.HARDWARE,
                WooPosSettingsCategory.LOCAL_CATALOG,
            ),
            fixedCategories = listOf(WooPosSettingsCategory.HELP),
            selectedCategory = WooPosSettingsCategory.LOCAL_CATALOG,
            onCategorySelected = {}
        )
    }
}
