package com.woocommerce.android.ui.coupons

import com.woocommerce.android.ui.coupons.CouponTestUtils.generateTestCoupon
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.Date

class CouponRepositoryTest {
    @Test
    fun `given unchanged local expiry, when update request is created, then omit expiry`() {
        val original = generateTestCoupon(1L).copy(
            dateExpiresGmt = Date.from(Instant.parse("2025-06-15T13:45:30Z")),
            dateExpiresLocal = LocalDate.of(2025, 6, 16)
        )

        val request = original.copy(description = "changed").createUpdateCouponRequest(original)

        assertThat(request.expiryDate).isNull()
    }

    @Test
    fun `given existing local expiry, when it is cleared, then send empty expiry`() {
        val original = generateTestCoupon(1L).copy(dateExpiresLocal = LocalDate.of(2025, 6, 16))

        val request = original.copy(dateExpiresLocal = null).createUpdateCouponRequest(original)

        assertThat(request.expiryDate).isEmpty()
    }

    @Test
    fun `given local expiry changes, when update request is created, then send local midnight`() {
        val original = generateTestCoupon(1L).copy(dateExpiresLocal = LocalDate.of(2025, 6, 16))

        val request = original.copy(dateExpiresLocal = LocalDate.of(2025, 11, 3))
            .createUpdateCouponRequest(original)

        assertThat(request.expiryDate).isEqualTo("2025-11-03T00:00:00")
    }

    @Test
    fun `given no expiry, when create request is created, then preserve empty expiry behavior`() {
        val request = generateTestCoupon(1L).createUpdateCouponRequest()

        assertThat(request.expiryDate).isEmpty()
    }

    @Test
    fun `given local expiry, when create request is created, then send local midnight`() {
        val request = generateTestCoupon(1L)
            .copy(dateExpiresLocal = LocalDate.of(2025, 11, 3))
            .createUpdateCouponRequest()

        assertThat(request.expiryDate).isEqualTo("2025-11-03T00:00:00")
    }
}
