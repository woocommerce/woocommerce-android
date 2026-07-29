package com.woocommerce.android.ui.coupons.edit

import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_COUPON_DISCOUNT_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_DESCRIPTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_EXPIRY_DATE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_PRODUCT_OR_CATEGORY_RESTRICTIONS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_USAGE_RESTRICTIONS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_INCLUDES_FREE_SHIPPING
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_COUPON_DISCOUNT_TYPE_FIXED_CART
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_COUPON_DISCOUNT_TYPE_FIXED_PRODUCT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_COUPON_DISCOUNT_TYPE_PERCENTAGE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.coupons.CouponRepository
import com.woocommerce.android.ui.coupons.CouponTestUtils
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.OpenDescriptionEditor
import com.woocommerce.android.ui.coupons.edit.EditCouponViewModel.Mode
import com.woocommerce.android.ui.coupons.edit.EditCouponViewModel.NavigateBackToPOS
import com.woocommerce.android.ui.coupons.tracking.StoreManagementCouponCreationFlowTrackerEventProvider
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.woopos.home.items.coupons.creation.WooPosCouponCreationFlowTrackerEventProvider
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.spy
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.TIMEOUT
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Date

private const val COUPON_ID = 1L

@ExperimentalCoroutinesApi
class EditCouponViewModelTests : BaseUnitTest() {
    private lateinit var viewModel: EditCouponViewModel

    private var storedCoupon = CouponTestUtils.generateTestCoupon(COUPON_ID)
    private var emptyCoupon = CouponTestUtils.generateEmptyCoupon()

    private val couponRepository: CouponRepository = mock {
        on { observeCoupon(COUPON_ID) } doAnswer {
            // use an Answer to use always the last value of [storedCoupon]
            flowOf(storedCoupon)
        }
    }

