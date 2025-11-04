package com.woocommerce.android.ui.woopos.settings.details.store

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItemInfo

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
            .padding(bottom = WooPosSpacing.Large.value)
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
private fun StoreInformationSection(storeInfo: WooPosSettingsStoreState.StoreInfo) {
    WooPosCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WooPosSpacing.Medium.value)
    ) {
        Column(
            modifier = Modifier.padding(WooPosSpacing.Medium.value)
        ) {
            StoreSectionTitle(R.string.woopos_settings_store_general_title)

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosSettingsDetailsMenuItemInfo(
                title = stringResource(R.string.woopos_settings_store_name_label),
                subtitle = storeInfo.storeName.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))

            WooPosSettingsDetailsMenuItemInfo(
                title = stringResource(R.string.woopos_settings_store_address_label),
                subtitle = storeInfo.address.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
            )
        }
    }
}

@Composable
private fun StoreInformationLoadingSection() {
    WooPosCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WooPosSpacing.Medium.value)
    ) {
        Column(
            modifier = Modifier.padding(WooPosSpacing.Medium.value)
        ) {
            StoreSectionTitle(R.string.woopos_settings_store_general_title)

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            SettingItemShimmer()

            HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))

            SettingItemShimmer()
        }
    }
}

@Composable
private fun ReceiptLoadingSection() {
    WooPosCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WooPosSpacing.Medium.value)
    ) {
        Column(
            modifier = Modifier.padding(WooPosSpacing.Medium.value)
        ) {
            StoreSectionTitle(R.string.woopos_settings_receipt_information_title)

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            repeat(5) { index ->
                SettingItemShimmer()
                if (index < 4) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))
                }
            }
        }
    }
}

@Composable
private fun ReceiptInformationSection(receiptInfo: WooPosSettingsStoreState.ReceiptInfo) {
    WooPosCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WooPosSpacing.Medium.value)
    ) {
        Column(
            modifier = Modifier.padding(WooPosSpacing.Medium.value)
        ) {
            StoreSectionTitle(R.string.woopos_settings_receipt_information_title)

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosSettingsDetailsMenuItemInfo(
                title = stringResource(R.string.woopos_settings_store_name_label),
                subtitle = receiptInfo.storeName.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))

            WooPosSettingsDetailsMenuItemInfo(
                title = stringResource(R.string.woopos_settings_store_physical_address_label),
                subtitle = receiptInfo.address.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))

            WooPosSettingsDetailsMenuItemInfo(
                title = stringResource(R.string.woopos_settings_store_phone_label),
                subtitle = receiptInfo.phone.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))

            WooPosSettingsDetailsMenuItemInfo(
                title = stringResource(R.string.woopos_settings_store_email_label),
                subtitle = receiptInfo.email.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))

            WooPosSettingsDetailsMenuItemInfo(
                title = stringResource(R.string.woopos_settings_refund_policy_label),
                subtitle = receiptInfo.refundPolicy.ifBlank { stringResource(R.string.woopos_settings_store_not_set) }
            )
        }
    }
}

@Composable
private fun StoreSectionTitle(title: Int) {
    WooPosText(
        text = stringResource(title),
        style = WooPosTypography.BodyLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SettingItemShimmer() {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        WooPosShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .height(22.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))

        WooPosShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.25f)
                .height(22.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
        )
    }
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
            storeInfoState = WooPosSettingsStoreState.StoreState.Loading,
            receiptState = WooPosSettingsStoreState.ReceiptState.Loading
        )

        WooPosSettingsStoreScreen(state = state)
    }
}
