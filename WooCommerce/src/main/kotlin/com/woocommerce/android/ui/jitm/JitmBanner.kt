package com.woocommerce.android.ui.jitm

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooButtonSize
import com.woocommerce.android.ui.compose.designsystem.component.WooIconButton
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.component.WooOverflowMenu
import com.woocommerce.android.ui.compose.designsystem.component.WooOverflowMenuItem
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.Ellipsis
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import com.woocommerce.android.util.UiHelpers

@Composable
internal fun JitmBanner(
    state: JitmState.Banner,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WooTheme.radius.extraLarge),
        color = WooTheme.colors.surface.bright,
        contentColor = WooTheme.colors.surface.onDefault,
        shadowElevation = WooTheme.spacing.space0,
        tonalElevation = WooTheme.spacing.space0,
    ) {
        Box {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = WooTheme.padding.padding5,
                            top = WooTheme.padding.padding5,
                            end = WooTheme.padding.padding3,
                            bottom = WooTheme.padding.padding5,
                        ),
                    verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
                ) {
                    state.badgeIcon?.let { BadgeIcon(it) }
                    Text(
                        text = UiHelpers.getTextOfUiString(LocalContext.current, state.title),
                        color = WooTheme.colors.surface.onDefault,
                        style = WooTheme.text.titleMedium.strong,
                    )
                    Text(
                        text = UiHelpers.getTextOfUiString(LocalContext.current, state.description),
                        color = WooTheme.colors.surface.onVariant,
                        style = WooTheme.text.bodyLarge.regular,
                    )
                    WooOutlinedButton(
                        text = UiHelpers.getTextOfUiString(LocalContext.current, state.primaryActionLabel),
                        onClick = state.onPrimaryActionClicked,
                        size = WooButtonSize.Small,
                    )
                }

                BackgroundImage(
                    image = state.backgroundImage,
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .width(JITM_ILLUSTRATION_WIDTH),
                )
            }

            DismissMenu(
                onDismissClicked = state.onDismissClicked,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun DismissMenu(
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WooOverflowMenu(
        trigger = { onClick ->
            WooIconButton(
                imageVector = WooIcons.Regular.Ellipsis,
                contentDescription = stringResource(R.string.more_menu),
                onClick = onClick,
            )
        },
        modifier = modifier,
    ) { dismiss ->
        WooOverflowMenuItem(
            text = stringResource(R.string.card_reader_upsell_card_reader_banner_hide_content),
            onClick = {
                dismiss()
                onDismissClicked()
            },
        )
    }
}

@Composable
private fun BackgroundImage(
    image: JitmState.Banner.LocalOrRemoteImage,
    modifier: Modifier = Modifier,
) {
    when (image) {
        is JitmState.Banner.LocalOrRemoteImage.Local -> Image(
            painter = painterResource(id = image.drawableId),
            contentDescription = null,
            contentScale = ContentScale.Inside,
            modifier = modifier,
        )

        is JitmState.Banner.LocalOrRemoteImage.Remote -> AsyncImage(
            model = imageRequest(
                lightModeUrl = image.urlLightMode,
                darkModeUrl = image.urlDarkMode,
            ),
            contentDescription = null,
            modifier = modifier,
        )
    }
}

@Composable
private fun BadgeIcon(
    icon: JitmState.Banner.RemoteIcon,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = imageRequest(
            lightModeUrl = icon.urlLightMode,
            darkModeUrl = icon.urlDarkMode,
        ),
        contentDescription = null,
        modifier = modifier
            .height(JITM_BADGE_HEIGHT)
            .testTag(JITM_BADGE_TEST_TAG),
    )
}

@Composable
private fun imageRequest(
    lightModeUrl: String,
    darkModeUrl: String,
): ImageRequest = ImageRequest.Builder(LocalContext.current)
    .data(if (isSystemInDarkTheme()) darkModeUrl else lightModeUrl)
    .decoderFactory(SvgDecoder.Factory())
    .build()

@PreviewLightDark
@Composable
private fun JitmBannerWithBadgePreview() {
    WooDesignSystemThemeWithBackground {
        JitmBanner(
            state = previewBannerState(badgeIcon = previewBadgeIcon()),
        )
    }
}

@PreviewLightDark
@Composable
private fun JitmBannerWithoutBadgePreview() {
    WooDesignSystemThemeWithBackground {
        JitmBanner(state = previewBannerState())
    }
}

@Preview(name = "Long text / large font", fontScale = 2f, widthDp = 360)
@Composable
private fun JitmBannerLongTextPreview() {
    WooDesignSystemThemeWithBackground {
        JitmBanner(
            state = previewBannerState(
                title = UiString.UiStringText(
                    "Build stronger customer relationships with Point of Sale for WooCommerce",
                ),
                description = UiString.UiStringText(
                    "Take secure in-person payments and keep orders, customers, and inventory in sync wherever " +
                        "you sell.",
                ),
            ),
        )
    }
}

@Preview(name = "RTL", locale = "ar", widthDp = 360)
@Composable
private fun JitmBannerRtlPreview() {
    WooDesignSystemThemeWithBackground {
        JitmBanner(
            state = previewBannerState(
                title = UiString.UiStringText("اكتشف نقطة البيع من WooCommerce"),
                description = UiString.UiStringText(
                    "استقبل المدفوعات وحافظ على مزامنة مخزون متجرك.",
                ),
            ),
        )
    }
}

@Composable
private fun previewBadgeIcon(): JitmState.Banner.RemoteIcon {
    val url = "android.resource://${LocalContext.current.packageName}/${R.drawable.ic_badge_new}"
    return JitmState.Banner.RemoteIcon(urlLightMode = url, urlDarkMode = url)
}

private fun previewBannerState(
    title: UiString = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_title),
    description: UiString = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_description),
    badgeIcon: JitmState.Banner.RemoteIcon? = null,
) = JitmState.Banner(
    onPrimaryActionClicked = {},
    onDismissClicked = {},
    title = title,
    description = description,
    primaryActionLabel = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_cta),
    backgroundImage = JitmState.Banner.LocalOrRemoteImage.Local(
        R.drawable.ic_banner_upsell_card_reader_illustration,
    ),
    badgeIcon = badgeIcon,
)

internal const val JITM_BADGE_TEST_TAG = "jitm_badge"
private val JITM_ILLUSTRATION_WIDTH = 156.dp
private val JITM_BADGE_HEIGHT = 26.dp