    private val currencyFormatter: CurrencyFormatter = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }
    private val couponUtils = spy(
        // Spy this to override some responses
        CouponUtils(
            currencyFormatter = currencyFormatter,
            resourceProvider = resourceProvider
        )
    )
    private val siteParams = SiteParameters(
        currencyCode = null,
        currencySymbol = "$",
        currencyFormattingParameters = null,
        weightUnit = null,
        dimensionUnit = null,
        gmtOffset = 0f
    )

    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    suspend fun setup(
        mode: Mode = Mode.Edit(COUPON_ID),
        isPOSMode: Boolean = false,
        prepareMocks: suspend () -> Unit = {}
    ) {
        prepareMocks()

        viewModel = EditCouponViewModel(
            savedStateHandle = EditCouponFragmentArgs(mode, isPOSMode).toSavedStateHandle(),
            couponRepository = couponRepository,
            couponUtils = couponUtils,
            parameterRepository = mock {
                on { getParameters(any(), any()) } doReturn siteParams
            },
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            resourceProvider = resourceProvider,
            storeManagementEventProvider = StoreManagementCouponCreationFlowTrackerEventProvider(),
            posEventProvider = WooPosCouponCreationFlowTrackerEventProvider()
        )
    }

    @Test
    fun `when screen is opened, then load saved coupon`() = testBlocking {
        setup()

        val state = viewModel.viewState.captureValues().last()

        assertThat(state.couponDraft).isEqualTo(storedCoupon)
        assertThat(state.hasChanges).isEqualTo(false)
    }

    @Test
    fun `when screen is opened in creation mode, then load empty coupon`() = testBlocking {
        setup(mode = Mode.Create(Coupon.Type.Percent))

        val state = viewModel.viewState.captureValues().last()

        assertThat(state.couponDraft).isEqualTo(emptyCoupon.copy(type = Coupon.Type.Percent))
        assertThat(state.hasChanges).isEqualTo(false)
    }

    @Test
    fun `when screen is opened in creation mode, then save button should have correct text`() = testBlocking {
        setup(mode = Mode.Create(Coupon.Type.Percent))

        val state = viewModel.viewState.captureValues().last()

        assertThat(state.saveButtonText).isEqualTo(R.string.coupon_create_save_button)
    }

    @Test
    fun `when screen is opened in edit mode, then save button should have correct text`() = testBlocking {
        setup()

        val state = viewModel.viewState.captureValues().last()

        assertThat(state.saveButtonText).isEqualTo(R.string.coupon_edit_save_button)
    }

    @Test
    fun `when coupon type is percentage, then set amount unit to percent`() = testBlocking {
        storedCoupon = storedCoupon.copy(type = Coupon.Type.Percent)
        setup()

        val state = viewModel.viewState.captureValues().last()

        assertThat(state.amountUnit).isEqualTo("%")
    }

    @Test
    fun `when coupon type is set to fixed discount, then set amount unit to currency symbol`() = testBlocking {
        storedCoupon = storedCoupon.copy(type = Coupon.Type.FixedCart)
        setup()

        val state = viewModel.viewState.captureValues().last()

        assertThat(state.amountUnit).isEqualTo(siteParams.currencySymbol)
    }

    @Test
    fun `when editing the amount, then update coupon draft and hasChanges property`() = testBlocking {
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onAmountChanged(storedCoupon.amount!! + BigDecimal.ONE)
        }.last()

        assertThat(state.couponDraft.amount).isEqualByComparingTo(storedCoupon.amount!! + BigDecimal.ONE)
        assertThat(state.hasChanges).isTrue()
    }

    @Test
    fun `when editing the coupon code, then update coupon draft and hasChanges property`() = testBlocking {
        setup()
        val newCode = storedCoupon.code!! + "A"

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onCouponCodeChanged(newCode)
        }.last()

        assertThat(state.couponDraft.code).isEqualTo(newCode)
        assertThat(state.hasChanges).isTrue()
    }

    @Test
    fun `when regenerate code is clicked, then assign the generated code to the coupon draft`() = testBlocking {
        val generatedCode = "generated"
        setup {
            whenever(couponUtils.generateRandomCode()).thenReturn(generatedCode)
        }

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onRegenerateCodeClick()
        }.last()

        assertThat(state.couponDraft.code).isEqualTo(generatedCode)
    }

    @Test
    fun `when description button is clicked, then open description editor`() = testBlocking {
        setup()

        viewModel.onDescriptionButtonClick()

        val event = viewModel.event.captureValues().last()
        assertThat(event).isEqualTo(OpenDescriptionEditor(storedCoupon.description))
    }

    @Test
    fun `when description changes, then update coupon draft`() = testBlocking {
        setup()

        viewModel.onDescriptionChanged("description")

        val state = viewModel.viewState.captureValues().last()
        assertThat(state.couponDraft.description).isEqualTo("description")
    }

    @Test
    fun `given there are description changes, when description button is clicked, then open description editor`() =
        testBlocking {
            setup()

            viewModel.onDescriptionChanged("description")
            viewModel.onDescriptionButtonClick()

            val event = viewModel.event.captureValues().last()
            assertThat(event).isEqualTo(OpenDescriptionEditor("description"))
        }

    @Test
    fun `when expiry date is changed, then update only the local coupon date`() = testBlocking {
        val exactGmtExpiry = Date.from(Instant.parse("2025-06-15T13:45:30Z"))
        val newDate = LocalDate.of(2025, 6, 16)
        storedCoupon = storedCoupon.copy(dateExpiresGmt = exactGmtExpiry)
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onExpiryDateChanged(newDate)
        }.last()

        assertThat(state.couponDraft.dateExpiresLocal).isEqualTo(newDate)
        assertThat(state.couponDraft.dateExpiresGmt).isEqualTo(exactGmtExpiry)
    }

    @Test
    fun `when expiry date is removed, then update the coupon draft`() = testBlocking {
        storedCoupon = storedCoupon.copy(dateExpiresLocal = LocalDate.of(2025, 6, 16))
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onExpiryDateChanged(null)
        }.last()

        assertThat(state.couponDraft.dateExpiresLocal).isNull()
    }

    @Test
    fun `when free shipping toggle changes, then update coupon draft`() = testBlocking {
        storedCoupon = storedCoupon.copy(isShippingFree = false)
        setup()

        viewModel.onFreeShippingChanged(true)

        val state = viewModel.viewState.captureValues().last()
        assertThat(state.couponDraft.isShippingFree).isTrue()
    }

    @Test
    fun `when save button is clicked, then update coupon`() = testBlocking {
        setup()

        viewModel.onCouponCodeChanged("New code")
        viewModel.onSaveClick()

        verify(couponRepository).updateCoupon(argThat { code == "New code" }, eq(storedCoupon))
    }

    @Test
    fun `when coupon is updated, then show a snackbar and navigate up`() = testBlocking {
        setup {
            whenever(couponRepository.updateCoupon(any(), any())).thenReturn(Result.success(Unit))
        }

        val events = viewModel.event.runAndCaptureValues {
            viewModel.onSaveClick()
        }.takeLast(2)

        assertThat(events[0]).isEqualTo(ShowSnackbar(R.string.coupon_edit_coupon_updated))
        assertThat(events[1]).isEqualTo(Exit)
    }

    @Test
    fun `when coupon is fails, then show an error snackbar`() = testBlocking {
        setup {
            whenever(couponRepository.updateCoupon(any(), any())).thenReturn(
                Result.failure(WooException(WooError(TIMEOUT, UNKNOWN)))
            )
        }

        viewModel.onSaveClick()

        val event = viewModel.event.captureValues().last()
        assertThat(event).isEqualTo(ShowUiStringSnackbar(UiStringRes(R.string.coupon_edit_coupon_update_failed)))
    }

    @Test
    fun `given coupon creation mode and percentage type, when create clicked, should track event`() = testBlocking {
        setup(mode = Mode.Create(Coupon.Type.Percent))

        viewModel.onSaveClick()

        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.COUPON_CREATION_INITIATED,
            mapOf(
                KEY_COUPON_DISCOUNT_TYPE to VALUE_COUPON_DISCOUNT_TYPE_PERCENTAGE,
                KEY_HAS_EXPIRY_DATE to false,
                KEY_INCLUDES_FREE_SHIPPING to null,
                KEY_HAS_DESCRIPTION to false,
                KEY_HAS_PRODUCT_OR_CATEGORY_RESTRICTIONS to false,
                KEY_HAS_USAGE_RESTRICTIONS to false
            )
        )
    }

    @Test
    fun `given creation flow in POS, when create clicked, should track POS event`() = testBlocking {
        setup(mode = Mode.Create(Coupon.Type.Percent), isPOSMode = true)

        viewModel.onSaveClick()

        verify(analyticsTrackerWrapper).track(
            eq(WooPosAnalyticsEvent.Event.CouponCreationInitiated),
            any()
        )
    }

    @Test
    fun `given coupon creation mode and fixed product type, when create clicked, should track event`() = testBlocking {
        setup(mode = Mode.Create(Coupon.Type.FixedProduct))

        viewModel.onSaveClick()

        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.COUPON_CREATION_INITIATED,
            mapOf(
                KEY_COUPON_DISCOUNT_TYPE to VALUE_COUPON_DISCOUNT_TYPE_FIXED_PRODUCT,
                KEY_HAS_EXPIRY_DATE to false,
                KEY_INCLUDES_FREE_SHIPPING to null,
                KEY_HAS_DESCRIPTION to false,
                KEY_HAS_PRODUCT_OR_CATEGORY_RESTRICTIONS to false,
                KEY_HAS_USAGE_RESTRICTIONS to false
            )
        )
    }

    @Test
    fun `given coupon creation mode and fixed cart type, when create clicked, should track event`() = testBlocking {
        setup(mode = Mode.Create(Coupon.Type.FixedCart))

        viewModel.onSaveClick()

        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.COUPON_CREATION_INITIATED,
            mapOf(
                KEY_COUPON_DISCOUNT_TYPE to VALUE_COUPON_DISCOUNT_TYPE_FIXED_CART,
                KEY_HAS_EXPIRY_DATE to false,
                KEY_INCLUDES_FREE_SHIPPING to null,
                KEY_HAS_DESCRIPTION to false,
                KEY_HAS_PRODUCT_OR_CATEGORY_RESTRICTIONS to false,
                KEY_HAS_USAGE_RESTRICTIONS to false
            )
        )
    }

    @Test
    fun `given coupon creation mode, when coupon created, should track event`() = testBlocking {
        setup(mode = Mode.Create(Coupon.Type.Percent)) {
            whenever(couponRepository.createCoupon(any())).thenReturn(Result.success(1L))
        }

        viewModel.onSaveClick()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.COUPON_CREATION_SUCCESS)
    }

    @Test
    fun `given coupon creation mode in POS, when coupon created, should track POS event`() = testBlocking {
        setup(mode = Mode.Create(Coupon.Type.Percent), isPOSMode = true) {
            whenever(couponRepository.createCoupon(any())).thenReturn(Result.success(1L))
        }

        viewModel.onSaveClick()

        verify(analyticsTrackerWrapper).track(WooPosAnalyticsEvent.Event.CouponCreationSuccess)
    }

    @Test
    fun `given coupon creation mode, when coupon creation fails, should track event`() =
        testBlocking {
            setup(mode = Mode.Create(Coupon.Type.Percent)) {
                whenever(couponRepository.createCoupon(any())).thenReturn(
                    Result.failure(WooException(WooError(TIMEOUT, UNKNOWN, "message")))
                )
            }

            viewModel.onSaveClick()

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.COUPON_CREATION_FAILED,
                EditCouponViewModel::class.java.simpleName,
                "TIMEOUT",
                "message"
            )
        }

    @Test
    fun `given coupon creation mode in POS, when coupon creation fails, should track POS event`() =
        testBlocking {
            setup(mode = Mode.Create(Coupon.Type.Percent), isPOSMode = true) {
                whenever(couponRepository.createCoupon(any())).thenReturn(
                    Result.failure(WooException(WooError(TIMEOUT, UNKNOWN, "message")))
                )
            }

            viewModel.onSaveClick()

            verify(analyticsTrackerWrapper).track(
                WooPosAnalyticsEvent.Event.CouponCreationFailed,
                EditCouponViewModel::class.java.simpleName,
                "TIMEOUT",
                "message"
            )
        }

    @Test
    fun `given isPOSMode true, when coupon created, should return to POS with new coupon id`() = testBlocking {
        setup(
            mode = Mode.Create(Coupon.Type.Percent),
            isPOSMode = true
        ) {
            whenever(couponRepository.createCoupon(any())).thenReturn(Result.success(1L))
        }

        viewModel.onSaveClick()

        assertThat(viewModel.event.value).isEqualTo(NavigateBackToPOS(1L))
    }
}
