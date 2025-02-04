package com.woocommerce.android.ui.woopos.emailreceipt

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosEmailReceiptViewModelTest {
    @get:Rule
    val coroutinesTestRule = WooPosCoroutineTestRule()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val repository: WooPosEmailReceiptRepository = mock()
    private val resourceProvider: ResourceProvider = mock()

    private lateinit var viewModel: WooPosEmailReceiptViewModel

    @Before
    fun setUp() {
        val savedStateHandle = SavedStateHandle(
            mapOf(EMAIL_RECEIPT_ROUTE_ORDER_ID_KEY to 123L)
        )

        whenever(resourceProvider.getString(R.string.woopos_email_receipt_send_button))
            .thenReturn("Send")
        whenever(resourceProvider.getString(R.string.woopos_email_receipt_send_error))
            .thenReturn("Error sending email")

        whenever(repository.isEmailValid(any())).thenReturn(false)

        viewModel = WooPosEmailReceiptViewModel(
            repository = repository,
            resourceProvider = resourceProvider,
            savedState = savedStateHandle
        )
    }

    @Test
    fun `given initial state, when view model is created, then state should be Email with empty email and disabled button`() = runTest {
        // GIVEN

        // WHEN
        val state = viewModel.state.first()

        // THEN
        assertThat(state).isInstanceOf(WooPosEmailReceiptState.Email::class.java)
        val emailState = state as WooPosEmailReceiptState.Email
        assertThat(emailState.email).isEmpty()
        assertThat(emailState.errorMessage).isNull()
        assertThat(emailState.button.status).isEqualTo(WooPosEmailReceiptState.Email.Button.Status.DISABLED)
    }

    @Test
    fun `given invalid email, when EmailChanged called, then button should remain disabled`() = runTest {
        // GIVEN
        whenever(repository.isEmailValid("invalid")).thenReturn(false)

        // WHEN
        viewModel.onUIEvent(WooPosEmailReceiptUIEvent.EmailChanged("invalid"))
        val state = viewModel.state.first()

        // THEN
        val emailState = state as WooPosEmailReceiptState.Email
        assertThat(emailState.email).isEqualTo("invalid")
        assertThat(emailState.button.status).isEqualTo(WooPosEmailReceiptState.Email.Button.Status.DISABLED)
    }

    @Test
    fun `given valid email, when EmailChanged called, then button should become enabled`() = runTest {
        // GIVEN
        whenever(repository.isEmailValid("valid@example.com")).thenReturn(true)

        // WHEN
        viewModel.onUIEvent(WooPosEmailReceiptUIEvent.EmailChanged("valid@example.com"))
        val state = viewModel.state.first()

        // THEN
        val emailState = state as WooPosEmailReceiptState.Email
        assertThat(emailState.email).isEqualTo("valid@example.com")
        assertThat(emailState.errorMessage).isNull()
        assertThat(emailState.button.status).isEqualTo(WooPosEmailReceiptState.Email.Button.Status.ENABLED)
    }

    @Test
    fun `given valid email and successful send, when SendEmailClicked called, then state should become Sent`() = runTest {
        // GIVEN
        whenever(repository.isEmailValid("valid@example.com")).thenReturn(true)
        viewModel.onUIEvent(WooPosEmailReceiptUIEvent.EmailChanged("valid@example.com"))
        whenever(repository.sendReceiptByEmail(orderId = 123L, "valid@example.com"))
            .thenReturn(Result.success(Unit))

        // WHEN
        viewModel.onUIEvent(WooPosEmailReceiptUIEvent.SendEmailClicked)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosEmailReceiptState.Sent::class.java)
    }

    @Test
    fun `given valid email and failed send, when SendEmailClicked called, then show error message and re-enable button`() = runTest {
        // GIVEN
        whenever(repository.isEmailValid("valid@example.com")).thenReturn(true)
        viewModel.onUIEvent(WooPosEmailReceiptUIEvent.EmailChanged("valid@example.com"))
        whenever(repository.sendReceiptByEmail(orderId = 123L, "valid@example.com"))
            .thenReturn(Result.failure(RuntimeException("Failed")))

        // WHEN
        viewModel.onUIEvent(WooPosEmailReceiptUIEvent.SendEmailClicked)
        advanceUntilIdle()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosEmailReceiptState.Email::class.java)
        val emailState = state as WooPosEmailReceiptState.Email
        assertThat(emailState.errorMessage).isEqualTo("Error sending email")
        assertThat(emailState.button.status).isEqualTo(WooPosEmailReceiptState.Email.Button.Status.ENABLED)
    }
}
