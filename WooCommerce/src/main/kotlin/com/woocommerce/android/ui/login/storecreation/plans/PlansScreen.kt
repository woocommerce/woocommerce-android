package com.woocommerce.android.ui.login.storecreation.plans

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.woocommerce.android.iap.pub.IAPActivityWrapper
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.ProgressIndicator
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorScreen
import com.woocommerce.android.ui.login.storecreation.plans.BillingPeriod.ECOMMERCE_MONTHLY
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.PlanInfo
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.PlanInfo.Feature
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.CheckoutState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.ErrorState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.LoadingState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.ViewState.PlanState
import org.wordpress.android.fluxc.network.UserAgent

val LocalActivity = compositionLocalOf { AppCompatActivity() }

@Composable
fun PlanScreen(viewModel: PlansViewModel, authenticator: WPComWebViewAuthenticator, userAgent: UserAgent) {
    viewModel.viewState.observeAsState(LoadingState).value.let { state ->
        Crossfade(targetState = state) { viewState ->
            when (viewState) {
                is PlanState -> PlanInformation(
                    viewState,
                    viewModel::onExitTriggered,
                    viewModel::onConfirmClicked
                )

                is ErrorState -> StoreCreationErrorScreen(
                    viewState.errorType,
                    viewModel::onExitTriggered,
                    viewState.message
                )

                LoadingState -> ProgressIndicator()
                is CheckoutState -> WebViewPayment(
                    viewState,
                    authenticator,
                    userAgent,
                    viewModel::onPurchaseSuccess,
                    viewModel::onExitTriggered
                )
            }
        }
    }
}

@Composable
private fun PlanInformation(
    planState: PlanState,
    onExitTriggered: () -> Unit,
    onConfirmClicked: (IAPActivityWrapper) -> Unit
) {
    val currentActivity = LocalActivity.current
    val systemUiController = rememberSystemUiController()
    val wooDarkPurple = colorResource(id = R.color.woo_purple_90)
    val statusBarColor = colorResource(id = R.color.color_status_bar)
    DisposableEffect(key1 = planState) {
        systemUiController.setSystemBarsColor(
            color = wooDarkPurple,
        )
        onDispose {
            systemUiController.setSystemBarsColor(
                color = statusBarColor,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.woo_purple_90))
    ) {
        ConstraintLayout(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            val (icon, image, title, price, period, features) = createRefs()

            IconButton(
                onClick = onExitTriggered,
                modifier = Modifier.constrainAs(icon) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                }
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back),
                    tint = colorResource(id = R.color.woo_white)
                )
            }

            Image(
                painter = painterResource(id = R.drawable.img_plan),
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
                text = planState.plan.name,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.woo_white),
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
                text = planState.plan.formattedPrice,
                color = colorResource(id = R.color.woo_white),
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
                text = "/${stringResource(planState.plan.billingPeriod.nameId)}",
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
                    text = stringResource(id = R.string.store_creation_ecommerce_plan_features_tagline),
                    color = colorResource(id = R.color.woo_white),
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
                        text = stringResource(id = R.string.store_creation_ecommerce_plan_powered_by),
                        color = colorResource(id = R.color.woo_purple_dark_secondary),
                        style = MaterialTheme.typography.caption
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_wordpress),
                        contentDescription = "WordPress logo",
                        modifier = Modifier
                            .height(dimensionResource(id = R.dimen.major_110))
                            .padding(horizontal = dimensionResource(id = R.dimen.minor_50)),
                        contentScale = ContentScale.FillHeight,
                        colorFilter = ColorFilter.tint(color = colorResource(id = R.color.woo_purple_dark_secondary))
                    )
                    Text(
                        fontWeight = FontWeight.Bold,
                        text = stringResource(id = R.string.store_creation_ecommerce_plan_wordpress),
                        color = colorResource(id = R.color.woo_purple_dark_secondary),
                        style = MaterialTheme.typography.caption
                    )
                }
                planState.plan.features.forEach {
                    PlanFeatureRow(iconId = it.iconId, textId = it.textId)
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_75)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(id = R.dimen.major_125))
        ) {
            Divider(
                color = colorResource(id = R.color.woo_white_alpha_008),
                thickness = dimensionResource(id = R.dimen.minor_10),
            )

            Text(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                    .fillMaxWidth(),
                text = stringResource(id = R.string.store_creation_ecommerce_plan_refund_reminder),
                color = colorResource(id = R.color.woo_purple_dark_secondary),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            WCColoredButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
                onClick = { onConfirmClicked(IAPActivityWrapper(currentActivity)) },
                enabled = !planState.showMainButtonLoading
            ) {
                if (planState.showMainButtonLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size = dimensionResource(id = R.dimen.major_150)),
                        color = colorResource(id = R.color.color_on_primary_surface),
                    )
                } else {
                    val periodText = stringResource(id = planState.plan.billingPeriod.nameId)
                    Text(
                        text = stringResource(
                            id = R.string.store_creation_ecommerce_plan_purchase_button_title,
                            "${planState.plan.formattedPrice}/$periodText"
                        )
                    )
                }
            }
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
            color = colorResource(id = R.color.woo_white),
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
private fun WebViewPayment(
    viewState: CheckoutState,
    authenticator: WPComWebViewAuthenticator,
    userAgent: UserAgent,
    onStoreCreated: () -> Unit,
    onExitTriggered: () -> Unit
) {
    var storeCreationTriggered by remember { mutableStateOf(false) }

    WCWebView(
        url = viewState.startUrl,
        wpComAuthenticator = authenticator,
        userAgent = userAgent,
        onUrlLoaded = { url: String ->
            if (url.contains(viewState.successTriggerKeyword, ignoreCase = true) && !storeCreationTriggered) {
                storeCreationTriggered = true
                onStoreCreated()
            } else if (url == viewState.exitTriggerKeyword) {
                onExitTriggered()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Preview
@Composable
fun PreviewPlanInformation() {
    PlanInformation(
        PlanState(
            PlanInfo(
                name = "eCommerce",
                billingPeriod = ECOMMERCE_MONTHLY,
                formattedPrice = "$69.99",
                features = listOf(
                    Feature(
                        iconId = R.drawable.ic_star,
                        textId = R.string.store_creation_ecommerce_plan_feature_themes
                    ),
                    Feature(
                        iconId = R.drawable.ic_box,
                        textId = R.string.store_creation_ecommerce_plan_feature_products
                    ),
                    Feature(
                        iconId = R.drawable.ic_present,
                        textId = R.string.store_creation_ecommerce_plan_feature_subscriptions
                    ),
                    Feature(
                        iconId = R.drawable.ic_chart,
                        textId = R.string.store_creation_ecommerce_plan_feature_reports
                    ),
                    Feature(
                        iconId = R.drawable.ic_dollar,
                        textId = R.string.store_creation_ecommerce_plan_feature_payments
                    ),
                    Feature(
                        iconId = R.drawable.ic_truck,
                        textId = R.string.store_creation_ecommerce_plan_feature_shipping_labels
                    ),
                    Feature(
                        iconId = R.drawable.ic_megaphone,
                        textId = R.string.store_creation_ecommerce_plan_feature_sales
                    )
                )
            )
        ),
        { },
        { }
    )
}
