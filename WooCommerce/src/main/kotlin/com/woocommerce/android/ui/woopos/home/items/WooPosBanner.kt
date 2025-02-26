package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries")
fun WooPosBanner(
    title: String,
    message: String,
    bannerIcon: Int,
    onClose: () -> Unit,
    onLearnMore: () -> Unit
) {
    val bannerContentDescription = getGroupedContentDescription(title, message)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = WooPosSpacing.XSmall.value.toAdaptivePadding(),
                end = WooPosSpacing.XSmall.value.toAdaptivePadding(),
                bottom = WooPosSpacing.Medium.value.toAdaptivePadding()
            )
            .semantics {
                contentDescription = bannerContentDescription
            }
            .focusable()
    ) {
        WooPosCard(
            shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            elevation = WooPosElevation.Medium,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            ConstraintLayout(
                modifier = Modifier
                    .padding(WooPosSpacing.Large.value.toAdaptivePadding())
                    .fillMaxWidth()
            ) {
                val (icon, header, description, close) = createRefs()

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .constrainAs(icon) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            bottom.linkTo(parent.bottom)
                        }
                ) {
                    Icon(
                        painterResource(id = bannerIcon),
                        contentDescription = stringResource(
                            id = R.string.woopos_banner_simple_products_info_content_description
                        ),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                WooPosText(
                    text = title,
                    style = WooPosTypography.BodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(
                            start = WooPosSpacing.XLarge.value.toAdaptivePadding(),
                            bottom = WooPosSpacing.Small.value.toAdaptivePadding()
                        )
                        .constrainAs(header) {
                            top.linkTo(parent.top)
                            start.linkTo(icon.end)
                            end.linkTo(close.start)
                            width = Dimension.fillToConstraints
                        }
                )

                val annotatedText = buildAnnotatedString {
                    append(message)
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append(" ")
                        append(stringResource(id = R.string.woopos_banner_simple_products_only_message_learn_more))
                    }
                }

                Box(
                    modifier = Modifier
                        .constrainAs(description) {
                            top.linkTo(header.bottom)
                            start.linkTo(header.start)
                            end.linkTo(close.start)
                            width = Dimension.fillToConstraints
                        }
                        .padding(
                            start = WooPosSpacing.Large.value.toAdaptivePadding(),
                            end = WooPosSpacing.Medium.value.toAdaptivePadding()
                        )
                ) {
                    WooPosText(
                        modifier = Modifier
                            .clickable {
                                onLearnMore()
                            }
                            .padding(
                                start = WooPosSpacing.Small.value.toAdaptivePadding(),
                                top = WooPosSpacing.Small.value.toAdaptivePadding(),
                                bottom = WooPosSpacing.Small.value.toAdaptivePadding(),
                            ),
                        text = annotatedText,
                        style = WooPosTypography.BodyMedium,
                        color = WooPosTheme.colors.onSurfaceVariantHighest,
                    )
                }

                IconButton(
                    modifier = Modifier
                        .constrainAs(close) {
                            top.linkTo(header.top)
                            bottom.linkTo(header.bottom)
                            end.linkTo(parent.end)
                        },
                    onClick = { onClose() }
                ) {
                    Icon(
                        modifier = Modifier.size(WooPosSpacing.XLarge.value),
                        imageVector = Icons.Default.Close,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        contentDescription = stringResource(
                            id = R.string.woopos_banner_simple_products_close_content_description
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun getGroupedContentDescription(title: String, message: String): String {
    val bannerContentDescription = stringResource(id = R.string.woopos_banner_simple_products_content_description)
    val learnMoreContentDescription = stringResource(
        id = R.string.woopos_banner_simple_products_learn_more_content_description
    )
    val combinedContentDescription = "$bannerContentDescription\n$title\n$message\n$learnMoreContentDescription"
    return combinedContentDescription
}

@WooPosPreview
@Composable
fun PreviewWooPosBannerScreen() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp.toAdaptivePadding()),
            contentAlignment = Alignment.Center
        ) {
            WooPosBanner(
                title = "Showing simple products only",
                message = "Only simple physical products are compatible with POS right now. Other product types," +
                    " such as variable and virtual, will become available in future updates. ",
                bannerIcon = R.drawable.info,
                onClose = { },
                onLearnMore = { }
            )
        }
    }
}
