package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import java.io.IOException
import org.assertj.core.api.Assertions.assertThat
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEntity.DiscountType.Percent
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails
import java.math.BigDecimal

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CouponsDaoTest {
    private lateinit var couponsDao: CouponsDao
    private lateinit var db: WCAndroidDatabase

    private val coupon = generateCouponEntity()
    private val email = CouponEmailEntity(
        couponId = coupon.id,
        localSiteId = coupon.localSiteId,
        email = "test@test.com"
    )

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        couponsDao = db.couponsDao
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `test coupon insert and update`(): Unit = runBlocking {
        // when
        couponsDao.insertOrUpdateCoupon(coupon)
        couponsDao.insertOrUpdateCouponEmail(email)
        var observedCoupon = couponsDao.observeCoupons(coupon.localSiteId).first()

        // then
        val expected = CouponWithEmails(coupon, listOf(email))
        assertThat(observedCoupon.first()).isEqualTo(expected)

        // when
        val newCoupon = coupon.copy(description = "Updated", usageLimit = 2)
        val newExpected = CouponWithEmails(newCoupon, listOf(email))
        couponsDao.insertOrUpdateCoupon(newCoupon)
        couponsDao.insertOrUpdateCouponEmail(email)
        observedCoupon = couponsDao.observeCoupons(coupon.localSiteId).first()

        // then
        assertThat(observedCoupon.first()).isEqualTo(newExpected)
    }

    @Test
    fun `test specific observation when it is not in the DB`(): Unit = runBlocking {
        // when
        couponsDao.insertOrUpdateCoupon(coupon)
        couponsDao.insertOrUpdateCouponEmail(email)
        val observedCoupon = couponsDao.observeCoupon(LocalId(1), RemoteId( coupon.id.value + 1))

        // then
        assertThat(observedCoupon.first()).isNull()
    }

    @Test
    fun `test specific observation when it is present in the DB`(): Unit = runBlocking {
        // when
        couponsDao.insertOrUpdateCoupon(coupon)
        couponsDao.insertOrUpdateCouponEmail(email)
        val observedCoupon = couponsDao.observeCoupon(LocalId(1), coupon.id)

        var expected = CouponWithEmails(coupon, listOf(email))
        assertThat(observedCoupon.first()).isEqualTo(expected)

        // when
        val newCoupon = coupon.copy(description = "Updated", usageLimit = 2)
        expected = CouponWithEmails(newCoupon, listOf(email))
        couponsDao.insertOrUpdateCoupon(newCoupon)
        couponsDao.insertOrUpdateCouponEmail(email)

        // then
        assertThat(observedCoupon.first()).isEqualTo(expected)
    }

    @Test
    fun `test coupon delete`(): Unit = runBlocking {
        // when
        val coupon = generateCouponEntity()
        val email = CouponEmailEntity(
            couponId = coupon.id,
            localSiteId = coupon.localSiteId,
            email = "test@test.com"
        )
        couponsDao.insertOrUpdateCoupon(coupon)
        couponsDao.insertOrUpdateCouponEmail(email)
        var observedCoupons = couponsDao.observeCoupons(coupon.localSiteId).first()
        var emailCount = couponsDao.getEmailCount(coupon.localSiteId, coupon.id)

        // then
        assertThat(observedCoupons.size).isEqualTo(1)
        assertThat(emailCount).isEqualTo(1)

        // when
        couponsDao.deleteCoupon(coupon.localSiteId, coupon.id)
        observedCoupons = couponsDao.observeCoupons(coupon.localSiteId).first()
        emailCount = couponsDao.getEmailCount(coupon.localSiteId, coupon.id)

        // then
        assertThat(observedCoupons.size).isEqualTo(0)
        assertThat(emailCount).isEqualTo(0)
    }

    companion object {
        fun generateCouponEntity() = CouponEntity(
            id = RemoteId(1),
            localSiteId = LocalId(1),
            amount = BigDecimal(5),
            code = "code",
            dateCreated = "2007-04-05T14:30Z",
            dateCreatedGmt = "2007-04-05T14:30Z",
            dateModified = "2007-04-05T14:30Z",
            dateModifiedGmt = "2007-04-05T14:30Z",
            discountType = Percent,
            description = "Description",
            dateExpires = "2008-04-05T14:30Z",
            dateExpiresGmt = "2008-04-05T14:30Z",
            usageCount = 0,
            isForIndividualUse = true,
            usageLimit = 3,
            usageLimitPerUser = 1,
            limitUsageToXItems = 5,
            isShippingFree = false,
            areSaleItemsExcluded = true,
            minimumAmount = BigDecimal(1),
            maximumAmount = BigDecimal(10)
        )
    }
}
