package com.woocommerce.android.ui.login

import android.content.Context
import com.google.android.gms.gass.AgeSignalsVerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class AgeCheckViewModelTest {

    private lateinit var viewModel: AgeCheckViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockContext: Context = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AgeCheckViewModel(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `checkAge with VERIFIED status updates isUnder13 to false`() = runTest {
        viewModel.checkAge(AgeSignalsVerificationStatus.VERIFIED)
        assertEquals(false, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with SUPERVISED status and ageUpper below 13 updates isUnder13 to true`() = runTest {
        viewModel.checkAge(AgeSignalsVerificationStatus.SUPERVISED, ageUpper = 12)
        assertEquals(true, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with SUPERVISED status and ageUpper 13 updates isUnder13 to false`() = runTest {
        viewModel.checkAge(AgeSignalsVerificationStatus.SUPERVISED, ageUpper = 13)
        assertEquals(false, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with SUPERVISED_APPROVAL_PENDING and ageUpper below 13 updates isUnder13 to true`() = runTest {
        viewModel.checkAge(AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING, ageUpper = 10)
        assertEquals(true, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with UNKNOWN status updates isUnder13 to false`() = runTest {
        viewModel.checkAge(AgeSignalsVerificationStatus.UNKNOWN)
        assertEquals(false, viewModel.isUnder13.value)
    }
}
