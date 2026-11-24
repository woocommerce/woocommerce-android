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

    data object ChangeAdminPIN : WooPosManagerApprovalAction(
        WooPosCapability.MANAGE_STORE_SETTINGS,
        R.string.woopos_security_approve_admin_change,
    )

    data object DisablePINProtection : WooPosManagerApprovalAction(
        WooPosCapability.MANAGE_STORE_SETTINGS,
        R.string.woopos_security_approve_disable,
    )

    data object Exit : WooPosManagerApprovalAction(WooPosCapability.EXIT_POS, R.string.woopos_approval_exit)
}
