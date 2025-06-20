package com.woocommerce.android.ui.orders.wooshippinglabels.payment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.woocommerce.android.ui.compose.Render
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCSwitch
import com.woocommerce.android.ui.compose.component.web.WCWebView
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippingLabelSampleData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodModel

@Composable
fun WooShippingEditPaymentScreen(
    viewModel: WooShippingEditPaymentViewModel
) {
    viewModel.viewState.observeAsState().value?.let {
        WooShippingEditPaymentScreen(
            viewState = it,
            onAddNewPaymentMethod = viewModel::onAddNewPaymentMethod,
            onPaymentMethodSelected = viewModel::onPaymentMethodSelected,
            onSaveClicked = viewModel::onSaveClicked
        )
    }
}

@Composable
private fun WooShippingEditPaymentScreen(
    viewState: WooShippingEditPaymentViewModel.ViewState,
    onAddNewPaymentMethod: () -> Unit,
    onPaymentMethodSelected: (Int) -> Unit,
    onSaveClicked: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = dimensionResource(id = R.dimen.minor_100),
            topEnd = dimensionResource(id = R.dimen.minor_100)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            when (viewState) {
                is WooShippingEditPaymentViewModel.ViewState.Loading -> {
                    LoadingScreen(
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                is WooShippingEditPaymentViewModel.ViewState.Content -> {
                    WooShippingEditPaymentContent(
                        viewState = viewState,
                        onAddNewPaymentClicked = onAddNewPaymentMethod,
                        onSaveClicked = onSaveClicked,
                        onPaymentMethodSelected = onPaymentMethodSelected
                    )
                }

                is WooShippingEditPaymentViewModel.ViewState.AddPaymentMethodWebView -> {
                    WooShippingEditPaymentWebView(
                        viewState = viewState,
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleSection(
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.woo_shipping_payment_screen_title),
            style = MaterialTheme.typography.h6
        )
        Text(
            text = stringResource(R.string.woo_shipping_payment_screen_description),
            style = MaterialTheme.typography.body1
        )
    }
}

@Composable
private fun LoadingScreen(
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(16.dp)
    ) {
        BottomSheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(Modifier)

        TitleSection()

        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(200.dp)
                .wrapContentSize(align = Alignment.Center)
        )
    }
}

@Composable
private fun WooShippingEditPaymentContent(
    viewState: WooShippingEditPaymentViewModel.ViewState.Content,
    onAddNewPaymentClicked: () -> Unit,
    onPaymentMethodSelected: (Int) -> Unit,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BottomSheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(modifier = Modifier.height(16.dp))

        if (!viewState.canManagePaymentMethods) {
            EditDisabledWarning(
                storeOwnerName = viewState.storeOwnerName,
                storeOwnerUsername = viewState.storeOwnerUsername
            )
            Spacer(Modifier.height(16.dp))
        }

        when {
            viewState.paymentMethods.isEmpty() -> {
                EmptyPaymentMethodsView(
                    editEnabled = viewState.canManagePaymentMethods,
                    storeOwnerName = viewState.storeOwnerName,
                    storeOwnerUsername = viewState.storeOwnerUsername,
                    loadingAddedPaymentMethod = viewState.loadingState ==
                        WooShippingEditPaymentViewModel.LoadingState.LoadingAddedPaymentMethod,
                    onAddNewPaymentMethod = onAddNewPaymentClicked,
                )
            }

            else -> {
                PaymentMethodsList(
                    viewState = viewState,
                    onAddNewPaymentClicked = onAddNewPaymentClicked,
                    onPaymentMethodSelected = onPaymentMethodSelected,
                    onSaveClicked = onSaveClicked
                )
            }
        }
    }

    viewState.dialogState?.let {
        it.Render()
    }
}

@Composable
private fun WooShippingEditPaymentWebView(
    viewState: WooShippingEditPaymentViewModel.ViewState.AddPaymentMethodWebView,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        IconButton(
            onClick = viewState.onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(R.string.dismiss),
                tint = MaterialTheme.colors.onSurface
            )
        }

        WCWebView(
            url = viewState.url,
            userAgent = viewState.userAgent,
            onUrlLoaded = viewState.onUrlLoaded,
            authenticator = viewState.authenticator
        )
    }
}

@Composable
fun EmptyPaymentMethodsView(
    editEnabled: Boolean,
    storeOwnerName: String,
    storeOwnerUsername: String,
    loadingAddedPaymentMethod: Boolean,
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
                loading = loadingAddedPaymentMethod,
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
            text = stringResource(
                R.string.woo_shipping_payment_methods_info_footer,
                storeOwnerName,
                storeOwnerUsername
            ),
            style = MaterialTheme.typography.caption,
            color = LocalContentColor.current.copy(alpha = ContentAlpha.medium)
        )
    }
}

