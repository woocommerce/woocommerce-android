package com.woocommerce.android.ui.orders.wooshippinglabels.payment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun WooShippingEditPaymentScreen(
    viewModel: WooShippingEditPaymentViewModel
) {
    viewModel.viewState.observeAsState().value?.let {
        WooShippingEditPaymentScreen(
            viewState = it,
            onAddNewPaymentMethod = viewModel::onAddNewPaymentMethod
        )
    }
}

@Composable
private fun WooShippingEditPaymentScreen(
    viewState: WooShippingEditPaymentViewModel.ViewState,
    onAddNewPaymentMethod: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = dimensionResource(id = R.dimen.minor_100),
            topEnd = dimensionResource(id = R.dimen.minor_100)
        )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            BottomSheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))

            Text(
                text = stringResource(R.string.woo_shipping_payment_screen_title),
                style = MaterialTheme.typography.h6
            )
            Text(
                text = stringResource(R.string.woo_shipping_payment_screen_description),
                style = MaterialTheme.typography.body1
            )

            when (viewState) {
                is WooShippingEditPaymentViewModel.ViewState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .height(200.dp)
                            .wrapContentSize(align = Alignment.Center)
                    )
                }

                is WooShippingEditPaymentViewModel.ViewState.Content -> {
                    WooShippingEditPaymentContent(
                        viewState = viewState,
                        onAddNewPaymentMethod = onAddNewPaymentMethod,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun WooShippingEditPaymentContent(
    viewState: WooShippingEditPaymentViewModel.ViewState.Content,
    onAddNewPaymentMethod: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .padding(vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (!viewState.editEnabled) {
            EditDisabledWarning(
                storeOwnerName = viewState.storeOwnerName,
                storeOwnerUsername = viewState.storeOwnerUsername
            )
            Spacer(Modifier.height(16.dp))
        }

        when {
            viewState.paymentMethods.isEmpty() -> {
                EmptyPaymentMethodsView(
                    editEnabled = viewState.editEnabled,
                    storeOwnerUsername = viewState.storeOwnerUsername,
                    onAddNewPaymentMethod = onAddNewPaymentMethod,
                )
            }

            else -> {
                TODO()
            }
        }
    }
}

@Composable
fun EmptyPaymentMethodsView(
    editEnabled: Boolean,
    storeOwnerUsername: String,
    onAddNewPaymentMethod: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dividerColor = colorResource(R.color.divider_color)
    val cornerRadius = MaterialTheme.shapes.medium.topStart.toPx(
        shapeSize = Size.Unspecified,
        density = LocalDensity.current
    )
    val borderWidth = with(LocalDensity.current) { 1.dp.toPx() }

    Column(modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val stroke = Stroke(
                        width = borderWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    drawRoundRect(
                        color = dividerColor,
                        style = stroke,
                        cornerRadius = CornerRadius(cornerRadius)
                    )
                }
        ) {
            Spacer(Modifier.height(48.dp))
            Image(
                painterResource(R.drawable.ic_credit_card_grey),
                contentDescription = null
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.woo_shipping_payment_no_payment_methods_title),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.woo_shipping_payment_no_payment_methods_description),
                style = MaterialTheme.typography.body2,
                color = LocalContentColor.current.copy(alpha = ContentAlpha.medium)
            )
            Spacer(Modifier.height(16.dp))
            WCColoredButton(
                onClick = onAddNewPaymentMethod,
                enabled = editEnabled,
                text = stringResource(R.string.woo_shipping_payment_add_new_button),
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.ic_external),
                        tint = LocalContentColor.current,
                        contentDescription = null
                    )
                }
            )
            Spacer(Modifier.height(48.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.woo_shipping_payment_methods_info_footer, storeOwnerUsername),
            style = MaterialTheme.typography.caption,
            color = LocalContentColor.current.copy(alpha = ContentAlpha.medium)
        )
    }
}

@Composable
private fun EditDisabledWarning(
    storeOwnerName: String,
    storeOwnerUsername: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .background(colorResource(R.color.warning_banner_background_color), shape = MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            tint = colorResource(R.color.warning_banner_foreground_color),
            contentDescription = null
        )
        Text(
            text = stringResource(
                R.string.woo_shipping_payment_edit_disabled_warning,
                storeOwnerName,
                storeOwnerUsername
            ),
            style = MaterialTheme.typography.body2,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun LoadingScreenPreview() {
    WooThemeWithBackground {
        WooShippingEditPaymentScreen(
            viewState = WooShippingEditPaymentViewModel.ViewState.Loading,
            onAddNewPaymentMethod = {}
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun ContentScreenPreview() {
    WooThemeWithBackground {
        WooShippingEditPaymentScreen(
            viewState = WooShippingEditPaymentViewModel.ViewState.Content(
                editEnabled = false,
                emailTheReceipt = true,
                storeOwnerName = "John Doe",
                storeOwnerUsername = "johndoe",
                paymentMethods = emptyList(),
                selectedPaymentMethodId = null
            ),
            onAddNewPaymentMethod = {}
        )
    }
}
