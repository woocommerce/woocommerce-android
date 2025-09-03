package com.woocommerce.android.ui.woopos.settings.details.store

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
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
    WooPosSettingsStoreScreen(
        state = state,
        modifier = modifier
    )
}

@Composable
private fun WooPosSettingsStoreScreen(
    state: WooPosSettingsStoreState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = WooPosSpacing.Medium.value)
    ) {
        when (val storeState = state.storeInfoState) {
            is WooPosSettingsStoreState.StoreState.Loading -> {
                StoreInformationLoadingSection()
            }

            is WooPosSettingsStoreState.StoreState.Loaded -> {
                StoreInformationSection(storeState.storeInfo)
            }
        }

        when (val receiptState = state.receiptState) {
            is WooPosSettingsStoreState.ReceiptState.NotSupported,
            is WooPosSettingsStoreState.ReceiptState.Error -> {
            }

            is WooPosSettingsStoreState.ReceiptState.Loading -> {
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                ReceiptLoadingSection()
            }

            is WooPosSettingsStoreState.ReceiptState.Success -> {
                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                ReceiptInformationSection(receiptState.receiptInfo)
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
private fun StoreInformationLoadingSection() {
    // Store info loads so fast from cache that we can just show empty state briefly
}

@Composable
private fun StoreInformationSection(storeInfo: WooPosSettingsStoreState.StoreInfo) {
    SectionTitle(stringResource(R.string.woopos_settings_store_information_title))

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Store,
        title = stringResource(R.string.woopos_settings_store_name_label),
        subtitle = storeInfo.storeName.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
    )

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Home,
        title = stringResource(R.string.woopos_settings_store_address_label),
        subtitle = storeInfo.address.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
    )
}

@Composable
private fun ReceiptLoadingSection() {
    SectionTitle(stringResource(R.string.woopos_settings_receipt_information_title))

    repeat(5) {
        ReceiptMenuItemShimmer()
    }
}

@Composable
private fun ReceiptMenuItemShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WooPosSpacing.Large.value),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WooPosShimmerBox(
            modifier = Modifier.size(28.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = WooPosSpacing.Medium.value)
        ) {
            WooPosShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(24.dp)
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

            WooPosShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
            )
        }
    }
}

@Composable
private fun ReceiptInformationSection(receiptInfo: WooPosSettingsStoreState.ReceiptInfo) {
    SectionTitle(stringResource(R.string.woopos_settings_receipt_information_title))

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Store,
        title = stringResource(R.string.woopos_settings_store_name_label),
        subtitle = receiptInfo.storeName.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
    )

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Home,
        title = stringResource(R.string.woopos_settings_store_address_label),
        subtitle = receiptInfo.address.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
    )

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Phone,
        title = stringResource(R.string.woopos_settings_store_phone_label),
        subtitle = receiptInfo.phone.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
    )

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Email,
        title = stringResource(R.string.woopos_settings_store_email_label),
        subtitle = receiptInfo.email.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
    )

    WooPosSettingsDetailsMenuItem(
        icon = Icons.Default.Receipt,
        title = stringResource(R.string.woopos_settings_refund_policy_label),
        subtitle = receiptInfo.refundPolicy.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
    )
}

@WooPosPreview
@Composable
fun WooPosSettingsStoreScreenPreview() {
    WooPosTheme {
        val state = WooPosSettingsStoreState(
            storeInfoState = WooPosSettingsStoreState.StoreState.Loaded(
                WooPosSettingsStoreState.StoreInfo(
                    storeName = "My WooCommerce Store",
                    address = "123 Main Street, City, State 12345, US"
                )
            ),
            receiptState = WooPosSettingsStoreState.ReceiptState.Success(
                WooPosSettingsStoreState.ReceiptInfo(
                    storeName = "My WooCommerce Store",
                    address = "123 Main Street, City, State 12345, US",
                    phone = "+1 555 1234 1234",
                    email = "store@example.com",
                    refundPolicy = "Returns accepted within 30 days"
                )
            )
        )

        WooPosSettingsStoreScreen(state = state)
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsStoreScreenLoadingPreview() {
    WooPosTheme {
        val state = WooPosSettingsStoreState(
            storeInfoState = WooPosSettingsStoreState.StoreState.Loaded(
                WooPosSettingsStoreState.StoreInfo(
                    storeName = "My WooCommerce Store",
                    address = "123 Main Street, City, State 12345, US"
                )
            ),
            receiptState = WooPosSettingsStoreState.ReceiptState.Loading
        )

        WooPosSettingsStoreScreen(state = state)
    }
}
