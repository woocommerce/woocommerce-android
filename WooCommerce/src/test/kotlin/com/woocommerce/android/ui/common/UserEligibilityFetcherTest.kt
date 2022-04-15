package com.woocommerce.android.ui.common

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.store.WCUserStore
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

        fetcher = spy(
            UserEligibilityFetcher(
                appPrefsWrapper,
                userStore,
                selectedSite
            )
        )

        clearInvocations(
            appPrefsWrapper,
            selectedSite,
            userStore
        )
    }

    @Test
    fun `Fetches user info correctly`() = testBlocking {
        doReturn(expectedUser.isUserEligible()).whenever(appPrefsWrapper).isUserEligible()
        doReturn(expectedUser.email).whenever(appPrefsWrapper).getUserEmail()

        fetcher.fetchUserEligibility()

        assertThat(appPrefsWrapper.isUserEligible()).isEqualTo(expectedUser.isUserEligible())
        assertThat(appPrefsWrapper.getUserEmail()).isEqualTo(expectedUser.email)
        assertFalse(appPrefsWrapper.isUserEligible())
    }

    @Test
    fun `Get user info from db correctly`() {
        doReturn(expectedUser).whenever(userStore).getUserByEmail(any(), any())

        val user = fetcher.getUserByEmail(expectedUser.email)

        assertThat(user).isEqualTo(expectedUser)
        assertThat(user?.isUserEligible()).isEqualTo(expectedUser.isUserEligible())
        assertThat(user?.email).isEqualTo(expectedUser.email)
    }

    @Test
    fun `Do not update prefs when request failed`() = testBlocking {
        doReturn(true).whenever(appPrefsWrapper).isUserEligible()
        doReturn(null).whenever(appPrefsWrapper).getUserEmail()

        fetcher.fetchUserEligibility()

        // default value is set to true
        assertTrue(appPrefsWrapper.isUserEligible())

        // default value is null
        assertNull(appPrefsWrapper.getUserEmail())
    }
}
