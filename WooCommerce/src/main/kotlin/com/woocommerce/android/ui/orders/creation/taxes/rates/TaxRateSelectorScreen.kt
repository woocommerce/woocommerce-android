package com.woocommerce.android.ui.orders.creation.taxes.rates

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun TaxRateSelectorScreen(
    onEditTaxRatesInAdminClicked: () -> Unit,
    onInfoIconClicked: () -> Unit
) {
    Scaffold(
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(it), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(onInfoIconClicked)
            TaxRates()
            Divider()
            Footer(onEditTaxRatesInAdminClicked)
        }
    }
}

@Composable
private fun TaxRates() {
    Column {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(
                        id = R.dimen.minor_100
                    )
                ),
            fontSize = 13.sp,
            color = colorResource(id = R.color.woo_gray_40),
            text = stringResource(R.string.tax_rate_selector_list_header)
        )
        LazyColumn {
        }
    }
}

@Composable
fun Header(onInfoIconClicked: () -> Unit) {
    Box(modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(
                        dimensionResource(id = R.dimen.minor_10),
                        colorResource(id = R.color.woo_gray_80_alpha_012)
                    ),
                    RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_large))
                ),
        ) {
            IconButton(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.minor_25)),
                onClick = onInfoIconClicked
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_info_outline_20dp),
                    tint = MaterialTheme.colors.primary,
                    contentDescription = stringResource(R.string.tax_rate_selector_info_icon_content_description)
                )
            }
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        dimensionResource(id = R.dimen.minor_00),
                        dimensionResource(id = R.dimen.major_100),
                        dimensionResource(id = R.dimen.major_100),
                        dimensionResource(id = R.dimen.major_100)
                    ),
                style = MaterialTheme.typography.body2,
                text = stringResource(R.string.tax_rate_selector_header_label),
            )
        }
    }
}

@Composable
private fun Footer(
    onEditTaxRatesInAdminClicked: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                dimensionResource(id = R.dimen.major_100),
                dimensionResource(id = R.dimen.major_100),
                dimensionResource(id = R.dimen.major_150),
                dimensionResource(id = R.dimen.major_100)
            )
    ) {
        val (footerLabel, goToAdminButton) = createRefs()
        Text(
            modifier = Modifier.constrainAs(footerLabel) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(parent.top)
            },
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.woo_gray_40),
            text = stringResource(R.string.tax_rate_selector_footer_label)
        )
        EditTaxRatesInAdminButton(
            modifier = Modifier.constrainAs(goToAdminButton) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(footerLabel.bottom)
            },
            onEditTaxRatesInAdminClicked
        )
    }
}

@Composable
fun EditTaxRatesInAdminButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(stringResource(R.string.tax_rate_selector_edit_rates_button_label))
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_external),
                contentDescription = null,
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.image_minor_50))
                    .align(Alignment.CenterVertically)
                    .padding(
                        dimensionResource(id = R.dimen.minor_50),
                    )

            )
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TaxRateSelectorScreenPreview() = WooThemeWithBackground {
    TaxRateSelectorScreen(onEditTaxRatesInAdminClicked = {}, onInfoIconClicked = {})
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FooterPreview() = WooThemeWithBackground {
    Footer(onEditTaxRatesInAdminClicked = {})
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TaxRatesPreview() = WooThemeWithBackground {
    TaxRates()
}
