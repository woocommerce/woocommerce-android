package com.woocommerce.android.ui.login.storecreation.plans

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.Plan
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.Plan.BillingPeriod.MONTHLY
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.Plan.Feature
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.PlanState

@Composable
fun PlanScreen(viewModel: PlansViewModel) {
    viewModel.viewState.observeAsState(LoadingState).value.let { viewState ->
        when (viewState) {
            is PlanState -> PlanInformation(viewState, viewModel::onCloseClicked, viewModel::onConfirmClicked)
            is ErrorState -> PlanError(viewModel::onRetryClicked)
            LoadingState -> PlanLoading()
        }
    }
}

@Composable
private fun PlanInformation(
    viewState: PlanState,
    onCloseClicked: () -> Unit,
    onConfirmClicked: () -> Unit
) {

    val systemUiController = rememberSystemUiController()
    val wooDarkPurple = colorResource(id = color.woo_purple_90)
    val statusBarColor = colorResource(id = color.color_status_bar)
    DisposableEffect(key1 = viewState) {
        systemUiController.setSystemBarsColor(
            color = wooDarkPurple,
        )
        onDispose {
            systemUiController.setSystemBarsColor(
                color = statusBarColor,
            )
        }
    }

    ConstraintLayout(
        modifier = Modifier
            .background(colorResource(id = R.color.woo_purple_90))
            .verticalScroll(rememberScrollState())
    ) {
        val (icon, image, title, price, period, features, button) = createRefs()

        IconButton(
            onClick = onCloseClicked,
            modifier = Modifier.constrainAs(icon) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
            }
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(id = R.string.back),
                tint = colorResource(id = R.color.white)
            )
        }

        Image(
            painter = painterResource(id = drawable.img_plan),
            contentDescription = "Plan illustration",
            modifier = Modifier
                .constrainAs(image) {
                    top.linkTo(parent.top)
                    end.linkTo(parent.end)
                }
                .height(dimensionResource(id = R.dimen.image_major_200)),
            contentScale = ContentScale.FillHeight
        )
        Text(
            text = viewState.plan.name,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.white),
            fontSize = 20.sp,
            modifier = Modifier
                .constrainAs(title) {
                    top.linkTo(image.top)
                    bottom.linkTo(image.bottom)
                    start.linkTo(parent.start)
                }
                .padding(start = dimensionResource(id = R.dimen.major_125))
        )
        Text(
            text = viewState.plan.formattedPrice,
            color = colorResource(id = R.color.white),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .constrainAs(price) {
                    top.linkTo(title.bottom)
                    start.linkTo(title.start)
                }
                .padding(start = dimensionResource(id = R.dimen.major_125))
        )
        Text(
            text = "/${stringResource(viewState.plan.billingPeriod.nameId)}",
            color = colorResource(id = R.color.woo_purple_dark_secondary),
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier
                .constrainAs(period) {
                    top.linkTo(price.bottom)
                    start.linkTo(price.start)
                }
                .padding(
                    start = dimensionResource(id = R.dimen.major_125)
                )
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_75))
                .constrainAs(features) {
                    top.linkTo(period.bottom)
                }
        ) {
            Divider(
                color = colorResource(id = R.color.woo_gray_40),
                thickness = dimensionResource(id = R.dimen.minor_10),
                modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.major_75))
            )
            Text(
                fontWeight = FontWeight.Bold,
                text = stringResource(id = string.store_creation_ecommerce_plan_features_tagline),
                color = colorResource(id = R.color.white),
                fontSize = 28.sp,
                lineHeight = 36.sp
            )
            Row(
                modifier = Modifier.padding(
                    top = dimensionResource(id = R.dimen.minor_50),
                    bottom = dimensionResource(id = R.dimen.major_75)
                )
            ) {
                Text(
                    text = stringResource(id = string.store_creation_ecommerce_plan_powered_by),
                    color = colorResource(id = R.color.woo_purple_dark_secondary),
                    style = MaterialTheme.typography.caption
                )
                Image(
                    painter = painterResource(id = drawable.ic_wordpress),
                    contentDescription = "WordPress logo",
                    modifier = Modifier
                        .height(dimensionResource(id = R.dimen.major_110))
                        .padding(horizontal = dimensionResource(id = R.dimen.minor_50)),
                    contentScale = ContentScale.FillHeight,
                    colorFilter = ColorFilter.tint(color = colorResource(id = R.color.woo_purple_dark_secondary))
                )
                Text(
                    fontWeight = FontWeight.Bold,
                    text = stringResource(id = string.store_creation_ecommerce_plan_wordpress),
                    color = colorResource(id = R.color.woo_purple_dark_secondary),
                    style = MaterialTheme.typography.caption
                )
            }
            viewState.plan.features.forEach {
                PlanFeatureRow(iconId = it.iconId, textId = it.textId)
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_75)),
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(button) {
                    top.linkTo(features.bottom)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ) {
            Divider(
                color = colorResource(id = R.color.woo_white_alpha_008),
                thickness = dimensionResource(id = R.dimen.minor_10),
            )
            WCColoredButton(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                    .fillMaxWidth(),
                onClick = onConfirmClicked
            ) {
                val periodText = stringResource(id = viewState.plan.billingPeriod.nameId)
                Text(
                    text = stringResource(
                        id = string.store_creation_ecommerce_plan_purchase_button_title,
                        "${viewState.plan.formattedPrice}/$periodText"
                    )
                )
            }
            Text(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                    .fillMaxWidth(),
                text = stringResource(id = string.store_creation_ecommerce_plan_refund_reminder),
                color = colorResource(id = R.color.woo_purple_dark_secondary),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PlanFeatureRow(@DrawableRes iconId: Int, @StringRes textId: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconId),
            contentDescription = "Feature icon",
            modifier = Modifier
                .height(26.dp)
                .padding(end = dimensionResource(id = R.dimen.minor_100)),
            contentScale = ContentScale.FillHeight,
            colorFilter = ColorFilter.tint(color = colorResource(id = R.color.woo_purple_15))
        )
        Text(
            text = stringResource(id = textId),
            color = colorResource(id = R.color.white),
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
private fun PlanError(onRetryClicked: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(id = string.store_creation_ecommerce_plan_error),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)),
            textAlign = TextAlign.Center
        )

        WCColoredButton(
            onClick = onRetryClicked,
            text = stringResource(id = string.retry),
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
        )
    }
}

