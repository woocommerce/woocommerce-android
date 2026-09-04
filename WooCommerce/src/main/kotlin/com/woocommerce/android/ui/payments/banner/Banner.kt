package com.woocommerce.android.ui.payments.banner

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.compose.component.WCOverflowMenu
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.jitm.JitmState
import com.woocommerce.android.util.UiHelpers

@Composable
fun Banner(bannerState: JitmState.Banner) {
    val majorMargin = dimensionResource(id = R.dimen.major_100)
    val minorMargin = dimensionResource(id = R.dimen.minor_100)
    val ctaContentHorizontalPadding = dimensionResource(id = R.dimen.major_75)

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        ConstraintLayout(
            modifier = Modifier.fillMaxWidth()
        ) {
            val overflowMenu = createRef()
            val badge = createRef()
            val title = createRef()
            val description = createRef()
            val ctaButton = createRef()
            val backgroundImage = createRef()

            WCOverflowMenu(
                items = listOf(stringResource(R.string.card_reader_upsell_card_reader_banner_hide_content)),
                onSelected = { bannerState.onDismissClicked() },
                tint = colorResource(id = R.color.color_on_surface),
                modifier = Modifier.constrainAs(overflowMenu) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                }
            )

            if (bannerState.badgeIcon != null) {
                BadgeIcon(
                    bannerState = bannerState,
                    modifier = Modifier.constrainAs(badge) {
                        top.linkTo(parent.top, margin = majorMargin)
                        start.linkTo(parent.start, margin = majorMargin)
                    }
                )
            }

            Text(
                text = UiHelpers.getTextOfUiString(LocalContext.current, bannerState.title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .constrainAs(title) {
                        if (bannerState.badgeIcon != null) {
                            top.linkTo(badge.bottom, margin = minorMargin)
                        } else {
                            top.linkTo(parent.top, margin = majorMargin)
                        }
                        start.linkTo(parent.start, margin = majorMargin)
                        end.linkTo(backgroundImage.start, margin = minorMargin)
                        width = Dimension.fillToConstraints
                    }
                    .padding(bottom = minorMargin)
            )

            Text(
                text = UiHelpers.getTextOfUiString(LocalContext.current, bannerState.description),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .constrainAs(description) {
                        top.linkTo(title.bottom)
                        start.linkTo(parent.start, margin = majorMargin)
                        end.linkTo(backgroundImage.start, margin = minorMargin)
                        width = Dimension.fillToConstraints
                    }
                    .padding(bottom = minorMargin)
            )

            TextButton(
                onClick = bannerState.onPrimaryActionClicked,
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                modifier = Modifier.constrainAs(ctaButton) {
                    top.linkTo(description.bottom)
                    start.linkTo(parent.start, margin = majorMargin - ctaContentHorizontalPadding)
                }
            ) {
                Text(
                    text = UiHelpers.getTextOfUiString(LocalContext.current, bannerState.primaryActionLabel),
                    color = colorResource(id = R.color.color_primary),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            BackgroundImage(
                bannerState = bannerState,
                modifier = Modifier.constrainAs(backgroundImage) {
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
            )
        }
    }
}

@Composable
private fun BackgroundImage(
    bannerState: JitmState.Banner,
    modifier: Modifier = Modifier
) {
    val imageWidth = dimensionResource(id = R.dimen.image_major_150)
    when (val icon = bannerState.backgroundImage) {
        is JitmState.Banner.LocalOrRemoteImage.Local -> {
            Image(
                painter = painterResource(id = icon.drawableId),
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier = modifier.width(imageWidth)
            )
        }
        is JitmState.Banner.LocalOrRemoteImage.Remote -> {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(
                        if (isSystemInDarkTheme()) {
                            icon.urlDarkMode
                        } else {
                            icon.urlLightMode
                        }
                    )
                    .decoderFactory(SvgDecoder.Factory())
                    .build(),
                contentDescription = null,
                modifier = modifier.width(imageWidth)
            )
        }
    }
}

@Composable
private fun BadgeIcon(
    bannerState: JitmState.Banner,
    modifier: Modifier = Modifier
) {
    when (val icon = bannerState.badgeIcon) {
        is JitmState.Banner.LabelOrRemoteIcon.Label -> {
            val bcgColor = colorResource(id = R.color.woo_purple_0)
            Text(
                text = UiHelpers.getTextOfUiString(LocalContext.current, icon.label),
                color = colorResource(id = R.color.woo_purple_60),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = modifier
                    .drawBehind {
                        drawRoundRect(
                            color = bcgColor,
                            cornerRadius = CornerRadius(x = 8.dp.toPx(), y = 8.dp.toPx()),
                        )
                    }
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.minor_100),
                        vertical = dimensionResource(id = R.dimen.minor_50)
                    )
            )
        }
        is JitmState.Banner.LabelOrRemoteIcon.Remote -> {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(
                        if (isSystemInDarkTheme()) {
                            icon.urlDarkMode
                        } else {
                            icon.urlLightMode
                        }
                    )
                    .decoderFactory(SvgDecoder.Factory())
                    .build(),
                contentDescription = null,
                modifier = modifier.height(26.dp)
            )
        }
        null -> Unit
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PaymentScreenBannerPreview() {
    WooThemeWithBackground {
        Banner(
            JitmState.Banner(
                onPrimaryActionClicked = {},
                onDismissClicked = {},
                title = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_title),
                description = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_description),
                primaryActionLabel = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_cta),
                backgroundImage = JitmState.Banner.LocalOrRemoteImage.Local(
                    R.drawable.ic_banner_upsell_card_reader_illustration
                ),
                badgeIcon = JitmState.Banner.LabelOrRemoteIcon.Label(
                    UiString.UiStringRes(
                        R.string.card_reader_upsell_card_reader_banner_new
                    )
                ),
            )
        )
    }
}

@Preview(name = "No badge - Light mode")
@Preview(name = "No badge - Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PaymentScreenBannerNoBadgePreview() {
    WooThemeWithBackground {
        Banner(
            JitmState.Banner(
                onPrimaryActionClicked = {},
                onDismissClicked = {},
                title = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_title),
                description = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_description),
                primaryActionLabel = UiString.UiStringRes(R.string.card_reader_upsell_card_reader_banner_cta),
                backgroundImage = JitmState.Banner.LocalOrRemoteImage.Local(
                    R.drawable.ic_banner_upsell_card_reader_illustration
                ),
                badgeIcon = null,
            )
        )
    }
}
