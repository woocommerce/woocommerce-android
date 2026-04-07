package com.woocommerce.android.ui.orders.wooshippinglabels.address

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingAddresses
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeBannerUiState
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeType
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeType.MISSING_DESTINATION_ADDRESS
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeType.MISSING_ITN
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeType.MISSING_ORIGIN_ADDRESS
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeType.UNVERIFIED_DESTINATION_ADDRESS
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeType.UNVERIFIED_ORIGIN_ADDRESS
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeType.VERIFIED_DESTINATION_ADDRESS
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeType.VERIFIED_ORIGIN_ADDRESS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveShippingLabelNotice @Inject constructor(private val addressValidationHelper: AddressValidationHelper) {
    private val isDismissedFlow = MutableStateFlow(NoticeType.entries.associateWith { false })
    private var previousNotice: NoticeType? = null

    operator fun invoke(
        shippingAddresses: Flow<List<WooShippingAddresses>>,
        customsState: Flow<List<CustomsState>>,
        selectedIndexFlow: Flow<Int>,
        coroutineScope: CoroutineScope,
    ) = combine(
        shippingAddresses.filter { it.isNotEmpty() },
        customsState,
        selectedIndexFlow,
        isDismissedFlow
    ) { addresses, customs, selectedIndex, isDismissed ->
        val noticeType =
            getNoticeType(addresses[selectedIndex], customs[selectedIndex], isDismissed) ?: return@combine null
        getNoticeBannerUiState(noticeType).also { state ->
            previousNotice = state.type
            if (state.autoDismiss) {
                // Dismiss the notice after AUTO_DISMISS_TIME passes
                coroutineScope.launch {
                    delay(AUTO_DISMISS_TIME)
                    onDismissed(noticeType).invoke()
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getNoticeType(
        addresses: WooShippingAddresses,
        customs: CustomsState,
        isDismissed: Map<NoticeType, Boolean>
    ) = when {
        !addresses.shipFrom.isVerified && isDismissed[UNVERIFIED_ORIGIN_ADDRESS] == false -> {
            UNVERIFIED_ORIGIN_ADDRESS
        }

        (
            addressValidationHelper.isMissingDestinationAddress(addresses.shipTo.address) ||
                !addressValidationHelper.isPhoneValidForShippingLabel(addresses.shipTo.address.phone)
            ) &&
            isDismissed[MISSING_DESTINATION_ADDRESS] == false -> {
            MISSING_DESTINATION_ADDRESS
        }

        !addresses.shipTo.isVerified && isDismissed[MISSING_DESTINATION_ADDRESS] == false &&
            isDismissed[UNVERIFIED_DESTINATION_ADDRESS] == false -> {
            UNVERIFIED_DESTINATION_ADDRESS
        }

        addresses.shipFrom.isVerified && addressValidationHelper.isMissingOriginAddress(addresses.shipFrom) &&
            isDismissed[MISSING_ORIGIN_ADDRESS] == false -> {
            MISSING_ORIGIN_ADDRESS
        }

        addresses.shipFrom.isVerified &&
            (
                previousNotice == UNVERIFIED_ORIGIN_ADDRESS ||
                    (previousNotice == MISSING_ORIGIN_ADDRESS && isDismissed[MISSING_ORIGIN_ADDRESS] == false)
                ) && isDismissed[VERIFIED_ORIGIN_ADDRESS] == false -> {
            VERIFIED_ORIGIN_ADDRESS
        }

        addresses.shipTo.isVerified && isDismissed[VERIFIED_DESTINATION_ADDRESS] == false &&
            (previousNotice == MISSING_DESTINATION_ADDRESS || previousNotice == UNVERIFIED_DESTINATION_ADDRESS) -> {
            VERIFIED_DESTINATION_ADDRESS
        }

        customs is CustomsState.ItnMissing && isDismissed[MISSING_ITN] == false -> {
            MISSING_ITN
        }

        else -> null
    }

    private fun getNoticeBannerUiState(noticeType: NoticeType) = when (noticeType) {
        MISSING_ORIGIN_ADDRESS -> NoticeBannerUiState(
            message = R.string.woo_shipping_address_notification_origin_missing_or_invalid,
            type = MISSING_ORIGIN_ADDRESS,
            autoDismiss = false,
            error = true,
            onDismissed = onDismissed(MISSING_ORIGIN_ADDRESS)
        )

        UNVERIFIED_ORIGIN_ADDRESS -> NoticeBannerUiState(
            message = R.string.woo_shipping_address_notification_origin_unverified,
            type = UNVERIFIED_ORIGIN_ADDRESS,
            autoDismiss = false,
            error = true,
            onDismissed = onDismissed(UNVERIFIED_ORIGIN_ADDRESS)
        )

        MISSING_DESTINATION_ADDRESS -> NoticeBannerUiState(
            message = R.string.woo_shipping_address_notification_destination_missing_or_invalid,
            type = MISSING_DESTINATION_ADDRESS,
            autoDismiss = false,
            error = true,
            onDismissed = onDismissed(MISSING_DESTINATION_ADDRESS)
        )

        UNVERIFIED_DESTINATION_ADDRESS -> NoticeBannerUiState(
            message = R.string.woo_shipping_address_notification_destination_unverified,
            type = UNVERIFIED_DESTINATION_ADDRESS,
            autoDismiss = false,
            error = true,
            onDismissed = onDismissed(UNVERIFIED_DESTINATION_ADDRESS)
        )

        VERIFIED_ORIGIN_ADDRESS -> NoticeBannerUiState(
            message = R.string.woo_shipping_address_notification_origin_verified,
            type = VERIFIED_ORIGIN_ADDRESS,
            autoDismiss = true,
            error = false,
            onDismissed = onDismissed(VERIFIED_ORIGIN_ADDRESS)
        )

        VERIFIED_DESTINATION_ADDRESS -> NoticeBannerUiState(
            message = R.string.woo_shipping_address_notification_destination_verified,
            type = VERIFIED_DESTINATION_ADDRESS,
            autoDismiss = true,
            error = false,
            onDismissed = onDismissed(VERIFIED_DESTINATION_ADDRESS)
        )

        MISSING_ITN -> NoticeBannerUiState(
            message = R.string.woo_shipping_labels_customs_itn_required_error,
            type = MISSING_ITN,
            autoDismiss = false,
            error = true,
            onDismissed = onDismissed(MISSING_ITN)
        )
    }

    private fun onDismissed(noticeType: NoticeType) = {
        isDismissedFlow.value = isDismissedFlow.value.toMutableMap().also { it[noticeType] = true }
    }
}

@VisibleForTesting
const val AUTO_DISMISS_TIME = 2_000L
