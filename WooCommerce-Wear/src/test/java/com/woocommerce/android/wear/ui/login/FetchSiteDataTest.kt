package com.woocommerce.android.wear.ui.login

import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.wear.phone.PhoneConnectionRepository
import com.woocommerce.android.wear.ui.login.FetchSiteData.LoginRequestState
import com.woocommerce.android.wear.ui.login.FetchSiteData.LoginRequestState.Logged
import com.woocommerce.android.wear.ui.login.FetchSiteData.LoginRequestState.Timeout
import com.woocommerce.android.wear.ui.login.FetchSiteData.LoginRequestState.Waiting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test

@ExperimentalCoroutinesApi
class FetchSiteDataTest : BaseUnitTest() {

    private val phoneRepository: PhoneConnectionRepository = mock()
    private val loginRepository: LoginRepository = mock()

    @Test
    fun `when user is logged in, return Logged state`() = testBlocking {
        // Given
        val events = mutableListOf<LoginRequestState>()
        whenever(phoneRepository.isPhoneConnectionAvailable()).thenReturn(true)
        whenever(loginRepository.isSiteAvailable).thenReturn(flowOf(true))

        // When
        FetchSiteData(phoneRepository, loginRepository)
            .invoke()
            .onEach { events.add(it) }
            .launchIn(this)

        // Then
        assertThat(events).isEqualTo(listOf(Logged))
    }

    @Test
    fun `when user is not logged in and waiting timeout, return Timeout state`() = testBlocking {
        // Given
        val events = mutableListOf<LoginRequestState>()
        whenever(phoneRepository.isPhoneConnectionAvailable()).thenReturn(true)
        whenever(loginRepository.isSiteAvailable).thenReturn(flowOf(false))

        // When
        FetchSiteData(phoneRepository, loginRepository)
            .invoke()
            .onEach { events.add(it) }
            .launchIn(this)

        // Then
        advanceUntilIdle()
        assertThat(events).isEqualTo(listOf(Waiting, Timeout))
    }

    @Test
    fun `when user is not logged in and waiting, return Waiting state`() = testBlocking {
        // Given
        val events = mutableListOf<LoginRequestState>()
        whenever(phoneRepository.isPhoneConnectionAvailable()).thenReturn(true)
        whenever(loginRepository.isSiteAvailable).thenReturn(flowOf(false))

        // When
        FetchSiteData(phoneRepository, loginRepository)
            .invoke()
            .onEach { events.add(it) }
            .launchIn(this)

        // Then
        assertThat(events).isEqualTo(listOf(Waiting))
    }
}
