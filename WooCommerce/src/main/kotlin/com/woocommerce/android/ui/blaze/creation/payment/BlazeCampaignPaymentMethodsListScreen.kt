package com.woocommerce.android.ui.blaze.creation.payment

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.model.CreditCardType
import com.woocommerce.android.ui.blaze.BlazeRepository.PaymentMethod
import com.woocommerce.android.ui.blaze.BlazeRepository.PaymentMethodUrls
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BlazeCampaignPaymentMethodsListScreen(viewModel: BlazeCampaignPaymentMethodsListViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        BlazeCampaignPaymentMethodsListScreen(
            viewState = it
        )
    }
}

@Composable
private fun BlazeCampaignPaymentMethodsListScreen(viewState: BlazeCampaignPaymentMethodsListViewModel.ViewState) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.blaze_campaign_payment_list_screen_title),
                onNavigationButtonClick = viewState.onDismiss,
                navigationIcon = when (viewState) {
                    is BlazeCampaignPaymentMethodsListViewModel.ViewState.PaymentMethodsList -> Icons.Default.ArrowBack
                    is BlazeCampaignPaymentMethodsListViewModel.ViewState.AddPaymentMethodWebView -> Icons.Default.Clear
                }
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        when (viewState) {
            is BlazeCampaignPaymentMethodsListViewModel.ViewState.PaymentMethodsList -> {
                PaymentMethodsList(
                    paymentMethods = viewState.paymentMethods,
                    onPaymentMethodClicked = viewState.onPaymentMethodClicked,
                    onAddPaymentMethodClicked = viewState.onAddPaymentMethodClicked,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is BlazeCampaignPaymentMethodsListViewModel.ViewState.AddPaymentMethodWebView -> {
                AddPaymentMethodWebView(
                    urls = viewState.urls,
                    onUrlLoaded = viewState.onUrlLoaded,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun PaymentMethodsList(
    paymentMethods: List<PaymentMethod>,
    onPaymentMethodClicked: (PaymentMethod) -> Unit,
    onAddPaymentMethodClicked: () -> Unit,
    modifier: Modifier
) {
    Column(modifier) {
        PaymentMethodsHeader(Modifier.fillMaxWidth())
        if (paymentMethods.isEmpty()) {
            EmptyPaymentMethodsView(
                onAddPaymentMethodClicked = onAddPaymentMethodClicked,
                modifier = modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            PaymentMethodsListView(
                paymentMethods = paymentMethods,
                onPaymentMethodClicked = onPaymentMethodClicked,
                onAddPaymentMethodClicked = onAddPaymentMethodClicked,
                modifier = modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun PaymentMethodsHeader(modifier: Modifier = Modifier) {
    Row(modifier.padding(dimensionResource(id = R.dimen.major_100))) {
        Icon(
            imageVector = Icons.Outlined.VerifiedUser,
            tint = MaterialTheme.colors.primary,
            contentDescription = null
        )
        Spacer(modifier = Modifier.padding(dimensionResource(id = R.dimen.minor_100)))
        Text(text = stringResource(id = R.string.blaze_campaign_payment_list_header_text))
    }
}

@Composable
private fun PaymentMethodsListView(
    paymentMethods: List<PaymentMethod>,
    onPaymentMethodClicked: (PaymentMethod) -> Unit,
    onAddPaymentMethodClicked: () -> Unit,
    modifier: Modifier
) {
    LazyColumn(modifier = modifier) {
        items(paymentMethods) { paymentMethod ->
            Column {
                PaymentMethodItem(
                    paymentMethod = paymentMethod,
                    onPaymentMethodClicked = { onPaymentMethodClicked(paymentMethod) },
                    isSelected = true
                )
            }
            Divider()
        }

        item {
            Text(
                text = stringResource(id = R.string.blaze_campaign_payment_list_hint, "username", "email"),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(
                    alpha = ContentAlpha.medium
                ),
                modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
            )
        }

        item {
            WCTextButton(
                onClick = onAddPaymentMethodClicked,
                icon = Icons.Default.Add,
                text = stringResource(id = R.string.blaze_campaign_payment_list_add_new_payment_method_button)
            )
        }
    }
}

@Composable
private fun PaymentMethodItem(
    paymentMethod: PaymentMethod,
    onPaymentMethodClicked: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onPaymentMethodClicked)
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = paymentMethod.name,
                style = MaterialTheme.typography.subtitle1
            )
            paymentMethod.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(
                        alpha = ContentAlpha.medium
                    )
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                tint = MaterialTheme.colors.primary,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun EmptyPaymentMethodsView(
    onAddPaymentMethodClicked: () -> Unit,
    modifier: Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(
            horizontal = dimensionResource(id = R.dimen.major_100),
            vertical = dimensionResource(id = R.dimen.major_200)
        )
    ) {
        Text(text = stringResource(id = R.string.blaze_campaign_payment_list_empty_state_text))
        Spacer(modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)))
        WCColoredButton(
            onClick = onAddPaymentMethodClicked,
            text = stringResource(id = R.string.blaze_campaign_payment_list_add_payment_method_button)
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun AddPaymentMethodWebView(
    urls: PaymentMethodUrls,
    onUrlLoaded: (String) -> Unit,
    modifier: Modifier
) {
    TODO("Not yet implemented")
}

@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun EmptyPaymentMethodsListPreview() {
    WooThemeWithBackground {
        BlazeCampaignPaymentMethodsListScreen(
            viewState = BlazeCampaignPaymentMethodsListViewModel.ViewState.PaymentMethodsList(
                paymentMethods = emptyList(),
                onPaymentMethodClicked = {},
                onAddPaymentMethodClicked = {},
                onDismiss = {}
            )
        )
    }
}

@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun PaymentMethodsListPreview() {
    WooThemeWithBackground {
        BlazeCampaignPaymentMethodsListScreen(
            viewState = BlazeCampaignPaymentMethodsListViewModel.ViewState.PaymentMethodsList(
                paymentMethods = listOf(
                    PaymentMethod(
                        id = "1",
                        name = "Visa",
                        subtitle = "John Doe",
                        type = PaymentMethod.PaymentMethodType.CreditCard(CreditCardType.VISA)
                    ),
                    PaymentMethod(
                        id = "2",
                        name = "MasterCard",
                        subtitle = "John Doe",
                        type = PaymentMethod.PaymentMethodType.CreditCard(CreditCardType.MASTERCARD)
                    )
                ),
                onPaymentMethodClicked = {},
                onAddPaymentMethodClicked = {},
                onDismiss = {}
            )
        )
    }
}
