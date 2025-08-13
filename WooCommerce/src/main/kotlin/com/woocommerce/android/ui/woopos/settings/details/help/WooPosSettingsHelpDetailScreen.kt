package com.woocommerce.android.ui.woopos.settings.details.help

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
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
            .padding(WooPosSpacing.Medium.value),
        verticalArrangement = Arrangement.Top
    ) {
        HelpButton(
            icon = Icons.Default.SearchOff,
            title = stringResource(R.string.woopos_product_limitations_title),
            subtitle = stringResource(R.string.woopos_settings_help_product_limitations_subtitle),
            onClick = { viewModel.onProductLimitationsClicked() }
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        HelpButton(
            icon = Icons.Default.Description,
            title = stringResource(R.string.woopos_documentation_title),
            subtitle = stringResource(R.string.woopos_settings_help_documentation_subtitle),
            onClick = { viewModel.onDocumentationClicked() }
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

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
            .clip(RoundedCornerShape(WooPosCornerRadius.Large.value))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onClick() }
            .padding(WooPosSpacing.Large.value),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )

        Column(
            modifier = Modifier
                .padding(start = WooPosSpacing.Large.value)
                .weight(1f)
        ) {
            WooPosText(
                text = title,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            WooPosText(
                text = subtitle,
                style = WooPosTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            modifier = Modifier
                .fillMaxSize()
                .padding(WooPosSpacing.Medium.value),
            verticalArrangement = Arrangement.Top
        ) {
            HelpButton(
                icon = Icons.Default.SearchOff,
                title = "Where are my products?",
                subtitle = "Learn about which products are supported in POS",
                onClick = { }
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            HelpButton(
                icon = Icons.Default.Description,
                title = "Documentation",
                subtitle = "View guides and tutorials",
                onClick = { }
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            HelpButton(
                icon = Icons.AutoMirrored.Filled.Help,
                title = "Get Support",
                subtitle = "Contact our support team",
                onClick = { }
            )
        }
    }
}
