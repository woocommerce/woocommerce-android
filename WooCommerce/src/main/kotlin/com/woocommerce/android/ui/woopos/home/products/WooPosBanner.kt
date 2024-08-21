package com.woocommerce.android.ui.woopos.home.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries")
fun WooPosBanner(
    title: String,
    message: String,
    bannerIcon: Int,
    onClose: () -> Unit,
    onLearnMore: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 2.dp.toAdaptivePadding(),
                end = 2.dp.toAdaptivePadding(),
                bottom = 16.dp.toAdaptivePadding()
            )
            .testTag("woo_pos_simple_products_banner")

    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp.toAdaptivePadding())
            ) {
                ConstraintLayout(
                    modifier = Modifier
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
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.87f),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(
                                start = 32.dp.toAdaptivePadding(),
                                bottom = 8.dp.toAdaptivePadding()
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
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
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
                                start = 24.dp.toAdaptivePadding(),
                                end = 18.dp.toAdaptivePadding()
                            )
                    ) {
                        Text(
                            modifier = Modifier
                                .clickable {
                                    onLearnMore()
                                }
                                .padding(
                                    start = 8.dp.toAdaptivePadding(),
                                    top = 8.dp.toAdaptivePadding(),
                                    bottom = 8.dp.toAdaptivePadding(),
                                ),
                            text = annotatedText,
                            style = MaterialTheme.typography.body1,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.87f)
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
                            modifier = Modifier.size(32.dp),
                            imageVector = Icons.Default.Close,
                            tint = MaterialTheme.colors.onSurface,
                            contentDescription = stringResource(
                                id = R.string.woopos_banner_simple_products_close_content_description
                            ),
                        )
                    }
                }
            }
        }
    }
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
