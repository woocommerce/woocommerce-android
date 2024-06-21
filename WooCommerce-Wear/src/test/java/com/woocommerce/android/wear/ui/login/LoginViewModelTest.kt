package com.woocommerce.android.wear.ui.login

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.wear.ui.NavRoutes
import com.woocommerce.android.wear.ui.login.FetchSiteData.LoginRequestState.Logged
import com.woocommerce.android.wear.ui.login.FetchSiteData.LoginRequestState.Timeout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class LoginViewModelTest : BaseUnitTest() {

    private lateinit var sut: LoginViewModel
    private val fetchSiteData: FetchSiteData = mock()
    private val navController: NavHostController = mock()

    @Test
    fun `when user is logged in, navigate to MY_STORE`() = testBlocking {
        // Given
        whenever(fetchSiteData.invoke()).thenReturn(flowOf(Logged))

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
        whenever(fetchSiteData.invoke()).thenReturn(flowOf(Timeout))
        createSut()
        sut.viewState.observeForever { isLoading = it.isLoading }

        // Then
        assertThat(isLoading).isNotNull()
        assertThat(isLoading).isFalse
    }

    @Test
    fun `when login is waiting, then view state is loading`() = testBlocking {
        // Given
        var isLoading: Boolean? = null
        whenever(fetchSiteData.invoke()).thenReturn(flowOf(Timeout))
        createSut()
        sut.viewState.observeForever { isLoading = it.isLoading }

        // Then
        assertThat(isLoading).isNotNull()
        assertThat(isLoading).isFalse
    }

    private fun createSut() {
        sut = LoginViewModel(
            fetchSiteData,
            mock(),
            navController,
            SavedStateHandle()
        )
    }
}
