package com.woocommerce.android.wear.ui.login

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.wear.ui.login.FetchSiteData.LoginRequestState.Logged
import com.woocommerce.android.wear.ui.login.FetchSiteData.LoginRequestState.Timeout
import com.woocommerce.android.wear.ui.login.FetchSiteData.LoginRequestState.Waiting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class LoginViewModelTest : BaseUnitTest() {

    private lateinit var sut: LoginViewModel
    private val fetchSiteData: FetchSiteData = mock()

    @Test
    fun `when login Timeouts, then view state is Timeout`() = testBlocking {
        // Given
        var loginState: FetchSiteData.LoginRequestState? = null
        whenever(fetchSiteData.invoke()).thenReturn(flowOf(Timeout))
        createSut()
        sut.viewState.observeForever { loginState = it.loginState }

        // Then
        assertThat(loginState).isNotNull()
        assertThat(loginState).isEqualTo(Timeout)
    }

    @Test
    fun `when login is Logged, then view state is Logged`() = testBlocking {
        // Given
        var loginState: FetchSiteData.LoginRequestState? = null
        whenever(fetchSiteData.invoke()).thenReturn(flowOf(Logged))
        createSut()
        sut.viewState.observeForever { loginState = it.loginState }

        // Then
        assertThat(loginState).isNotNull()
        assertThat(loginState).isEqualTo(Logged)
    }

    @Test
    fun `when login is Waiting, then view state is Waiting`() = testBlocking {
        // Given
        var loginState: FetchSiteData.LoginRequestState? = null
        whenever(fetchSiteData.invoke()).thenReturn(flowOf(Waiting))
        createSut()
        sut.viewState.observeForever { loginState = it.loginState }

        // Then
        assertThat(loginState).isNotNull()
        assertThat(loginState).isEqualTo(Waiting)
    }



    private fun createSut() {
        sut = LoginViewModel(
            fetchSiteData,
            mock(),
            SavedStateHandle()
        )
    }
}
