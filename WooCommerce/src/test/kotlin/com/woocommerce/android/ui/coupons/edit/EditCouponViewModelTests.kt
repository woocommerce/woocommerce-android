package com.woocommerce.android.ui.coupons.edit

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.coupons.CouponRepository
import com.woocommerce.android.ui.coupons.CouponTestUtils
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.OpenDescriptionEditor
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CouponUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.spy
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date
import java.util.concurrent.TimeUnit.DAYS

private const val COUPON_ID = 1L

class EditCouponViewModelTests : BaseUnitTest() {
    private lateinit var viewModel: EditCouponViewModel

    private var storedCoupon = CouponTestUtils.generateTestCoupon(COUPON_ID)

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

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()

        viewModel = EditCouponViewModel(
            savedStateHandle = EditCouponFragmentArgs(couponId = COUPON_ID).initSavedStateHandle(),
            couponRepository = couponRepository,
            couponUtils = couponUtils,
            parameterRepository = mock {
                on { getParameters(any(), any()) } doReturn siteParams
            }
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
    fun `when expiry date is changed, then update the coupon draft`() = testBlocking {
        val newDate = Date(System.currentTimeMillis() + DAYS.toMillis(1))
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onExpiryDateChanged(newDate)
        }.last()

        assertThat(state.couponDraft.dateExpires).isEqualTo(newDate)
    }

    @Test
    fun `when expiry date is removed, then update the coupon draft`() = testBlocking {
        storedCoupon = storedCoupon.copy(dateExpires = Date())
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onExpiryDateChanged(null)
        }.last()

        assertThat(state.couponDraft.dateExpires).isNull()
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

        verify(couponRepository).updateCoupon(argThat { code == "New code" })
    }

    @Test
    fun `when coupon is updated, then show a snackbar and navigate up`() = testBlocking {
        setup {
            whenever(couponRepository.updateCoupon(any())).thenReturn(Result.success(Unit))
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
            whenever(couponRepository.updateCoupon(any())).thenReturn(Result.failure(Exception()))
        }

        viewModel.onSaveClick()

        val event = viewModel.event.captureValues().last()
        assertThat(event).isEqualTo(ShowUiStringSnackbar(UiStringRes(R.string.coupon_edit_coupon_update_failed)))
    }
}
