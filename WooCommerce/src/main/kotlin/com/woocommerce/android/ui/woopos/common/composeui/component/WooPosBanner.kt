package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
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

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries")
fun WooPosBanner(
    title: String,
    message: String,
    onClose: () -> Unit,
    onLearnMore: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
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
                    .padding(24.dp)
            ) {
                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    val (icon, header, description, close) = createRefs()

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(8.dp)
                            .constrainAs(icon) {
                                top.linkTo(parent.top)
                                start.linkTo(parent.start)
                                bottom.linkTo(parent.bottom)
                            }
                    ) {
                        Icon(
                            painterResource(id = R.drawable.info),
                            contentDescription = "Info",
                            tint = colorResource(id = R.color.woo_purple_30),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.constrainAs(header) {
                            top.linkTo(parent.top)
                            start.linkTo(icon.end, margin = 16.dp)
                            end.linkTo(close.start, margin = 16.dp)
                            width = Dimension.fillToConstraints
                        }
                    )

                    val annotatedText = buildAnnotatedString {
                        append(message)
                        withStyle(style = SpanStyle(color = colorResource(id = R.color.woo_purple_50))) {
                            append(" Learn more")
                        }
                    }

                    Text(
                        text = annotatedText,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(id = R.color.color_on_surface_high),
                        modifier = Modifier
                            .clickable { onLearnMore() }
                            .constrainAs(description) {
                                top.linkTo(header.bottom, margin = 8.dp)
                                start.linkTo(header.start)
                                end.linkTo(close.start)
                                width = Dimension.fillToConstraints
                            }
                            .padding(end = 8.dp)
                    )

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onClose() }
                            .constrainAs(close) {
                                top.linkTo(header.top)
                                bottom.linkTo(header.bottom)
                                end.linkTo(parent.end)
                            }
                    )
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
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            WooPosBanner(
                title = "Showing simple products only",
                message = "Only simple physical products are compatible with POS right now. Other product types," +
                    " such as variable and virtual, will become available in future updates. ",
                onClose = { },
                onLearnMore = { }
            )
        }
    }
}
