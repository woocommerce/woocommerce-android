package com.woocommerce.android.ui.woopos.settings.details.store

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Suppress("UnusedParameter")
@Composable
fun WooPosSettingsStoreScreen(
    modifier: Modifier = Modifier,
    viewModel: WooPosSettingsStoreViewModel = hiltViewModel()
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Medium.value),
        contentAlignment = Alignment.Center
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_settings_store_category),
            style = WooPosTypography.Heading
        )
    }
}
