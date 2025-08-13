package com.woocommerce.android.ui.woopos.settings.details.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsMenuItem
import com.woocommerce.android.util.ChromeCustomTabUtils
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WooPosHelpDetailScreen(
    viewModel: WooPosSettingsHelpDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.openUrlEvent.collectLatest { url ->
            ChromeCustomTabUtils.launchUrl(context, url, enableSlideAnimation = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        WooPosSettingsMenuItem(
            icon = Icons.Default.SearchOff,
            title = stringResource(R.string.woopos_product_limitations_title),
            subtitle = stringResource(R.string.woopos_settings_help_product_limitations_subtitle),
            onClick = { viewModel.onProductLimitationsClicked() }
        )

        WooPosSettingsMenuItem(
            icon = Icons.Default.Description,
            title = stringResource(R.string.woopos_documentation_title),
            subtitle = stringResource(R.string.woopos_settings_help_documentation_subtitle),
            onClick = { viewModel.onDocumentationClicked() }
        )

        WooPosSettingsMenuItem(
            icon = Icons.AutoMirrored.Filled.Help,
            title = stringResource(R.string.woopos_get_support_title),
            subtitle = stringResource(R.string.woopos_settings_help_get_support_subtitle),
            onClick = { viewModel.onGetSupportClicked() }
        )
    }
}

@WooPosPreview
@Composable
fun WooPosHelpDetailScreenPreview() {
    WooPosTheme {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            WooPosSettingsMenuItem(
                icon = Icons.Default.SearchOff,
                title = "Where are my products?",
                subtitle = "Learn about which products are supported in POS",
                onClick = { }
            )

            WooPosSettingsMenuItem(
                icon = Icons.Default.Description,
                title = "Documentation",
                subtitle = "View guides and tutorials",
                onClick = { }
            )

            WooPosSettingsMenuItem(
                icon = Icons.AutoMirrored.Filled.Help,
                title = "Get Support",
                subtitle = "Contact our support team",
                onClick = { }
            )
        }
    }
}
