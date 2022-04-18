package com.woocommerce.android.ui.coupons.details

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.coupons.CouponTestUtils
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.*
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.InlineClassesAnswer
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal

class CouponDetailsViewModelTest : BaseUnitTest() {
    companion object {
        private const val COUPON_ID = 1L
    }

    private val couponFlow = MutableSharedFlow<Coupon>(extraBufferCapacity = 1)
    private val couponDetailsRepository: CouponDetailsRepository = mock {
        on { observeCoupon(any()) } doReturn couponFlow
        onBlocking { fetchCouponPerformance(any()) } doReturn
            Result.success(CouponTestUtils.generateTestCouponPerformance(COUPON_ID))
        onBlocking { fetchCoupon(any()) } doReturn
            Result.success(Unit)
    }
    private val wooCommerceStore: WooCommerceStore = mock {
        on { getSiteSettings(any()) } doReturn WCSettingsModel(0, "USD", LEFT, "", "", 2)
    }
    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency(any<BigDecimal>(), any(), any()) } doAnswer { it.arguments[0].toString() }
    }
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
        on { getString(any(), anyVararg()) } doAnswer { it.arguments[0].toString() }
    }
    private val couponUtils = CouponUtils(
        currencyFormatter = currencyFormatter,
        resourceProvider = resourceProvider
    )
    private lateinit var viewModel: CouponDetailsViewModel

    suspend fun setup(mockSetup: suspend () -> Unit = {}) {
        val selectedSite = mock<SelectedSite> {
            on { get() } doReturn SiteModel()
        }
        mockSetup()
        viewModel = CouponDetailsViewModel(
            savedState = CouponDetailsFragmentArgs(COUPON_ID).initSavedStateHandle(),
            wooCommerceStore = wooCommerceStore,
            selectedSite = selectedSite,
            couponDetailsRepository = couponDetailsRepository,
            couponUtils = couponUtils
        )
    }

    @Test
    fun `when the screen loads, then show coupon details`() = testBlocking {
        val coupon = CouponTestUtils.generateTestCoupon(COUPON_ID)

        setup()

        var state: CouponDetailsState? = null
        viewModel.couponState.observeForever {
            state = it
        }

        couponFlow.tryEmit(coupon)

        assertThat(state?.couponSummary?.code).isEqualTo(coupon.code)
        assertThat(state?.couponSummary?.summary).isEqualTo(couponUtils.generateSummary(coupon, "USD"))
        assertThat(state?.couponSummary?.discountType).isEqualTo(couponUtils.localizeType(coupon.type!!))
        assertThat(state?.couponSummary?.minimumSpending).isEqualTo(
            couponUtils.formatMinimumSpendingInfo(
                coupon.minimumAmount,
                "USD"
            )
        )
        assertThat(state?.couponSummary?.maximumSpending).isEqualTo(
            couponUtils.formatMaximumSpendingInfo(
                coupon.minimumAmount,
                "USD"
            )
        )
    }

    @Test
    fun `when the screen loads, then fetch a fresh copy of coupon`() = testBlocking {
        setup()

        verify(couponDetailsRepository).fetchCoupon(COUPON_ID)
    }

    @Test
    fun `given no cached coupon exists, when fetching coupon fails, then show an error`() = testBlocking {
        setup {
            whenever(couponDetailsRepository.fetchCoupon(COUPON_ID)).doSuspendableAnswer {
                // Force suspending, to make sure fetch result is handled after observing DB
                delay(1)
                Result.failure(Exception())
            }
        }

        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever {
            events.add(it)
        }
        viewModel.couponState.observeForever {}

        advanceUntilIdle()

        assertThat(events.takeLast(2)).containsExactly(
            ShowSnackbar(R.string.coupon_summary_loading_failure),
            Exit
        )
    }

    @Test
    fun `given a cached coupon exists, when fetching coupon fails, then don't show an error`() = testBlocking {
        setup {
            whenever(couponDetailsRepository.fetchCoupon(COUPON_ID)).doSuspendableAnswer {
                // Force suspending, to make sure fetch result is handled after observing DB
                delay(1)
                Result.failure(Exception())
            }
        }

        val events = mutableListOf<MultiLiveEvent.Event>()
        viewModel.event.observeForever {
            events.add(it)
        }
        viewModel.couponState.observeForever {}

        couponFlow.emit(CouponTestUtils.generateTestCoupon(COUPON_ID))
        advanceUntilIdle()

        assertThat(events).doesNotContain(ShowSnackbar(R.string.coupon_summary_loading_failure))
    }

    @Test
    fun `when screen loads, then fetch coupon performance report`() = testBlocking {
        setup()

        viewModel.couponState.observeForever {}

        verify(couponDetailsRepository).fetchCouponPerformance(COUPON_ID)
    }

    @Test
    fun `when the coupon has 0 usage, then infer performance directly`() = testBlocking {
        setup {
            whenever(couponDetailsRepository.fetchCouponPerformance(COUPON_ID)).doSuspendableAnswer {
                // Force suspending, to make sure asserts are executed before the actual fetch
                delay(1)
                Result.success(CouponTestUtils.generateTestCouponPerformance(COUPON_ID).copy(ordersCount = 10))
            }
        }

        var state: CouponDetailsState? = null
        viewModel.couponState.observeForever {
            state = it
        }

        couponFlow.emit(CouponTestUtils.generateTestCoupon(COUPON_ID).copy(usageCount = 0))

        assertThat(state?.couponPerformanceState).isEqualTo(
            CouponPerformanceState.Success(CouponPerformanceUi(0, couponUtils.formatCurrency(BigDecimal.ZERO, "USD")))
        )
    }

    @Test
    fun `when the coupon performance is loading, then infer usage count from coupon`() = testBlocking {
        setup {
            whenever(couponDetailsRepository.fetchCouponPerformance(COUPON_ID)).doSuspendableAnswer {
                // Force suspending, to make sure asserts are executed before the actual fetch
                delay(1)
                Result.success(CouponTestUtils.generateTestCouponPerformance(COUPON_ID))
            }
        }

        var state: CouponDetailsState? = null
        viewModel.couponState.observeForever {
            state = it
        }

        couponFlow.emit(CouponTestUtils.generateTestCoupon(COUPON_ID))

        assertThat(state?.couponPerformanceState).isEqualTo(
            CouponPerformanceState.Loading(CouponTestUtils.generateTestCoupon(COUPON_ID).usageCount)
        )
    }

    @Test
    fun `when the coupon performance is loaded, then display its state`() = testBlocking {
        val performanceReport = CouponTestUtils.generateTestCouponPerformance(COUPON_ID)
        setup {
            whenever(couponDetailsRepository.fetchCouponPerformance(COUPON_ID)).doAnswer(
                InlineClassesAnswer {
                    Result.success(performanceReport)
                }
            )
        }

        var state: CouponDetailsState? = null
        viewModel.couponState.observeForever {
            state = it
        }

        couponFlow.emit(CouponTestUtils.generateTestCoupon(COUPON_ID))

        val performanceUi = (state?.couponPerformanceState as CouponPerformanceState.Success).data
        assertThat(performanceUi.ordersCount).isEqualTo(performanceReport.ordersCount)
        assertThat(performanceUi.formattedAmount).isEqualTo(couponUtils.formatCurrency(performanceReport.amount, "USD"))
    }
}
