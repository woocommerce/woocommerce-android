package com.woocommerce.android.ui.woopos.settings.details.store

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItem

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
                WooPosCircularLoadingIndicator()
            }
        }
        is WooPosSettingsStoreState.Loaded -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = WooPosSpacing.Medium.value)
            ) {
                StoreInformationSection(currentState.storeInfo)

                when (currentState.receiptState) {
                    is WooPosSettingsStoreState.ReceiptState.NotSupported -> {
                        // Don't show receipt section
                    }
                    is WooPosSettingsStoreState.ReceiptState.Loading -> {
                        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                        ReceiptLoadingSection()
                    }
                    is WooPosSettingsStoreState.ReceiptState.Success -> {
                        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                        ReceiptInformationSection(currentState.receiptState.receiptInfo)
                    }
                    is WooPosSettingsStoreState.ReceiptState.Error -> {
                        // Could show error state or just omit the section
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    WooPosText(
        text = title,
        style = WooPosTypography.BodyXLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = WooPosSpacing.Large.value)
    )
}

@Composable
private fun StoreInformationSection(storeInfo: WooPosSettingsStoreState.StoreInfo) {
    SectionTitle(stringResource(R.string.woopos_settings_store_information_title))

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Store,
        title = stringResource(R.string.woopos_settings_store_name_label),
        subtitle = storeInfo.storeName.ifBlank { stringResource(R.string.woopos_settings_store_not_set) },
        onClick = { }
    )

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Home,
        title = stringResource(R.string.woopos_settings_store_address_label),
        subtitle = storeInfo.address.ifBlank { stringResource(R.string.woopos_settings_store_not_set) },
        onClick = { }
    )

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Phone,
        title = stringResource(R.string.woopos_settings_store_phone_label),
        subtitle = storeInfo.phone.ifBlank { stringResource(R.string.woopos_settings_store_not_set) },
        onClick = { }
    )

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Email,
        title = stringResource(R.string.woopos_settings_store_email_label),
        subtitle = storeInfo.email.ifBlank { stringResource(R.string.woopos_settings_store_not_set) },
        onClick = { }
    )
}

@Composable
private fun ReceiptLoadingSection() {
    SectionTitle(stringResource(R.string.woopos_settings_receipt_information_title))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Large.value),
        contentAlignment = Alignment.Center
    ) {
        WooPosCircularLoadingIndicator()
    }
}

@Composable
private fun ReceiptInformationSection(receiptInfo: WooPosSettingsStoreState.ReceiptInfo) {
    SectionTitle(stringResource(R.string.woopos_settings_receipt_information_title))

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Store,
        title = stringResource(R.string.woopos_settings_store_name_label),
        subtitle = receiptInfo.storeName.ifBlank { stringResource(R.string.woopos_settings_store_not_set) },
        onClick = { }
    )

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Home,
        title = stringResource(R.string.woopos_settings_store_address_label),
        subtitle = receiptInfo.address.ifBlank { stringResource(R.string.woopos_settings_store_not_set) },
        onClick = { }
    )

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Phone,
        title = stringResource(R.string.woopos_settings_store_phone_label),
        subtitle = receiptInfo.phone.ifBlank { stringResource(R.string.woopos_settings_store_not_set) },
        onClick = { }
    )

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Email,
        title = stringResource(R.string.woopos_settings_store_email_label),
        subtitle = receiptInfo.email.ifBlank { stringResource(R.string.woopos_settings_store_not_set) },
        onClick = { }
    )

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Receipt,
        title = stringResource(R.string.woopos_settings_refund_policy_label),
        subtitle = receiptInfo.refundPolicy.ifBlank { stringResource(R.string.woopos_settings_store_not_set) },
        onClick = { }
    )
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
            modifier = Modifier.fillMaxSize()
        ) {
            StoreInformationSection(storeInfo)
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
            ReceiptInformationSection(receiptInfo)
        }
    }
}
