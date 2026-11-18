package com.woocommerce.android.ui.woopos.settings.details.hardware

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.featureflags.WooPosReceiptPrintingEnabled
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosSettingsHardwareViewModelTest {
    private val isReceiptPrintingEnabled: WooPosReceiptPrintingEnabled = mock()

    @Test
    fun `given receipt printing enabled, when created, then receipt printers is the last item`() {
        // GIVEN
        whenever(isReceiptPrintingEnabled()).thenReturn(true)

        // WHEN
        val viewModel = WooPosSettingsHardwareViewModel(isReceiptPrintingEnabled)

        // THEN
        assertThat(viewModel.state.value.items.map { it.titleRes }).containsExactly(
            R.string.woopos_settings_hardware_card_readers,
            R.string.woopos_settings_hardware_barcode_scanners,
            R.string.woopos_settings_hardware_receipt_printers,
        )
    }

    @Test
    fun `given receipt printing disabled, when created, then there is no receipt printers item`() {
        // GIVEN
        whenever(isReceiptPrintingEnabled()).thenReturn(false)

        // WHEN
        val viewModel = WooPosSettingsHardwareViewModel(isReceiptPrintingEnabled)

        // THEN
        assertThat(viewModel.state.value.items.map { it.titleRes }).containsExactly(
            R.string.woopos_settings_hardware_card_readers,
            R.string.woopos_settings_hardware_barcode_scanners,
        )
    }
}
