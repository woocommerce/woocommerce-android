package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons.CouponDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons.CouponReportDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons.CouponRestClient
import org.wordpress.android.fluxc.persistence.TransactionExecutor
import org.wordpress.android.fluxc.persistence.dao.CouponsDao
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEntity.DiscountType
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.math.BigDecimal

@RunWith(MockitoJUnitRunner::class)
class CouponStoreTest {
    @Mock private lateinit var restClient: CouponRestClient
    @Mock private lateinit var couponsDao: CouponsDao

    private val transactionExecutor: TransactionExecutor = mock {
        val blockArg1 = argumentCaptor<suspend () -> Unit>()
        onBlocking { executeInTransaction(blockArg1.capture()) }.then {
            runBlocking {
                blockArg1.lastValue.invoke()
            }
        }
    }

    private lateinit var couponStore: CouponStore

    private val site = SiteModel().apply { siteId = 123 }

    private val couponDto = CouponDto(
        id = 1L,
        code = "CODE",
        amount = "10",
        dateCreated = "2021-12-27 11:33:55",
        dateCreatedGmt = "2021-12-27 11:33:55Z",
        dateModified = "2021-12-27 11:33:55",
        dateModifiedGmt = "2021-12-27 11:33:55Z",
        discountType = "percent",
        description = "Description",
        dateExpires = "2023-12-27 11:33:55",
        dateExpiresGmt = "2023-12-27 11:33:55Z",
        usageCount = 1,
        isForIndividualUse = false,
        productIds = listOf(2L, 3L),
        excludedProductIds = listOf(4L, 5L),
        usageLimit = 3,
        usageLimitPerUser = 1,
        limitUsageToXItems = 2,
        isShippingFree = false,
        productCategoryIds = listOf(2L, 3L),
        excludedProductCategoryIds = listOf(4L, 5L),
        areSaleItemsExcluded = true,
        minimumAmount = "5",
        maximumAmount = "50",
        restrictedEmails = listOf("email@email.com"),
        usedBy = null
    )

    private val expectedCoupon = CouponEntity(
        id = RemoteId(couponDto.id),
        localSiteId = site.localId(),
        code = couponDto.code,
        amount = couponDto.amount?.toBigDecimal(),
        dateCreated = couponDto.dateCreated,
        dateCreatedGmt = couponDto.dateCreatedGmt,
        dateModified = couponDto.dateModified,
        dateModifiedGmt = couponDto.dateModifiedGmt,
        discountType = couponDto.discountType?.let { DiscountType.fromString(it) },
        description = couponDto.description,
        dateExpires = couponDto.dateExpires,
        dateExpiresGmt = couponDto.dateExpiresGmt,
        usageCount = couponDto.usageCount,
        isForIndividualUse = couponDto.isForIndividualUse,
        usageLimit = couponDto.usageLimit,
        usageLimitPerUser = couponDto.usageLimitPerUser,
        limitUsageToXItems = couponDto.limitUsageToXItems,
        isShippingFree = couponDto.isShippingFree,
        areSaleItemsExcluded = couponDto.areSaleItemsExcluded,
        minimumAmount = couponDto.minimumAmount?.toBigDecimal(),
        maximumAmount = couponDto.maximumAmount?.toBigDecimal(),
        includedProductIds = couponDto.productIds,
        excludedProductIds = couponDto.excludedProductIds,
        includedCategoryIds = couponDto.productCategoryIds,
        excludedCategoryIds = couponDto.excludedProductCategoryIds
    )

    private val expectedEmail = CouponEmailEntity(
        couponId = RemoteId(couponDto.id),
        localSiteId = site.localId(),
        email = couponDto.restrictedEmails!!.first()
    )

    @Before
    fun setUp() {
        couponStore = CouponStore(
            restClient,
            couponsDao,
            initCoroutineEngine(),
            transactionExecutor
        )
    }

    @Test
    fun `Coupons is fetched with a correct result`() = test {
        whenever(restClient.fetchCoupons(
            site,
            CouponStore.DEFAULT_PAGE,
            CouponStore.DEFAULT_PAGE_SIZE
        )).thenReturn(WooPayload(arrayOf(couponDto)))

        val result = couponStore.fetchCoupons(site)

        assertThat(result).isEqualTo(WooResult(false))
    }

    @Test
    fun `Coupon is inserted in DB correctly`() = test {
        whenever(restClient.fetchCoupons(
            site,
            CouponStore.DEFAULT_PAGE,
            CouponStore.DEFAULT_PAGE_SIZE
        )).thenReturn(WooPayload(arrayOf(couponDto)))

        couponStore.fetchCoupons(site, page = CouponStore.DEFAULT_PAGE)

        verify(couponsDao).insertOrUpdateCoupon(expectedCoupon)
    }

    @Test
    fun `Coupon emails are inserted in DB correctly`() = test {
        whenever(restClient.fetchCoupons(
            site,
            CouponStore.DEFAULT_PAGE,
            CouponStore.DEFAULT_PAGE_SIZE
        )).thenReturn(WooPayload(arrayOf(couponDto)))

        couponStore.fetchCoupons(site)

        verify(couponsDao).insertOrUpdateCouponEmail(expectedEmail)
    }

    @Test
    fun `Observing the DB changes returns the correct coupon data model`(): Unit = test {
        val expectedDataModel = listOf(
            CouponWithEmails(
                expectedCoupon,
                listOf(expectedEmail)
            )
        )

        whenever(couponsDao.observeCoupons(site.localId())).thenReturn(flowOf(expectedDataModel))

        val observedDataModel = couponStore.observeCoupons(site).first()

        assertThat(observedDataModel).isEqualTo(expectedDataModel)
    }

    @Test
    fun `Observing a specific coupon returns the correct coupon data model`(): Unit = test {
        val expectedDataModel = CouponWithEmails(
            expectedCoupon,
            listOf(expectedEmail)
        )

        whenever(couponsDao.observeCoupon(site.localId(), expectedCoupon.id)).thenReturn(
            flowOf(expectedDataModel)
        )

        val observedDataModel = couponStore.observeCoupon(site, expectedCoupon.id.value).first()

        assertThat(observedDataModel).isEqualTo(expectedDataModel)
    }

    @Test
    fun `fetching coupon report should return the correct data`() = test {
        whenever(restClient.fetchCouponReport(any(), any(), any())).thenReturn(
            WooPayload(
                CouponReportDto(
                    couponId = expectedCoupon.id.value,
                    amount = "10",
                    ordersCount = 2
                )
            )
        )

        val couponReport = couponStore.fetchCouponReport(site, expectedCoupon.id.value).model!!

        assertThat(couponReport.couponId).isEqualTo(expectedCoupon.id.value)
        assertThat(couponReport.amount).isEqualByComparingTo(BigDecimal.TEN)
        assertThat(couponReport.ordersCount).isEqualTo(2)
    }
}
