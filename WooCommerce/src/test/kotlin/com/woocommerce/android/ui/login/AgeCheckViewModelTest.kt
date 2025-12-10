package com.woocommerce.android.ui.login

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AgeCheckViewModelTest : BaseUnitTest() {

    private lateinit var viewModel: AgeCheckViewModel
    private val context: Context = mock()

    @Before
    fun setup() {
        viewModel = AgeCheckViewModel(
            savedStateHandle = SavedStateHandle(),
            context,
        )
    }

    @Test
    fun `checkAge with VERIFIED status updates isUnder13 to false`() = testBlocking {
        viewModel.checkAge(AgeSignalsVerificationStatus.VERIFIED)
        assertEquals(false, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with SUPERVISED status and ageUpper below 13 updates isUnder13 to true`() = testBlocking {
        viewModel.checkAge(AgeSignalsVerificationStatus.SUPERVISED, ageUpper = 12)
        assertEquals(true, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with SUPERVISED status and ageUpper 13 updates isUnder13 to false`() = testBlocking {
        viewModel.checkAge(AgeSignalsVerificationStatus.SUPERVISED, ageUpper = 13)
        assertEquals(false, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with SUPERVISED_APPROVAL_PENDING and ageUpper below 13 updates isUnder13 to true`() = testBlocking {
        viewModel.checkAge(AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING, ageUpper = 10)
        assertEquals(true, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with SUPERVISED_APPROVAL_PENDING and ageUpper 13 updates isUnder13 to false`() = testBlocking {
        viewModel.checkAge(AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING, ageUpper = 13)
        assertEquals(false, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with SUPERVISED_APPROVAL_DENIED and ageUpper below 13 updates isUnder13 to true`() = testBlocking {
        viewModel.checkAge(AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED, ageUpper = 12)
        assertEquals(true, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with SUPERVISED_APPROVAL_DENIED and ageUpper 13 updates isUnder13 to false`() = testBlocking {
        viewModel.checkAge(AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED, ageUpper = 13)
        assertEquals(false, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with SUPERVISED and null ageUpper updates isUnder13 to false`() = testBlocking {
        viewModel.checkAge(AgeSignalsVerificationStatus.SUPERVISED, ageUpper = null)
        assertEquals(false, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge using mocked Client with SUPERVISED_APPROVAL_PENDING updates isUnder13 correctly`() = testBlocking {
        val mockClient = mock<AgeSignalsClient>()
        val result = AgeCheckResult(
            userStatus = AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING,
            ageUpper = 17
        )
        whenever(mockClient.checkAge()).thenReturn(result)

        viewModel.client = mockClient
        viewModel.checkAge()

        assertEquals(false, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with UNKNOWN status updates isUnder13 to false`() = testBlocking {
        viewModel.checkAge(AgeSignalsVerificationStatus.UNKNOWN)
        assertEquals(false, viewModel.isUnder13.value)
    }
}
