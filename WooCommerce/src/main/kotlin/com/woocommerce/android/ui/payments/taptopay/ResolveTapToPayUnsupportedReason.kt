package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.util.DeviceSecurityPatchProvider
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

/**
 * Turns Stripe's rejection into a reason the merchant can act on.
 *
 * Stripe only reports that attestation failed, most often with its Trusted Execution Environment
 * message, while the actual cause is frequently a security patch older than the 12 months Stripe
 * requires. That one we can check ourselves, so it is reported separately.
 */
class ResolveTapToPayUnsupportedReason @Inject constructor(
    private val deviceSecurityPatchProvider: DeviceSecurityPatchProvider,
    private val clock: Clock,
) {
    operator fun invoke(stripeMessage: String): TapToPayUnsupportedReason =
        if (isSecurityPatchOutdated()) {
            TapToPayUnsupportedReason.OutdatedSecurityPatch
        } else {
            TapToPayUnsupportedReason.Unspecified(stripeMessage)
        }

    private fun isSecurityPatchOutdated(): Boolean {
        val securityPatch = deviceSecurityPatchProvider.get()?.let {
            try {
                LocalDate.parse(it)
            } catch (e: DateTimeParseException) {
                null
            }
        } ?: return false
        return securityPatch.isBefore(LocalDate.now(clock).minusMonths(MAX_SECURITY_PATCH_AGE_MONTHS))
    }

    private companion object {
        const val MAX_SECURITY_PATCH_AGE_MONTHS = 12L
    }
}
