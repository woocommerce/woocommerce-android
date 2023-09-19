package com.woocommerce.android.ui.products.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton

@Composable
fun MainLayout(
    enableProductHighlight: Boolean = true,
    footer: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = dimensionResource(id = R.dimen.major_100)),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            ) {
                Text(
                    text = stringResource(id = R.string.ai_product_name_sheet_title),
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier
                        .padding(vertical = dimensionResource(id = R.dimen.minor_100))
                )
                Text(
                    text = stringResource(id = R.string.ai_product_name_sheet_subtitle),
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier
                        .padding(vertical = dimensionResource(id = R.dimen.minor_100))
                )
            }

            Divider(
                color = colorResource(id = R.color.divider_color),
                thickness = dimensionResource(id = R.dimen.minor_10),
                modifier = Modifier
                    .padding(vertical = dimensionResource(id = R.dimen.minor_100))
            )

            Column(
                modifier = Modifier
                    .padding(dimensionResource(id = R.dimen.major_100))
            ) {
                WCOutlinedTextField(
                    value = "Some entered text about the product",
                    onValueChange = { },
                    label = "",
                    enabled = enableProductHighlight,
                    textFieldModifier = Modifier.height(dimensionResource(id = R.dimen.major_400))
                )

                Text(
                    text = stringResource(id = R.string.ai_product_name_sheet_input_description),
                    style = MaterialTheme.typography.caption,
                    color = colorResource(id = R.color.color_on_surface_medium),
                    modifier = Modifier
                        .padding(vertical = dimensionResource(id = R.dimen.minor_100))
                )

                footer()
            }
        }
    }
}

@Composable
fun StartLayout() {
    MainLayout {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_375)))

        WCColoredButton(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.ai_product_name_sheet_generate_button),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ai),
                    contentDescription = null,
                    tint = colorResource(id = R.color.woo_white)
                )
            }
        )
    }
}

@Composable
fun GeneratingLayout() {
    MainLayout(enableProductHighlight = false) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_300)))

        Column(
            modifier = Modifier
                .background(
                    color = colorResource(id = R.color.skeleton_compose_background),
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_50))
                )
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(id = R.dimen.major_100))
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            SkeletonView(
                modifier = Modifier
                    .width(dimensionResource(id = R.dimen.skeleton_text_large_width))
                    .height(dimensionResource(id = R.dimen.major_100))
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        }
    }
}

@Composable
fun ResultLayout() {
    MainLayout {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        GeneratedTextLayout()

        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10),
            modifier = Modifier
                .padding(vertical = dimensionResource(id = R.dimen.major_100))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            WCTextButton(
                onClick = { },
                modifier = Modifier.align(Alignment.CenterStart),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(id = R.color.color_on_surface)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.major_150))
                )
                Text(
                    modifier = Modifier.padding(start = dimensionResource(id = R.dimen.minor_100)),
                    text = stringResource(id = R.string.ai_product_name_sheet_regenerate_button)
                )
            }
            WCColoredButton(
                onClick = { },
                modifier = Modifier
                    .align(Alignment.CenterEnd),
            ) {
                Text(
                    text = stringResource(id = R.string.apply),
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
                )
            }
        }
    }
}

@Composable
fun GeneratedTextLayout() {
    Column(
        modifier = Modifier
            .background(
                color = colorResource(id = R.color.ai_generated_text_background),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_50))
            )
            .padding(dimensionResource(id = R.dimen.major_100))
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "The generated product name made by AI",
            style = MaterialTheme.typography.body1
        )

        WCTextButton(
            modifier = Modifier.align(Alignment.End),
            onClick = { },
            colors = ButtonDefaults.textButtonColors(
                contentColor = colorResource(id = R.color.color_on_surface_medium)
            )
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R.dimen.major_150))
            )
            Text(
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.minor_100)),
                text = stringResource(id = R.string.copy)
            )
        }
    }
}

@Composable
fun ErrorLayout() {
    MainLayout {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colors.error,
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_50))
                )
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100))
        ) {
            Text(
                text = stringResource(id = R.string.ai_product_name_sheet_error_message),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onError,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        WCColoredButton(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.ai_product_name_sheet_generate_button),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ai),
                    contentDescription = null,
                    tint = colorResource(id = R.color.woo_white)
                )
            }
        )
    }
}

@Preview
@Composable
fun StartLayoutPreview() {
    StartLayout()
}

@Preview
@Composable
fun GeneratingLayoutPreview() {
    GeneratingLayout()
}

@Preview
@Composable
fun ResultLayoutPreview() {
    ResultLayout()
}

@Preview
@Composable
fun ErrorLayoutPreview() {
    ErrorLayout()
}
