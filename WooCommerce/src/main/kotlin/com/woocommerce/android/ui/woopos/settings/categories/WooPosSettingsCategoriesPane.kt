package com.woocommerce.android.ui.woopos.settings.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.settings.SettingsCategory

@Composable
fun WooPosSettingsCategoriesPane(
    selectedCategory: SettingsCategory,
    onCategorySelected: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WooPosSettingsCategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(WooPosSpacing.Medium.value)
    ) {
        state.categories.forEach { item ->
            CategoryItem(
                item = item,
                isSelected = item.category == selectedCategory,
                onClick = { 
                    if (item.isEnabled) {
                        onCategorySelected(item.category)
                    }
                }
            )
        }
    }
}

@Composable
private fun CategoryItem(
    item: SettingsCategoryItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val textColor = when {
        !item.isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    WooPosText(
        text = stringResource(item.titleRes),
        style = WooPosTypography.BodyLarge,
        color = textColor,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(enabled = item.isEnabled) { onClick() }
            .padding(WooPosSpacing.Medium.value)
    )
}