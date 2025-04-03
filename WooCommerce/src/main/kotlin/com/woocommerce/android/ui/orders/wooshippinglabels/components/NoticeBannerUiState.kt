package com.woocommerce.android.ui.orders.wooshippinglabels.components

import androidx.annotation.StringRes

data class NoticeBannerUiState(
    @StringRes val message: Int,
    val type: NoticeType,
    val autoDismiss: Boolean = false,
    val error: Boolean,
    val onTapped: (() -> Unit)? = null,
    val onDismissed: (() -> Unit)? = null
)

enum class NoticeType {
    UNVERIFIED_ORIGIN_ADDRESS,
    MISSING_DESTINATION_ADDRESS,
    UNVERIFIED_DESTINATION_ADDRESS,
    VERIFIED_ORIGIN_ADDRESS,
    VERIFIED_DESTINATION_ADDRESS,
    MISSING_ITN
}
