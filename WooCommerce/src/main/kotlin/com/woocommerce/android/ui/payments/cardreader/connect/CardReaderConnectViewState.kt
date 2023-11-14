package com.woocommerce.android.ui.payments.cardreader.connect

import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString

sealed interface ScanningState
sealed interface ConnectingState

@Suppress("LongParameterList")
sealed class CardReaderConnectViewState(
    val headerLabel: UiString? = null,
    @DrawableRes val illustration: Int? = null,
    @StringRes val hintLabel: Int? = null,
    val primaryActionLabel: Int? = null,
    val secondaryActionLabel: Int? = null,
    val tertiaryActionLabel: Int? = null,
    @DimenRes val illustrationTopMargin: Int = R.dimen.major_200,
    open val listItems: List<CardReaderConnectViewModel.ListItemViewState>? = null,
    val learnMoreLabel: UiString? = null,
) {
    open val onPrimaryActionClicked: (() -> Unit)? = null
    open val onSecondaryActionClicked: (() -> Unit)? = null
    open val onTertiaryActionClicked: (() -> Unit)? = null
    open val onLearnMoreClicked: (() -> Unit)? = null

    data class ExternalReaderScanningState(
        override val onSecondaryActionClicked: (() -> Unit),
        override val onLearnMoreClicked: (() -> Unit),
    ) : ScanningState,
        CardReaderConnectViewState(
            headerLabel = UiString.UiStringRes(R.string.card_reader_connect_scanning_header),
            illustration = R.drawable.img_card_reader_scanning,
            hintLabel = R.string.card_reader_connect_scanning_hint,
            secondaryActionLabel = R.string.cancel,
            learnMoreLabel = UiString.UiStringRes(
                R.string.card_reader_connect_learn_more,
                containsHtml = true,
            )
        )

    data class BuiltInReaderScanningState(
        override val onSecondaryActionClicked: (() -> Unit),
        override val onLearnMoreClicked: (() -> Unit),
    ) : ScanningState,
        CardReaderConnectViewState(
            headerLabel = UiString.UiStringRes(R.string.card_reader_connect_scanning_built_in_header),
            illustration = R.drawable.img_card_reader_tpp_connecting,
            hintLabel = R.string.card_reader_connect_scanning_built_in_hint,
            secondaryActionLabel = R.string.cancel,
        )

    data class ExternalReaderFoundState(
        override val onPrimaryActionClicked: (() -> Unit),
        override val onSecondaryActionClicked: (() -> Unit),
        override val onTertiaryActionClicked: (() -> Unit),
        val readerId: String,
    ) : CardReaderConnectViewState(
        headerLabel = UiString.UiStringRes(
            stringRes = R.string.card_reader_connect_reader_found_header,
            params = listOf(UiString.UiStringText("<b>$readerId</b>")),
            containsHtml = true
        ),
        illustration = R.drawable.img_card_reader,
        primaryActionLabel = R.string.card_reader_connect_to_reader,
        secondaryActionLabel = R.string.card_reader_connect_keep_searching_button,
        tertiaryActionLabel = R.string.cancel,
        illustrationTopMargin = R.dimen.major_150
    )

    data class MultipleExternalReadersFoundState(
        override val listItems: List<CardReaderConnectViewModel.ListItemViewState>,
        override val onSecondaryActionClicked: () -> Unit
    ) : CardReaderConnectViewState(
        headerLabel = UiString.UiStringRes(R.string.card_reader_connect_multiple_readers_found_header),
        secondaryActionLabel = R.string.cancel
    )

    data class ExternalReaderConnectingState(override val onSecondaryActionClicked: (() -> Unit)) :
        ConnectingState,
        CardReaderConnectViewState(
            headerLabel = UiString.UiStringRes(R.string.card_reader_connect_connecting_header),
            illustration = R.drawable.img_card_reader_connecting,
            hintLabel = R.string.card_reader_connect_connecting_hint,
            secondaryActionLabel = R.string.cancel,
            illustrationTopMargin = R.dimen.major_275
        )

    data class BuiltInReaderConnectingState(override val onSecondaryActionClicked: (() -> Unit)) :
        ConnectingState,
        CardReaderConnectViewState(
            headerLabel = UiString.UiStringRes(R.string.card_reader_connect_connecting_built_in_header),
            illustration = R.drawable.img_card_reader_tpp_connecting,
            hintLabel = R.string.card_reader_connect_connecting_hint,
            secondaryActionLabel = R.string.cancel,
            illustrationTopMargin = R.dimen.major_275
        )

    data class ScanningFailedState(
        override val onPrimaryActionClicked: () -> Unit,
        override val onSecondaryActionClicked: () -> Unit
    ) : CardReaderConnectViewState(
        headerLabel = UiString.UiStringRes(R.string.card_reader_connect_scanning_failed_header),
        illustration = R.drawable.img_products_error,
        primaryActionLabel = R.string.try_again,
        secondaryActionLabel = R.string.cancel,
        illustrationTopMargin = R.dimen.major_150
    )

    data class ConnectingFailedState(
        override val onPrimaryActionClicked: () -> Unit,
        override val onSecondaryActionClicked: () -> Unit
    ) : CardReaderConnectViewState(
        headerLabel = UiString.UiStringRes(R.string.card_reader_connect_failed_header),
        illustration = R.drawable.img_products_error,
        primaryActionLabel = R.string.try_again,
        secondaryActionLabel = R.string.cancel,
        illustrationTopMargin = R.dimen.major_150
    )

    data class LocationPermissionRationale(
        override val onPrimaryActionClicked: () -> Unit
    ) : CardReaderConnectViewState(
        headerLabel = UiString.UiStringRes(R.string.card_reader_connect_permission_rationale_header),
        hintLabel = R.string.card_reader_connect_permission_rationale_hint,
        illustration = R.drawable.img_location,
        primaryActionLabel = R.string.card_reader_connect_permission_rationale_action,
        illustrationTopMargin = R.dimen.major_150
    )

    data class MissingLocationPermissionsError(
        override val onPrimaryActionClicked: () -> Unit,
        override val onSecondaryActionClicked: () -> Unit
    ) : CardReaderConnectViewState(
        headerLabel = UiString.UiStringRes(R.string.card_reader_connect_missing_permissions_header),
        illustration = R.drawable.img_products_error,
        primaryActionLabel = R.string.card_reader_connect_open_permission_settings,
        secondaryActionLabel = R.string.cancel,
        illustrationTopMargin = R.dimen.major_150
    )

    data class LocationDisabledError(
        override val onPrimaryActionClicked: () -> Unit,
        override val onSecondaryActionClicked: () -> Unit
    ) : CardReaderConnectViewState(
        headerLabel = UiString.UiStringRes(R.string.card_reader_connect_location_provider_disabled_header),
        illustration = R.drawable.img_products_error,
        primaryActionLabel = R.string.card_reader_connect_open_location_settings,
        secondaryActionLabel = R.string.cancel,
        illustrationTopMargin = R.dimen.major_150
    )

    data class BluetoothDisabledError(
        override val onPrimaryActionClicked: () -> Unit,
        override val onSecondaryActionClicked: () -> Unit
    ) : CardReaderConnectViewState(
        headerLabel = UiString.UiStringRes(R.string.card_reader_connect_bluetooth_disabled_header),
        illustration = R.drawable.img_products_error,
        primaryActionLabel = R.string.card_reader_connect_open_bluetooth_settings,
        secondaryActionLabel = R.string.cancel,
        illustrationTopMargin = R.dimen.major_150
    )

    data class MissingBluetoothPermissionsError(
        override val onPrimaryActionClicked: () -> Unit,
        override val onSecondaryActionClicked: () -> Unit
    ) : CardReaderConnectViewState(
        headerLabel = UiString.UiStringRes(R.string.card_reader_connect_missing_bluetooth_permissions_header),
        illustration = R.drawable.img_products_error,
        primaryActionLabel = R.string.card_reader_connect_missing_bluetooth_permission_button,
        secondaryActionLabel = R.string.cancel,
        illustrationTopMargin = R.dimen.major_150
    )

    data class MissingMerchantAddressError(
        override val onPrimaryActionClicked: () -> Unit,
        override val onSecondaryActionClicked: () -> Unit
    ) : CardReaderConnectViewState(
        headerLabel = UiString.UiStringRes(R.string.card_reader_connect_missing_address),
        illustration = R.drawable.img_products_error,
        primaryActionLabel = R.string.card_reader_connect_missing_address_button,
        secondaryActionLabel = R.string.cancel,
        illustrationTopMargin = R.dimen.major_150
    )

    data class InvalidMerchantAddressPostCodeError(
        override val onPrimaryActionClicked: () -> Unit,
    ) : CardReaderConnectViewState(
        headerLabel = UiString.UiStringRes(R.string.card_reader_connect_invalid_postal_code_header),
        hintLabel = R.string.card_reader_connect_invalid_postal_code_hint,
        illustration = R.drawable.img_products_error,
        primaryActionLabel = R.string.try_again,
        illustrationTopMargin = R.dimen.major_150,
    )
}
