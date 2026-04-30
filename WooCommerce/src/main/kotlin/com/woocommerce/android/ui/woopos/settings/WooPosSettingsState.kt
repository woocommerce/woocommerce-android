package com.woocommerce.android.ui.woopos.settings

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategory
import kotlinx.parcelize.Parcelize

data class WooPosSettingsState(
    val selectedCategory: WooPosSettingsCategory = WooPosSettingsCategory.STORE,
    val currentDestination: WooPosSettingsDetailDestination = selectedCategory.rootDestination,
    val dialogState: WooPosSettingsDialogState = WooPosSettingsDialogState.Hidden,
    val isDetailVisible: Boolean = false,
) {
    val canGoBack: Boolean
        get() = currentDestination.parentDestination != null || isDetailVisible
}

sealed class WooPosSettingsDetailDestination {
    @get:StringRes
    abstract val titleRes: Int
    abstract val parentDestination: WooPosSettingsDetailDestination?
    abstract val childDestinations: List<WooPosSettingsDetailDestination>

    sealed class Hardware : WooPosSettingsDetailDestination() {
        data object Overview : Hardware() {
            override val titleRes: Int = R.string.woopos_settings_hardware_category
            override val parentDestination: WooPosSettingsDetailDestination? = null
            override val childDestinations: List<Hardware> = listOf(BarcodeScanners, CardReaders)
        }

        data object BarcodeScanners : Hardware() {
            override val titleRes: Int = R.string.woopos_settings_hardware_barcode_scanners
            override val parentDestination: Hardware = Overview
            override val childDestinations: List<WooPosSettingsDetailDestination> = emptyList()
        }

        data object CardReaders : Hardware() {
            override val titleRes: Int = R.string.woopos_settings_hardware_card_readers
            override val parentDestination: Hardware = Overview
            override val childDestinations: List<WooPosSettingsDetailDestination> = emptyList()
        }
    }

    sealed class Store : WooPosSettingsDetailDestination() {
        data object Overview : Store() {
            override val titleRes: Int = R.string.woopos_settings_store_category
            override val parentDestination: WooPosSettingsDetailDestination? = null
            override val childDestinations: List<WooPosSettingsDetailDestination> = emptyList()
        }
    }

    sealed class LocalCatalog : WooPosSettingsDetailDestination() {
        data object Overview : LocalCatalog() {
            override val titleRes: Int = R.string.woopos_settings_local_catalog_category
            override val parentDestination: WooPosSettingsDetailDestination? = null
            override val childDestinations: List<WooPosSettingsDetailDestination> = emptyList()
        }
    }

    sealed class Help : WooPosSettingsDetailDestination() {
        data object Overview : Help() {
            override val titleRes: Int = R.string.woopos_settings_help_category
            override val parentDestination: WooPosSettingsDetailDestination? = null
            override val childDestinations: List<WooPosSettingsDetailDestination> = emptyList()
        }
    }
}

@Parcelize
sealed class WooPosSettingsDialogState : Parcelable {
    @Parcelize
    data object Hidden : WooPosSettingsDialogState()

    @Parcelize
    data object ProductsInfoDialog : WooPosSettingsDialogState()

    @Parcelize
    data object ScanningSetupDialog : WooPosSettingsDialogState()

    @Parcelize
    data class SyncErrorDialog(val errorMessage: String) : WooPosSettingsDialogState()

    @Parcelize
    data object CardReaderConnectionDialog : WooPosSettingsDialogState()

    @Parcelize
    data object CardReaderUpdateDialog : WooPosSettingsDialogState()
}
