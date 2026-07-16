package com.woocommerce.android.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooPageHeaderDefaults
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import com.woocommerce.android.ui.compose.designsystem.icons.Xmark

@Composable
internal fun DashboardScreen(
    storeName: String,
    showShareStoreButton: Boolean,
    onShareStoreClicked: () -> Unit,
    showJetpackBenefitsBanner: Boolean,
    onJetpackBenefitsBannerClicked: () -> Unit,
    onJetpackBenefitsBannerDismissed: () -> Unit,
    dashboardContent: @Composable (Modifier, DashboardHeaderScrollBridge, @Composable () -> Unit) -> Unit,
    jitmContent: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = WooPageHeaderDefaults.exitUntilCollapsedScrollBehavior()
    val headerScrollBridge = remember { DashboardHeaderScrollBridge() }
    DisposableEffect(scrollBehavior, headerScrollBridge) {
        headerScrollBridge.attach(scrollBehavior)
        onDispose { headerScrollBridge.detach(scrollBehavior) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(DASHBOARD_CONTAINER_TEST_TAG),
    ) {
        DashboardHeader(
            storeName = storeName,
            showShareStoreButton = showShareStoreButton,
            onShareStoreClicked = onShareStoreClicked,
            scrollBehavior = scrollBehavior,
        )
        Box(modifier = Modifier.weight(1f)) {
            dashboardContent(Modifier.fillMaxSize(), headerScrollBridge) {
                Box(modifier = Modifier.padding(vertical = WooTheme.padding.padding2)) {
                    jitmContent(Modifier.fillMaxWidth())
                }
            }
        }
        AnimatedVisibility(visible = showJetpackBenefitsBanner) {
            JetpackBenefitsBanner(
                onClick = onJetpackBenefitsBannerClicked,
                onDismiss = onJetpackBenefitsBannerDismissed,
            )
        }
    }
}

@Composable
fun JetpackBenefitsBanner(
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .background(colorResource(R.color.jetpack_black))
            .clickable(onClick = onClick)
            .padding(start = WooTheme.padding.padding5)
            .padding(vertical = WooTheme.padding.padding4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_jetpack_logo),
            contentDescription = null,
            modifier = Modifier.size(WooTheme.iconSize.size24),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = WooTheme.padding.padding5),
        ) {
            Text(
                text = stringResource(R.string.jetpack_benefits_bottom_banner_title),
                color = colorResource(R.color.woo_white),
                style = WooTheme.text.labelLarge.emphasized,
            )
            Text(
                text = stringResource(R.string.jetpack_benefits_bottom_banner_subtitle),
                color = colorResource(R.color.woo_white_alpha_060),
                style = WooTheme.text.labelMedium.emphasized,
                modifier = Modifier.padding(top = WooTheme.padding.padding1),
            )
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(WooTheme.spacing.space10),
        ) {
            Icon(
                imageVector = WooIcons.Regular.Xmark,
                contentDescription = stringResource(R.string.dismiss),
                tint = colorResource(R.color.woo_white_alpha_060),
                modifier = Modifier.size(WooTheme.iconSize.size24),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DashboardScreenPreview() {
    WooDesignSystemThemeWithBackground {
        DashboardScreen(
            storeName = "Example Store",
            showShareStoreButton = true,
            onShareStoreClicked = {},
            showJetpackBenefitsBanner = true,
            onJetpackBenefitsBannerClicked = {},
            onJetpackBenefitsBannerDismissed = {},
            jitmContent = { jitmModifier ->
                Column(
                    modifier = jitmModifier
                        .background(WooTheme.colors.background.section)
                        .padding(WooTheme.padding.padding4),
                    verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2),
                ) {
                    Text(text = "Just-in-time message with natural height")
                    Text(text = "Its height follows content and text wrapping without a fixed size.")
                }
            },
            dashboardContent = { bodyModifier, _, leadingContent ->
                Column(modifier = bodyModifier) {
                    leadingContent()
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "Dashboard content")
                    }
                }
            },
        )
    }
}

@Preview(name = "Empty body and hidden JITM", widthDp = 360, heightDp = 640)
@Composable
private fun DashboardScreenEmptyBodyPreview() {
    WooDesignSystemThemeWithBackground {
        DashboardScreen(
            storeName = "Example Store",
            showShareStoreButton = true,
            onShareStoreClicked = {},
            showJetpackBenefitsBanner = true,
            onJetpackBenefitsBannerClicked = {},
            onJetpackBenefitsBannerDismissed = {},
            jitmContent = { jitmModifier ->
                Spacer(modifier = jitmModifier.height(0.dp))
            },
            dashboardContent = { bodyModifier, _, leadingContent ->
                Box(modifier = bodyModifier) {
                    leadingContent()
                }
            },
        )
    }
}

@Preview(name = "Jetpack benefits - RTL, 2x font", locale = "ar", fontScale = 2f, widthDp = 360)
@Composable
private fun JetpackBenefitsBannerRtlLargeFontPreview() {
    WooDesignSystemThemeWithBackground {
        JetpackBenefitsBanner(
            onClick = {},
            onDismiss = {},
        )
    }
}

private const val DASHBOARD_CONTAINER_TEST_TAG = "dashboard_container"
