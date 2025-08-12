package com.woocommerce.android.ui.woopos.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        WooPosToolbar(
            titleText = stringResource(R.string.woopos_settings_title),
            onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) }
        )

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            WooPosSettingsCategoriesPaneScreen(
                selectedCategory = navigationState.selectedCategory,
                onCategorySelected = containerViewModel::onCategorySelected,
                modifier = Modifier.weight(0.3f)
            )

            VerticalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            WooPosSettingsDetailPaneScreen(
                state = navigationState,
                onNavigate = containerViewModel::navigateToDetail,
                onBack = containerViewModel::navigateBack,
                modifier = Modifier.weight(0.7f)
            )
        }
    }
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
