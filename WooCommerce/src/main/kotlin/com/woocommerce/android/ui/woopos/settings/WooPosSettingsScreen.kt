package com.woocommerce.android.ui.woopos.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosSettingsScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    val viewModel: WooPosSettingsViewModel = hiltViewModel()
    BackHandler { onNavigationEvent(WooPosNavigationEvent.GoBack) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        WooPosToolbar(
            titleText = stringResource(R.string.woopos_settings_title),
            onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) }
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            WooPosText(
                text = "Settings Screen",
                style = WooPosTypography.Heading,
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
