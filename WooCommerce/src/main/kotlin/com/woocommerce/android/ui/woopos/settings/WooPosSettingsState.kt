package com.woocommerce.android.ui.woopos.settings

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategory
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class WooPosSettingsState(
    val selectedCategory: WooPosSettingsCategory = WooPosSettingsCategory.STORE,
    val currentDestination: WooPosSettingsDetailDestination = selectedCategory.rootDestination,
    val dialogState: WooPosSettingsDialogState = WooPosSettingsDialogState.Hidden
) : Parcelable {
    val canGoBack: Boolean
        get() = currentDestination.parentDestination != null
}

sealed class WooPosSettingsDetailDestination : Parcelable {
    @get:StringRes
    abstract val titleRes: Int
    abstract val parentDestination: WooPosSettingsDetailDestination?
    abstract val childDestinations: List<WooPosSettingsDetailDestination>

    sealed class Hardware : WooPosSettingsDetailDestination() {
        @Parcelize
        data object Overview : Hardware() {
            @IgnoredOnParcel
            override val titleRes: Int = R.string.woopos_settings_hardware_category

            @IgnoredOnParcel
            override val parentDestination: WooPosSettingsDetailDestination? = null

            @IgnoredOnParcel
            override val childDestinations: List<Hardware> = listOf(BarcodeScanners, CardReaders)
        }

        @Parcelize
        data object BarcodeScanners : Hardware() {
            @IgnoredOnParcel
            override val titleRes: Int = R.string.woopos_settings_hardware_barcode_scanners

            @IgnoredOnParcel
            override val parentDestination: Hardware = Overview

            @IgnoredOnParcel
            override val childDestinations: List<WooPosSettingsDetailDestination> = emptyList()
        }

        @Parcelize
        data object CardReaders : Hardware() {
            @IgnoredOnParcel
            override val titleRes: Int = R.string.woopos_settings_hardware_card_readers

            @IgnoredOnParcel
            override val parentDestination: Hardware = Overview

            @IgnoredOnParcel
            override val childDestinations: List<WooPosSettingsDetailDestination> = emptyList()
        }
    }

    sealed class Store : WooPosSettingsDetailDestination() {
        @Parcelize
        data object Overview : Store() {
            @IgnoredOnParcel
            override val titleRes: Int = R.string.woopos_settings_store_category

            @IgnoredOnParcel
            override val parentDestination: WooPosSettingsDetailDestination? = null

            @IgnoredOnParcel
            override val childDestinations: List<WooPosSettingsDetailDestination> = emptyList()
        }
    }

    sealed class LocalCatalog : WooPosSettingsDetailDestination() {
        @Parcelize
        data object Overview : LocalCatalog() {
            @IgnoredOnParcel
            override val titleRes: Int = R.string.woopos_settings_local_catalog_category

            @IgnoredOnParcel
            override val parentDestination: WooPosSettingsDetailDestination? = null

            @IgnoredOnParcel
            override val childDestinations: List<WooPosSettingsDetailDestination> = emptyList()
        }
    }

    sealed class Help : WooPosSettingsDetailDestination() {
        @Parcelize
        data object Overview : Help() {
            @IgnoredOnParcel
            override val titleRes: Int = R.string.woopos_settings_help_category

            @IgnoredOnParcel
            override val parentDestination: WooPosSettingsDetailDestination? = null

            @IgnoredOnParcel
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
