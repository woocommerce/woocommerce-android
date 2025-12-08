package com.woocommerce.android.ui.login

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

@ExperimentalCoroutinesApi
class AgeCheckViewModelTest {

    private lateinit var viewModel: AgeCheckViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AgeCheckViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `checkAge with VERIFIED status updates isUnder13 to false`() = runTest {
        viewModel.checkAge(AgeCheckViewModel.USER_STATUS_VERIFIED)
        assertEquals(false, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with SUPERVISED status and ageUpper below 13 updates isUnder13 to true`() = runTest {
        viewModel.checkAge(AgeCheckViewModel.USER_STATUS_SUPERVISED, ageUpper = 12)
        assertEquals(true, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with SUPERVISED status and ageUpper 13 updates isUnder13 to false`() = runTest {
        viewModel.checkAge(AgeCheckViewModel.USER_STATUS_SUPERVISED, ageUpper = 13)
        assertEquals(false, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with SUPERVISED_APPROVAL_PENDING and ageUpper below 13 updates isUnder13 to true`() = runTest {
        viewModel.checkAge(AgeCheckViewModel.USER_STATUS_SUPERVISED_APPROVAL_PENDING, ageUpper = 10)
        assertEquals(true, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with UNKNOWN status updates isUnder13 to false`() = runTest {
        viewModel.checkAge(AgeCheckViewModel.USER_STATUS_UNKNOWN)
        assertEquals(false, viewModel.isUnder13.value)
    }

    @Test
    fun `checkAge with EMPTY status updates isUnder13 to false`() = runTest {
        viewModel.checkAge(AgeCheckViewModel.USER_STATUS_EMPTY)
        assertEquals(false, viewModel.isUnder13.value)
    }
}