@Composable
private fun PaymentMethodsList(
    viewState: WooShippingEditPaymentViewModel.ViewState.Content,
    onAddNewPaymentClicked: () -> Unit,
    onPaymentMethodSelected: (Int) -> Unit,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        viewState.paymentMethods.forEach { paymentMethod ->
            PaymentMethodItem(
                paymentMethod = paymentMethod,
                isSelected = paymentMethod.paymentMethodId == viewState.selectedPaymentMethodId,
                enabled = viewState.canManagePaymentMethods,
                onClick = { onPaymentMethodSelected(paymentMethod.paymentMethodId) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
        WCOutlinedButton(
            onClick = onAddNewPaymentClicked,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.onSurface),
            enabled = viewState.canManagePaymentMethods,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (viewState.loadingState == WooShippingEditPaymentViewModel.LoadingState.LoadingAddedPaymentMethod) {
                CircularProgressIndicator(Modifier.size(24.dp))
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        tint = if (viewState.canManagePaymentMethods) {
                            MaterialTheme.colors.primary
                        } else {
                            LocalContentColor.current.copy(LocalContentAlpha.current)
                        },
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(R.string.woo_shipping_payment_add_new_button)
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Info, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    R.string.woo_shipping_payment_methods_info_footer,
                    viewState.storeOwnerName,
                    viewState.storeOwnerUsername
                ),
                style = MaterialTheme.typography.caption
            )
        }
        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(16.dp))
        WCSwitch(
            text = stringResource(R.string.woo_shipping_payment_email_receipt_toggle),
            checked = viewState.emailReceipts,
            enabled = viewState.canEditSettings,
            onCheckedChange = viewState.onEmailReceiptsChanged,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        WCColoredButton(
            onClick = onSaveClicked,
            text = stringResource(
                if (viewState.canManagePaymentMethods) {
                    R.string.woo_shipping_payment_use_card_button
                } else {
                    R.string.save
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PaymentMethodItem(
    paymentMethod: PaymentMethodModel,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val borderColor = if (enabled && isSelected) MaterialTheme.colors.primary else colorResource(R.color.divider_color)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val backgroundColor = when {
        enabled && isSelected -> colorResource(R.color.woo_item_selected)
        isSelected -> colorResource(R.color.woo_item_selected_disabled)
        else -> Color.Transparent
    }
    CompositionLocalProvider(
        LocalContentAlpha provides if (enabled) LocalContentAlpha.current else ContentAlpha.disabled
    ) {
        Column(
            modifier
                .border(color = borderColor, width = borderWidth, shape = MaterialTheme.shapes.medium)
                .background(color = backgroundColor, shape = MaterialTheme.shapes.medium)
                .clickable(onClick = onClick, enabled = enabled)
                .padding(16.dp)
        ) {
            Text(
                text = paymentMethod.cardTypeWithDigits,
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                text = paymentMethod.name,
                style = MaterialTheme.typography.body2,
            )
            Text(
                text = paymentMethod.expiry,
                style = MaterialTheme.typography.body2,
            )
        }
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
            onAddNewPaymentMethod = {},
            onPaymentMethodSelected = {},
            onSaveClicked = {}
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun ContentScreenEmptyPreview() {
    WooThemeWithBackground {
        WooShippingEditPaymentScreen(
            viewState = WooShippingEditPaymentViewModel.ViewState.Content(
                canManagePaymentMethods = false,
                canEditSettings = true,
                emailReceipts = true,
                storeOwnerName = "John Doe",
                storeOwnerUsername = "johndoe",
                selectedPaymentMethodId = null,
                currentPaymentOptions = ShippingLabelSampleData.getPaymentOptions(countOfPaymentMethods = 0),
                onEmailReceiptsChanged = {}
            ),
            onAddNewPaymentMethod = {},
            onPaymentMethodSelected = {},
            onSaveClicked = {}
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun ContentScreenWithPaymentMethodsPreview() {
    WooThemeWithBackground {
        WooShippingEditPaymentScreen(
            viewState = WooShippingEditPaymentViewModel.ViewState.Content(
                canManagePaymentMethods = true,
                canEditSettings = true,
                emailReceipts = true,
                storeOwnerName = "John Doe",
                storeOwnerUsername = "johndoe",
                selectedPaymentMethodId = 1,
                currentPaymentOptions = ShippingLabelSampleData.getPaymentOptions(),
                onEmailReceiptsChanged = {}
            ),
            onAddNewPaymentMethod = {},
            onPaymentMethodSelected = {},
            onSaveClicked = {}
        )
    }
}
