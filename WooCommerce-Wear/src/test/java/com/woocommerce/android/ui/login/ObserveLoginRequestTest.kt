package com.woocommerce.android.ui.login

import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Failed
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Logged
import com.woocommerce.android.ui.login.ObserveLoginRequest.LoginRequestState.Waiting
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ObserveLoginRequestTest : BaseUnitTest() {

    private lateinit var sut: ObserveLoginRequest
    private val loginRepository: LoginRepository = mock()

    @Test
    fun `when user is logged in, return Logged state`() = testBlocking {
        // Given
        whenever(loginRepository.isUserLoggedIn).thenReturn(flowOf(true))

        // When
        sut = ObserveLoginRequest(loginRepository)

        // Then
        assertThat(sut.invoke().first()).isEqualTo(Logged)
    }

    @Test
    fun `when user is not logged in and not waiting, return Failed state`() = testBlocking {
        // Given
        val events = mutableListOf<ObserveLoginRequest.LoginRequestState>()
        whenever(loginRepository.isUserLoggedIn).thenReturn(flowOf(false))

        // When
        ObserveLoginRequest(loginRepository).invoke()
            .onEach { events.add(it) }
            .launchIn(this)

        // Then
        advanceUntilIdle()
        assertThat(events).isEqualTo(listOf(Waiting, Failed))
    }

    @Test
    fun `when user is not logged in and waiting, return Waiting state`() = testBlocking {
        // Given
        whenever(loginRepository.isUserLoggedIn).thenReturn(flowOf(false))

        // When
        sut = ObserveLoginRequest(loginRepository)

        // Then
        assertThat(sut.invoke().first()).isEqualTo(Waiting)
    }
}
