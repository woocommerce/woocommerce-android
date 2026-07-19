package com.woocommerce.android.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails
import java.time.Instant
import java.time.LocalDate
import java.util.Date

class CouponMapperTest {
    @Test
    fun `when expiry fields are valid, then preserve exact GMT instant and independent local date`() {
        val coupon = createCoupon(
            dateExpires = "2025-06-16T00:00:00",
            dateExpiresGmt = "2025-06-15T13:45:30"
        ).toAppModel()

        assertThat(coupon.dateExpiresGmt).isEqualTo(Date.from(Instant.parse("2025-06-15T13:45:30Z")))
        assertThat(coupon.dateExpiresLocal).isEqualTo(LocalDate.of(2025, 6, 16))
    }

    @Test
    fun `when local expiry is malformed, then preserve GMT instant without inferring local date`() {
        val coupon = createCoupon(
            dateExpires = "invalid",
            dateExpiresGmt = "2025-06-15T13:45:30"
        ).toAppModel()

        assertThat(coupon.dateExpiresGmt).isEqualTo(Date.from(Instant.parse("2025-06-15T13:45:30Z")))
        assertThat(coupon.dateExpiresLocal).isNull()
    }

    @Test
    fun `when local expiry is missing, then preserve GMT instant without inferring local date`() {
        val coupon = createCoupon(
            dateExpires = null,
            dateExpiresGmt = "2025-06-15T13:45:30"
        ).toAppModel()

        assertThat(coupon.dateExpiresGmt).isEqualTo(Date.from(Instant.parse("2025-06-15T13:45:30Z")))
        assertThat(coupon.dateExpiresLocal).isNull()
    }

    @Test
    fun `when GMT expiry is missing, then preserve local date without inferring GMT instant`() {
        val coupon = createCoupon(
            dateExpires = "2025-06-16T00:00:00",
            dateExpiresGmt = null
        ).toAppModel()

        assertThat(coupon.dateExpiresGmt).isNull()
        assertThat(coupon.dateExpiresLocal).isEqualTo(LocalDate.of(2025, 6, 16))
    }

    private fun createCoupon(dateExpires: String?, dateExpiresGmt: String?) = CouponWithEmails(
        coupon = CouponEntity(
            id = RemoteId(1L),
            localSiteId = LocalId(1),
            dateExpires = dateExpires,
            dateExpiresGmt = dateExpiresGmt
        ),
        restrictedEmails = emptyList()
    )
}
