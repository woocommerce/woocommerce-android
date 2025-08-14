package com.woocommerce.android.ui.woopos.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategoriesPaneScreen
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailPaneScreen

@Composable
fun WooPosSettingsScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    val containerViewModel: WooPosSettingsViewModel = hiltViewModel()
    val navigationState by containerViewModel.navigationState.collectAsState()

    BackHandler { onNavigationEvent(WooPosNavigationEvent.GoBack) }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(0.3f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            SettingsCategoriesToolbar(
                titleText = stringResource(R.string.woopos_settings_title)
            )

            WooPosSettingsCategoriesPaneScreen(
                selectedCategory = navigationState.selectedCategory,
                onCategorySelected = containerViewModel::onCategorySelected,
                modifier = Modifier.fillMaxSize()
            )
        }

        WooPosSettingsDetailPaneScreen(
            state = navigationState,
            onNavigate = containerViewModel::navigateToDetail,
            onBack = containerViewModel::navigateBack,
            modifier = Modifier
                .weight(0.7f)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }
}

@Composable
private fun SettingsCategoriesToolbar(
    titleText: String
) {
    WooPosText(
        text = titleText,
        style = WooPosTypography.Heading,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = WooPosSpacing.Medium.value,
                vertical = WooPosSpacing.Medium.value
            )
    )
}

@WooPosPreview
@Composable
fun WooPosSettingsScreenPreview() {
    WooPosTheme {
        WooPosSettingsScreen(
            onNavigationEvent = {}
        )
    }
}
