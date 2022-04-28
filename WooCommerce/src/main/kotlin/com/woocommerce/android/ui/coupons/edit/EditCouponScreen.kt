package com.woocommerce.android.ui.coupons.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCOutlinedSpinner
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.theme.WooTheme
import java.math.BigDecimal

@Composable
fun EditCouponScreen(viewModel: EditCouponViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        EditCouponScreen(it)
    }
}

@Composable
fun EditCouponScreen(viewState: EditCouponViewModel.ViewState) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .background(color = MaterialTheme.colors.surface)
            .verticalScroll(scrollState)
            .fillMaxSize()
    ) {
        DetailsSection(viewState)
        ConditionsSection(viewState)
        UsageRestrictionsSection(viewState)
        WCColoredButton(
            onClick = { /*TODO*/ },
            text = stringResource(id = R.string.coupon_edit_save_button)
        )
    }
}

@Composable
private fun DetailsSection(viewState: EditCouponViewModel.ViewState) {
    val couponDraft = viewState.couponDraft
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.major_100)
            )
    ) {
        Text(
            text = stringResource(id = R.string.coupon_edit_details_section).toUpperCase(Locale.current),
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        WCOutlinedTextField(
            value = couponDraft.amount?.toPlainString().orEmpty(),
            label = stringResource(id = R.string.coupon_edit_amount_hint, "%"/*TODO*/),
            onValueChange = { /*TODO*/ },
            helperText = stringResource(
                if (couponDraft.type is Coupon.Type.Percent) R.string.coupon_edit_amount_percentage_helper
                else R.string.coupon_edit_amount_rate_helper
            ),
            modifier = Modifier.fillMaxWidth()
        )
        WCOutlinedTextField(
            value = couponDraft.code.orEmpty(),
            label = stringResource(id = R.string.coupon_edit_code_hint),
            onValueChange = { /*TODO*/ },
            helperText = stringResource(id = R.string.coupon_edit_code_helper),
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(
            onClick = { /*TODO*/ },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.secondary)
        ) {
            Text(text = stringResource(id = R.string.coupon_edit_regenerate_coupon).toUpperCase(Locale.current))
        }

        WCOutlinedButton(
            onClick = { /*TODO*/ },
            text = "Edit Description",
            leadingIcon = {
                Icon(
                    imageVector = Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(id = dimen.major_100))
                )
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.onSurface),
            modifier = Modifier.fillMaxWidth()
        )

        WCOutlinedSpinner(
            onClick = { /*TODO*/ },
            value = couponDraft.dateExpiresGmt?.toString() ?: "None",
            label = stringResource(id = R.string.coupon_edit_expiry_date)
        )
    }
}

@Composable
private fun ConditionsSection(viewState: EditCouponViewModel.ViewState) {
    /*TODO*/
}

@Composable
private fun UsageRestrictionsSection(viewState: EditCouponViewModel.ViewState) {
    /*TODO*/
}

@Composable
@Preview
private fun EditCouponPreview() {
    WooTheme {
        EditCouponScreen(
            viewState = EditCouponViewModel.ViewState(
                couponDraft = Coupon(
                    id = 0L,
                    code = "code",
                    amount = BigDecimal.TEN,
                    products = emptyList(),
                    categories = emptyList(),
                    excludedProducts = emptyList(),
                    excludedCategories = emptyList(),
                    restrictedEmails = emptyList()
                ),
                localizedType = "Fixed Rate Discount"
            )
        )
    }
}
