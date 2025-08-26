package com.woocommerce.android.ui.woopos.orders

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
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.WooPosProductInfoDialog
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupDialog
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategoriesPaneScreen
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategoriesViewModel
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategory
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailPaneScreen

@Composable
fun WooPosOrdersScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    val containerViewModel: WooPosOrdersViewModel = hiltViewModel()
    val state by containerViewModel.state.collectAsState()

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

            WooPosOrdersListPaneScreen()
        }

        WooPosOrdersDetailPaneScreen()
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
fun WooPosOrdersScreenPreview() {
    WooPosTheme {
        WooPosOrdersScreen(
            onNavigationEvent = {}
        )
    }
}

@Composable
fun WooPosOrdersListPaneScreen(
) {}

@Composable
fun WooPosOrdersDetailPaneScreen(
) {}

