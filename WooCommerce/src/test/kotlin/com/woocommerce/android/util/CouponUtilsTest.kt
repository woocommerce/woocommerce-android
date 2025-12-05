package com.woocommerce.android.util

import com.woocommerce.android.R
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.coupons.CouponTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class CouponUtilsTest : BaseUnitTest() {

    private lateinit var couponUtils: CouponUtils
    private val currencyFormatter: CurrencyFormatter = mock()
    private val resourceProvider: ResourceProvider = mock()

    @Before
    fun setup() {
        couponUtils = CouponUtils(
            currencyFormatter = currencyFormatter,
            resourceProvider = resourceProvider
        )

        whenever(currencyFormatter.formatCurrency(any<BigDecimal>(), any<String>(), any())).then { invocation ->
            val amount = invocation.arguments[0] as BigDecimal
            val currencyCode = invocation.arguments[1] as String
            "$currencyCode${amount.toPlainString()}"
        }

        whenever(resourceProvider.getString(any(), any(), any())).then { invocation ->
            when (invocation.arguments[0]) {
                R.string.coupon_details_share_coupon_all -> {
                    val discount = invocation.arguments[1] as String
                    val code = invocation.arguments[2] as String
                    "Apply $discount off to all products with the promo code $code"
                }
                R.string.coupon_details_share_coupon_some -> {
                    val discount = invocation.arguments[1] as String
                    val code = invocation.arguments[2] as String
                    "Apply $discount off to select products with the promo code $code"
                }
                else -> ""
            }
        }
    }

    @Test
    fun `given percentage coupon, when formatting sharing message, then shows percentage`() {
        val coupon = CouponTestUtils.generateTestCoupon(1L, "PERCENT10").copy(
            type = Coupon.Type.Percent,
            amount = BigDecimal("10")
        )

        val result = couponUtils.formatSharingMessage(
            coupon = coupon,
            currencyCode = "USD"
        )

        assertThat(result).isEqualTo("Apply 10% off to all products with the promo code PERCENT10")
    }

    @Test
    fun `given fixed cart coupon, when formatting sharing message, then shows currency amount`() {
        val coupon = CouponTestUtils.generateTestCoupon(1L, "FIXED15").copy(
            type = Coupon.Type.FixedCart,
            amount = BigDecimal("15.00")
        )

        val result = couponUtils.formatSharingMessage(
            coupon = coupon,
            currencyCode = "USD"
        )

        assertThat(result).isEqualTo("Apply USD15.00 off to all products with the promo code FIXED15")
    }

    @Test
    fun `given fixed product coupon, when formatting sharing message, then shows currency amount`() {
        val coupon = CouponTestUtils.generateTestCoupon(1L, "PRODUCT20").copy(
            type = Coupon.Type.FixedProduct,
            amount = BigDecimal("20.00")
        )

        val result = couponUtils.formatSharingMessage(
            coupon = coupon,
            currencyCode = "EUR"
        )

        assertThat(result).isEqualTo("Apply EUR20.00 off to all products with the promo code PRODUCT20")
    }

    @Test
    fun `given custom type coupon, when formatting sharing message, then shows plain amount`() {
        val coupon = CouponTestUtils.generateTestCoupon(1L, "CUSTOM25").copy(
            type = Coupon.Type.Custom("custom_discount"),
            amount = BigDecimal("25")
        )

        val result = couponUtils.formatSharingMessage(
            coupon = coupon,
            currencyCode = "USD"
        )

        assertThat(result).isEqualTo("Apply 25 off to all products with the promo code CUSTOM25")
    }

    @Test
    fun `given coupon with included products, when formatting sharing message, then shows select products`() {
        val coupon = CouponTestUtils.generateTestCoupon(1L, "SELECT10").copy(
            type = Coupon.Type.Percent,
            amount = BigDecimal("10"),
            productIds = listOf(1L, 2L, 3L)
        )

        val result = couponUtils.formatSharingMessage(
            coupon = coupon,
            currencyCode = "USD"
        )

        assertThat(result).isEqualTo("Apply 10% off to select products with the promo code SELECT10")
    }

    @Test
    fun `given coupon with excluded products, when formatting sharing message, then shows select products`() {
        val coupon = CouponTestUtils.generateTestCoupon(1L, "EXCLUDE5").copy(
            type = Coupon.Type.Percent,
            amount = BigDecimal("5"),
            restrictions = CouponTestUtils.generateCouponRestrictions().copy(
                excludedProductIds = listOf(4L, 5L)
            )
        )

        val result = couponUtils.formatSharingMessage(
            coupon = coupon,
            currencyCode = "USD"
        )

        assertThat(result).isEqualTo("Apply 5% off to select products with the promo code EXCLUDE5")
    }

    @Test
    fun `given coupon with null amount, when formatting sharing message, then returns null`() {
        val coupon = CouponTestUtils.generateTestCoupon(1L, "NULLAMOUNT").copy(
            amount = null
        )

        val result = couponUtils.formatSharingMessage(
            coupon = coupon,
            currencyCode = "USD"
        )

        assertThat(result).isNull()
    }

    @Test
    fun `given coupon with null code, when formatting sharing message, then returns null`() {
        val coupon = CouponTestUtils.generateTestCoupon(1L).copy(
            code = null,
            amount = BigDecimal("10")
        )

        val result = couponUtils.formatSharingMessage(
            coupon = coupon,
            currencyCode = "USD"
        )

        assertThat(result).isNull()
    }

    @Test
    fun `given null currency code, when formatting sharing message, then returns null`() {
        val coupon = CouponTestUtils.generateTestCoupon(1L, "NOCURRENCY").copy(
            amount = BigDecimal("10")
        )

        val result = couponUtils.formatSharingMessage(
            coupon = coupon,
            currencyCode = null
        )

        assertThat(result).isNull()
    }

    @Test
    fun `given percentage coupon with decimal, when formatting sharing message, then shows correct percentage`() {
        val coupon = CouponTestUtils.generateTestCoupon(1L, "PERCENT7HALF").copy(
            type = Coupon.Type.Percent,
            amount = BigDecimal("7.5")
        )

        val result = couponUtils.formatSharingMessage(
            coupon = coupon,
            currencyCode = "USD"
        )

        assertThat(result).isEqualTo("Apply 7.5% off to all products with the promo code PERCENT7HALF")
    }

    @Test
    fun `given coupon with null type, when formatting sharing message, then shows plain amount`() {
        val coupon = CouponTestUtils.generateTestCoupon(1L, "NOTYPE").copy(
            type = null,
            amount = BigDecimal("30")
        )

        val result = couponUtils.formatSharingMessage(
            coupon = coupon,
            currencyCode = "USD"
        )

        assertThat(result).isEqualTo("Apply 30 off to all products with the promo code NOTYPE")
    }
}
