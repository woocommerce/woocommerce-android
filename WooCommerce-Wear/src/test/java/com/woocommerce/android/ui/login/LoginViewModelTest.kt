package com.woocommerce.android.ui.login

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.ui.NavRoutes
import com.woocommerce.commons.wear.MessagePath.REQUEST_SITE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
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
        verify(navController).navigate(
            eq(NavRoutes.MY_STORE.route),
            builder = any()
        )
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
        sut.onTryAgainClicked()

        // Then
        verify(phoneConnectionRepository, times(2)).sendMessage(REQUEST_SITE)
    }

    @Test
    fun `on login button clicked, loading is started`() = testBlocking {
        // Given
        var isLoading: Boolean? = null
        whenever(loginRepository.isUserLoggedIn).thenReturn(flowOf(false))
        createSut()
        sut.viewState.observeForever { isLoading = it.isLoading }

        // When
        sut.onTryAgainClicked()

        // Then
        assertThat(isLoading).isNotNull()
        assertThat(isLoading).isTrue
    }

    @Test
    fun `on viewModel init, then send REQUEST_SITE message and observe the logged in status`() = testBlocking {
        // Given
        whenever(loginRepository.isUserLoggedIn).thenReturn(flowOf(false))
        whenever(phoneConnectionRepository.sendMessage(REQUEST_SITE))
            .doReturn(Result.success(Unit))

        // When
        createSut()

        // Then
        verify(phoneConnectionRepository).sendMessage(REQUEST_SITE)
        verify(loginRepository).isUserLoggedIn
    }

    @Test
    fun `on viewModel init, then fail REQUEST_SITE message and cancel loading`() = testBlocking {
        // Given
        var isLoading: Boolean? = null
        whenever(phoneConnectionRepository.sendMessage(REQUEST_SITE))
            .doReturn(Result.failure(Exception("")))

        // When
        createSut()
        sut.viewState.observeForever { isLoading = it.isLoading }

        // Then
        verify(phoneConnectionRepository).sendMessage(REQUEST_SITE)
        verify(loginRepository, never()).isUserLoggedIn
        assertThat(isLoading).isNotNull()
        assertThat(isLoading).isFalse
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
