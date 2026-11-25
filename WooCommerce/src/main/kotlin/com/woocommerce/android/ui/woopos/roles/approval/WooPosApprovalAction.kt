package com.woocommerce.android.ui.woopos.roles.approval

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.roles.WooPosCapability

internal sealed class WooPosApprovalAction(
    val capability: WooPosCapability,
    @StringRes val reason: Int,
) {
    data object CreateCoupon : WooPosApprovalAction(
        WooPosCapability.CREATE_COUPONS,
        R.string.woopos_approval_create_coupon,
    )

    data class IssueRefund(val orderNumber: String) : WooPosApprovalAction(
        WooPosCapability.ISSUE_REFUNDS,
        R.string.woopos_approval_issue_refund,
    )

    data object Exit : WooPosApprovalAction(WooPosCapability.EXIT_POS, R.string.woopos_approval_exit)

    data object ExitFromHome : WooPosApprovalAction(WooPosCapability.EXIT_POS, R.string.woopos_approval_exit)
}
