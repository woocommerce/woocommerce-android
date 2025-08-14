package com.woocommerce.android.ui.woopos.settings.details.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosSettingsStoreScreen(
    modifier: Modifier = Modifier,
    viewModel: WooPosSettingsStoreViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    when (val currentState = state) {
        is WooPosSettingsStoreState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is WooPosSettingsStoreState.Loaded -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WooPosSpacing.Medium.value)
            ) {
                StoreInformationSection(currentState.storeInfo)

                currentState.receiptInfo?.let { receiptInfo ->
                    Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
                    ReceiptInformationSection(receiptInfo)
                }
            }
        }
    }
}

@Composable
private fun StoreInformationSection(storeInfo: WooPosSettingsStoreState.StoreInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
            )
            .padding(WooPosSpacing.Medium.value)
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_settings_store_information_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        InfoRow(
            label = stringResource(R.string.woopos_settings_store_name_label),
            value = storeInfo.storeName
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = WooPosSpacing.Small.value),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        InfoRow(
            label = stringResource(R.string.woopos_settings_store_address_label),
            value = storeInfo.address.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = WooPosSpacing.Small.value),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        InfoRow(
            label = stringResource(R.string.woopos_settings_store_phone_label),
            value = storeInfo.phone.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = WooPosSpacing.Small.value),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        InfoRow(
            label = stringResource(R.string.woopos_settings_store_email_label),
            value = storeInfo.email
        )
    }
}

@Composable
private fun ReceiptInformationSection(receiptInfo: WooPosSettingsStoreState.ReceiptInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
            )
            .padding(WooPosSpacing.Medium.value)
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_settings_receipt_information_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        InfoRow(
            label = stringResource(R.string.woopos_settings_store_name_label),
            value = receiptInfo.storeName
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = WooPosSpacing.Small.value),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        InfoRow(
            label = stringResource(R.string.woopos_settings_store_address_label),
            value = receiptInfo.address.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = WooPosSpacing.Small.value),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        InfoRow(
            label = stringResource(R.string.woopos_settings_store_phone_label),
            value = receiptInfo.phone.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = WooPosSpacing.Small.value),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        InfoRow(
            label = stringResource(R.string.woopos_settings_store_email_label),
            value = receiptInfo.email
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = WooPosSpacing.Small.value),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        InfoRow(
            label = stringResource(R.string.woopos_settings_refund_policy_label),
            value = receiptInfo.refundPolicy.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WooPosSpacing.XSmall.value),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        WooPosText(
            text = label,
            style = WooPosTypography.BodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.35f)
        )
        WooPosText(
            text = value,
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.65f)
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsStoreScreenPreview() {
    WooPosTheme {
        val storeInfo = WooPosSettingsStoreState.StoreInfo(
            storeName = "My WooCommerce Store",
            address = "123 Main Street, City, State 12345, US",
            phone = "+1 555 1234 1234",
            email = "myemail@something.com"
        )

        val receiptInfo = WooPosSettingsStoreState.ReceiptInfo(
            storeName = "My WooCommerce Store",
            address = "123 Main Street, City, State 12345, US",
            phone = "+1 555 1234 1234",
            email = "myemail@something.com",
            refundPolicy = "Returns accepted within 30 days"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WooPosSpacing.Medium.value)
        ) {
            StoreInformationSection(storeInfo)
            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))
            ReceiptInformationSection(receiptInfo)
        }
    }
}
