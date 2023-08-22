package com.woocommerce.android.ui.orders.creation.taxes

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

data class TaxRateInfoModalState(
    val taxSetting: TaxBasedOnSetting,
    val taxLines: Collection<Order.TaxLine> = emptyList(),
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TaxRateInfoModal(
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        content = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(size = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(dimensionResource(id = R.dimen.major_150)),
                ) {

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

                    Text(
                        text = "Taxes & Tax Rates",
                        style = MaterialTheme.typography.h5,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

                    Text(
                        text = "Taxes are calculated by matching your customer’s billing or shipping address, or your shop address to a tax rate location.",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

                    Text(
                        text = "Tax rates for different locations can be managed in your store’s admin.",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

                    Divider()

                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

                    WCColoredButton(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_redirect),
                                modifier = Modifier.size(20.dp),
                                contentDescription = "Edit Tax Rates in Admin Button",
                            )
                        },
                        text = "Edit Tax Rates in Admin"
                    )

                    WCOutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                    ) {
                        Text(text = "Done")
                    }
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                }
            }
        }
    )
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TaxRateInfoModalPreview() {
    WooThemeWithBackground {
        TaxRateInfoModal(
        )
    }
}
