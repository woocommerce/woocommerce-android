package com.woocommerce.android.ui.common

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCUserStore
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
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
            ))

        clearInvocations(
            appPrefsWrapper,
            selectedSite,
            userStore
        )
    }

    @Test
    fun `Fetches user info correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(WooResult(expectedUser)).whenever(userStore).fetchUserRole(any())
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
    fun `Do not update prefs when request failed`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(
            WooResult<WooError>(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.UNKNOWN))
        ).whenever(userStore).fetchUserRole(any())
        doReturn(true).whenever(appPrefsWrapper).isUserEligible()
        doReturn(null).whenever(appPrefsWrapper).getUserEmail()

        fetcher.fetchUserEligibility()

        // default value is set to true
        assertTrue(appPrefsWrapper.isUserEligible())

        // default value is null
        assertNull(appPrefsWrapper.getUserEmail())
    }
}
