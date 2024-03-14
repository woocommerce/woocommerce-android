package com.woocommerce.android.ui.coupons.create

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.woocommerce.android.R

@Composable
fun CouponTypePickerScreen(
    onPercentageDiscountClicked: () -> Unit,
    onFixedCartDiscountClicked: () -> Unit,
    onFixedProductDiscountClicked: () -> Unit
) {
    Column(modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))) {
        Text(
            text = stringResource(R.string.coupon_type_picker_title),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                start = 0.dp,
                top = dimensionResource(id = R.dimen.major_125),
                end = 0.dp,
                bottom = dimensionResource(id = R.dimen.major_75)
            )
        )
        CouponType(
            typeNameTitle = stringResource(R.string.coupon_type_picker_percentage_discount_title),
            subtitle = stringResource(R.string.coupon_type_picker_percentage_discount_subtitle),
            iconRes = R.drawable.ic_coupon_percentage,
            contentDescription = R.string.coupon_type_picker_percentage_content_description,
            onClick = onPercentageDiscountClicked
        )
        CouponType(
            typeNameTitle = stringResource(R.string.coupon_type_picker_fixed_cart_discount_title),
            subtitle = stringResource(R.string.coupon_type_picker_fixed_cart_discount_subtitle),
            iconRes = R.drawable.ic_coupon_fixed_cart,
            contentDescription = R.string.coupon_type_picker_fixed_cart_content_description,
            onClick = onFixedCartDiscountClicked
        )
        CouponType(
            typeNameTitle = stringResource(R.string.coupon_type_picker_fixed_product_discount_title),
            subtitle = stringResource(R.string.coupon_type_picker_fixed_product_discount_subtitle),
            iconRes = R.drawable.ic_coupon_fixed_product,
            contentDescription = R.string.coupon_type_picker_fixed_product_content_description,
            onClick = onFixedProductDiscountClicked
        )
    }
}

@Composable
fun CouponType(
    @DrawableRes iconRes: Int,
    typeNameTitle: String,
    subtitle: String,
    @StringRes contentDescription: Int,
    onClick: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = dimensionResource(id = R.dimen.major_75))
    ) {
        val (icon, title, description) = createRefs()
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = stringResource(contentDescription),
            modifier = Modifier
                .constrainAs(icon) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                }
                .size(dimensionResource(id = R.dimen.major_125))
        )
        Text(
            text = typeNameTitle,
            style = MaterialTheme.typography.body1,
            color = colorResource(R.color.color_on_surface_high_selector),
            modifier = Modifier
                .constrainAs(title) {
                    top.linkTo(icon.top)
                    start.linkTo(icon.end)
                }
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.body2,
            color = colorResource(R.color.color_on_surface_medium_selector),
            modifier = Modifier
                .constrainAs(description) {
                    top.linkTo(title.bottom)
                    start.linkTo(icon.end)
                }
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
        )
    }
}

@Preview
@Composable
fun CouponTypePreview() {
    CouponType(
        typeNameTitle = "Percentage Discount",
        subtitle = "Create a percentage discount for selected products",
        iconRes = R.drawable.ic_coupon_fixed_cart,
        contentDescription = R.string.coupon_type_picker_percentage_content_description,
        onClick = {}
    )
}

@Preview
@Composable
fun CouponTypePickerScreenPreview() {
    CouponTypePickerScreen(
        {},
        {},
        {}
    )
}
