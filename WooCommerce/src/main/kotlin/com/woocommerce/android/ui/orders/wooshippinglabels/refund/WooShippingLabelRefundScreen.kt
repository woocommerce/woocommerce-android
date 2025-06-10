package com.woocommerce.android.ui.orders.wooshippinglabels.refund

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringResLegacy
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun WooShippingLabelRefundScreen(viewModel: WooShippingLabelRefundViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        when (it) {
            is WooShippingLabelRefundViewModel.ViewState.Loading -> LoadingScreen()

            is WooShippingLabelRefundViewModel.ViewState.DataState -> WooShippingLabelRefundScreen(
                purchaseDate = it.purchaseDate,
                refundableAmount = it.refundableAmount,
                onRefundLabelClick = viewModel::onRefundShippingLabelButtonClicked,
            )
        }
    }
}

@Composable
fun WooShippingLabelRefundScreen(
    purchaseDate: String?,
    refundableAmount: String?,
    onRefundLabelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.woo_shipping_refund_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.color_on_surface)
            )

            Text(
                text = stringResource(R.string.woo_shipping_refund_description),
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(id = R.color.color_on_surface),
                modifier = Modifier.padding(top = 16.dp)
            )

            purchaseDate?.let {
                Text(
                    text = annotatedStringResLegacy(
                        stringResId = R.string.woo_shipping_refund_purchase_date,
                        it
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorResource(id = R.color.color_on_surface),
                )
            }

            refundableAmount?.let {
                Text(
                    text = annotatedStringResLegacy(
                        stringResId = R.string.woo_shipping_refund_amount_eligible_for_refund,
                        it
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorResource(id = R.color.color_on_surface),
                )
            }

            Text(
                text = stringResource(R.string.woo_shipping_refund_note),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = colorResource(id = R.color.color_on_surface),
            )

            Spacer(modifier = Modifier.weight(1f))

            refundableAmount?.let {
                WCColoredButton(
                    text = stringResource(R.string.shipping_label_refund_button, it),
                    onClick = onRefundLabelClick,
                    modifier = modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Scaffold { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}