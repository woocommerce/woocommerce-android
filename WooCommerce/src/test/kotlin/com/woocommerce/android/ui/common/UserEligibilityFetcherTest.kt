package com.woocommerce.android.ui.common

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCUserStore

@ExperimentalCoroutinesApi
class UserEligibilityFetcherTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val userStore: WCUserStore = mock()
    private val appPrefsWrapper: AppPrefs = mock()

    private lateinit var fetcher: UserEligibilityFetcher

    private val expectedUser = WCUserModel().apply {
        remoteUserId = 1L
        firstName = "Anitaa"
        lastName = "Murthy"
        username = "murthyanitaa"
        roles = "[author, editor]"
        email = "reallychumma1@gmail.com"
    }

    @Before
    fun setup() {
        doReturn(SiteModel()).whenever(selectedSite).get()

        fetcher = UserEligibilityFetcher(
            appPrefsWrapper,
            userStore,
            selectedSite
        )
    }

    @Test
    fun `Fetches user info correctly`() = testBlocking {
        whenever(userStore.fetchUserRole(any())).thenReturn(WooResult(expectedUser))

        fetcher.fetchUserInfo()

        verify(appPrefsWrapper).setUserEmail(expectedUser.email)
        verify(appPrefsWrapper).setIsUserEligible(expectedUser.isUserEligible())
    }

    @Test
    fun `Get user info from db correctly`() {
        doReturn(expectedUser).whenever(userStore).getUserByEmail(any(), any())
        doReturn(expectedUser.email).whenever(appPrefsWrapper).getUserEmail()

        val user = fetcher.getUser()

        assertThat(user).isEqualTo(expectedUser.toAppModel())
        assertThat(user?.isEligible).isEqualTo(expectedUser.isUserEligible())
        assertThat(user?.email).isEqualTo(expectedUser.email)
    }

    @Test
    fun `Do not update prefs when request failed`() = testBlocking {
        whenever(userStore.fetchUserRole(any())).thenReturn(WooResult(WooError(GENERIC_ERROR, UNKNOWN, "")))

        fetcher.fetchUserInfo()

        verify(appPrefsWrapper, never()).setUserEmail(any())
        verify(appPrefsWrapper, never()).setIsUserEligible(any())
    }
}
