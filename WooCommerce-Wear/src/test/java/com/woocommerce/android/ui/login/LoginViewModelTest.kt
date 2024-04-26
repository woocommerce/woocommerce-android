package com.woocommerce.android.ui.login

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.ui.NavRoutes
import com.woocommerce.commons.wear.MessagePath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class LoginViewModelTest : BaseUnitTest() {

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
        verify(navController).navigate(NavRoutes.MY_STORE.route)
    }

    @Test
    fun `when user is not logged in, loading is stopped`() = testBlocking {
        // Given
        var isLoading: Boolean? = null
        whenever(loginRepository.isUserLoggedIn).thenReturn(flowOf(false))
        createSut()
        sut.viewState.observeForever { isLoading = it.isLoading }

        // Then
        assertThat(isLoading).isNotNull()
        assertThat(isLoading).isFalse
    }

    @Test
    fun `on login button clicked, loading is started and message is sent`() = testBlocking {
        // Given
        whenever(loginRepository.isUserLoggedIn).thenReturn(flowOf(false))
        createSut()

        // When
        sut.onLoginButtonClicked()

        // Then
        verify(phoneConnectionRepository).sendMessage(MessagePath.REQUEST_SITE)
    }

    @Test
    fun `on login button clicked, loading is started`() = testBlocking {
        // Given
        var isLoading: Boolean? = null
        whenever(loginRepository.isUserLoggedIn).thenReturn(flowOf(false))
        createSut()
        sut.viewState.observeForever { isLoading = it.isLoading }

        // When
        sut.onLoginButtonClicked()

        // Then
        assertThat(isLoading).isNotNull()
        assertThat(isLoading).isTrue
    }

    private fun createSut() {
        sut = LoginViewModel(
            loginRepository,
            phoneConnectionRepository,
            navController,
            SavedStateHandle()
        )
    }
}
