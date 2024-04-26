package com.woocommerce.android.ui

import androidx.navigation.NavHostController
import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.android.ui.login.LoginViewModel
import com.woocommerce.commons.wear.MessagePath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class LoginViewModelTest: BaseUnitTest() {

    private lateinit var sut: LoginViewModel
    private val loginRepository: LoginRepository = mock()
    private val phoneConnectionRepository: PhoneConnectionRepository = mock()
    private val navController: NavHostController = mock()

    @Test
    fun `when user is logged in, navigate to MY_STORE`() = testBlocking {
        // Given
        whenever(loginRepository.isUserLoggedIn).thenReturn(flowOf(true))

        // When
        createSut()

        // Then
        assertEquals(NavRoutes.MY_STORE.route, navController.currentDestination?.route)
    }

    @Test
    fun `when user is not logged in, loading is stopped`() = testBlocking {
        // Given
        whenever(loginRepository.isUserLoggedIn).thenReturn(flowOf(true))

        // When
        createSut()

        // Then
        assertEquals(false, sut.viewState.value?.isLoading)
    }

    @Test
    fun `on login button clicked, loading is started and message is sent`() = testBlocking {
        // Given
        createSut()

        // When
        sut.onLoginButtonClicked()

        // Then
        assertEquals(true, sut.viewState.value?.isLoading)
        verify(phoneConnectionRepository).sendMessage(MessagePath.REQUEST_SITE)
    }

    @Test
    fun `on login button clicked, loading is stopped after message is sent`() = testBlocking {
        // Given
        createSut()

        // When
        sut.onLoginButtonClicked()

        // Then
        assertEquals(false, sut.viewState.value?.isLoading)
    }

    private fun createSut() {
        sut = LoginViewModel(
            loginRepository,
            phoneConnectionRepository,
            navController,
            mock()
        )
    }
}