@Composable
private fun PlanLoading() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()
    }
}

@Preview
@Preview()
@Composable
fun PreviewPlanInformation() {
    PlanInformation(
        PlanState(
            Plan(
                name = "eCommerce",
                billingPeriod = MONTHLY,
                formattedPrice = "$69.99",
                features = listOf(
                    Feature(
                        iconId = drawable.ic_star,
                        textId = string.store_creation_ecommerce_plan_feature_themes
                    ),
                    Feature(
                        iconId = drawable.ic_box,
                        textId = string.store_creation_ecommerce_plan_feature_products
                    ),
                    Feature(
                        iconId = drawable.ic_present,
                        textId = string.store_creation_ecommerce_plan_feature_subscriptions
                    ),
                    Feature(
                        iconId = drawable.ic_chart,
                        textId = string.store_creation_ecommerce_plan_feature_reports
                    ),
                    Feature(
                        iconId = drawable.ic_dollar,
                        textId = string.store_creation_ecommerce_plan_feature_payments
                    ),
                    Feature(
                        iconId = drawable.ic_truck,
                        textId = string.store_creation_ecommerce_plan_feature_shipping_labels
                    ),
                    Feature(
                        iconId = drawable.ic_megaphone,
                        textId = string.store_creation_ecommerce_plan_feature_sales
                    )
                )
            )
        ),
        { },
        { }
    )
}
