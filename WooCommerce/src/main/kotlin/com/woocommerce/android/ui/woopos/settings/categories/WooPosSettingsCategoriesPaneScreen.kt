package com.woocommerce.android.ui.woopos.settings.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosSettingsCategoriesPaneScreen(
    onCategorySelected: (WooPosSettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WooPosSettingsCategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        state.categories.forEach { item ->
            CategoryItem(
                item = item,
                onClick = {
                    onCategorySelected(item)
                }
            )
        }
    }
}

@Composable
private fun CategoryItem(
    item: WooPosSettingsCategory,
    onClick: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(
                horizontal = WooPosSpacing.Medium.value,
                vertical = WooPosSpacing.Small.value
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.padding(start = WooPosSpacing.Medium.value)
        ) {
            WooPosText(
                text = stringResource(item.titleRes),
                style = WooPosTypography.BodyLarge,
                color = textColor
            )
            WooPosText(
                text = stringResource(item.subtitleRes),
                style = WooPosTypography.BodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = WooPosSpacing.XSmall.value)
            )
        }
    }
}
