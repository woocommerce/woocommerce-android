package com.woocommerce.android.ui.woopos.settings.details.help

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
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
        HelpButton(
            icon = Icons.Default.SearchOff,
            title = stringResource(R.string.woopos_product_limitations_title),
            subtitle = stringResource(R.string.woopos_settings_help_product_limitations_subtitle),
            onClick = { viewModel.onProductLimitationsClicked() }
        )

        HelpButton(
            icon = Icons.Default.Description,
            title = stringResource(R.string.woopos_documentation_title),
            subtitle = stringResource(R.string.woopos_settings_help_documentation_subtitle),
            onClick = { viewModel.onDocumentationClicked() }
        )

        HelpButton(
            icon = Icons.AutoMirrored.Filled.Help,
            title = stringResource(R.string.woopos_get_support_title),
            subtitle = stringResource(R.string.woopos_settings_help_get_support_subtitle),
            onClick = { viewModel.onGetSupportClicked() }
        )
    }
}

@Composable
private fun HelpButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() }
            .padding(WooPosSpacing.Medium.value),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = WooPosSpacing.Medium.value)
        ) {
            WooPosText(
                text = title,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            WooPosText(
                text = subtitle,
                style = WooPosTypography.BodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = WooPosSpacing.XSmall.value)
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosHelpDetailScreenPreview() {
    WooPosTheme {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            HelpButton(
                icon = Icons.Default.SearchOff,
                title = "Where are my products?",
                subtitle = "Learn about which products are supported in POS",
                onClick = { }
            )

            HelpButton(
                icon = Icons.Default.Description,
                title = "Documentation",
                subtitle = "View guides and tutorials",
                onClick = { }
            )

            HelpButton(
                icon = Icons.AutoMirrored.Filled.Help,
                title = "Get Support",
                subtitle = "Contact our support team",
                onClick = { }
            )
        }
    }
}
