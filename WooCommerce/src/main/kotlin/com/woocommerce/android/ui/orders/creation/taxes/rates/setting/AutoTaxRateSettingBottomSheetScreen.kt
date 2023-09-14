package com.woocommerce.android.ui.orders.creation.taxes.rates.setting

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun AutoTaxRateSettingBottomSheetScreen(
    taxRateLabel: String,
    taxRateValue: String,
) {
    Column(modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))) {
        Text("Automatically adding tax rate")
        Spacer(Modifier.width(dimensionResource(id = R.dimen.major_100)))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(
                        dimensionResource(id = R.dimen.minor_10),
                        colorResource(id = R.color.woo_gray_80_alpha_012)
                    ),
                    RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_large))
                )
        ) {
            Text(
                modifier = Modifier
                    .weight(1F)
                    .padding(dimensionResource(id = R.dimen.major_100)),
                text = taxRateLabel
            )
            Text(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)),
                text = taxRateValue
            )
        }
        Spacer(Modifier.width(dimensionResource(id = R.dimen.major_100)))
        WCTextButton(onClick = {}) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_edit),
                contentDescription = null,
                tint = colorResource(id = R.color.color_on_surface)
            )
            Spacer(Modifier.width(dimensionResource(id = R.dimen.major_100)))
            Text(
                modifier = Modifier.weight(1F),
                text = "Set a new tax rate for this order",
                color = colorResource(id = R.color.color_on_surface)
            )
        }
        TextButton(onClick = {}) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_clear),
                contentDescription = null,
                tint = colorResource(id = R.color.woo_red_70)
            )
            Spacer(Modifier.width(dimensionResource(id = R.dimen.major_100)))
            Text(
                modifier = Modifier.weight(1F),
                text = "Set a new tax rate for this order",
                color = colorResource(id = R.color.woo_red_70)
            )
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AutoTaxRateSettingBottomSheetScreenPreview() = WooThemeWithBackground {
    AutoTaxRateSettingBottomSheetScreen(
        taxRateLabel = "Tax rate",
        taxRateValue = "10%"
    )
}