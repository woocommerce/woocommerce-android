package com.woocommerce.android.ui.aiassistant.appfunctions

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.aiassistant.appfunctions.StoreAppFunctionsAvailability
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooAppFunctionsAccessTest : BaseUnitTest() {
    private val selectedSiteState = MutableStateFlow<SiteModel?>(store())
    private val selectedSite: SelectedSite = mock {
        on { observe() } doReturn selectedSiteState
        on { getIfExists() } doAnswer { selectedSiteState.value }
    }
    private val accountRepository: AccountRepository = mock {
        on { isUserLoggedIn() } doReturn true
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        on { isAppFunctionsEnabled } doReturn true
    }
    private val buildConfigWrapper: BuildConfigWrapper = mock {
        on { debug } doReturn true
    }
    private val availability: StoreAppFunctionsAvailability = mock()
    private lateinit var appScope: CoroutineScope
    private lateinit var access: WooAppFunctionsAccess

    @Before
    fun setUp() {
        appScope = TestScope(coroutinesTestRule.testDispatcher)
        access = createAccess()
    }

    @Test
    fun `given a debug build, an open store, and the switch is on, when access is checked, then it is allowed and functions are enabled`() = testBlocking {
        access.init()

        assertThat(access.isAllowed()).isTrue()
        verifyBlocking(availability) { setEnabled(true) }
    }

    @Test
    fun `given a release build, when access is checked, then access is denied without calling the system`() = testBlocking {
        whenever(buildConfigWrapper.debug).thenReturn(false)
        access.init()

        assertThat(access.isAllowed()).isFalse()
        verifyBlocking(availability, never()) { setEnabled(any()) }
    }

    @Test
    fun `given the user is logged out while a store remains selected, when access is checked, then access is denied`() {
        whenever(accountRepository.isUserLoggedIn()).thenReturn(false)

        assertThat(access.isAllowed()).isFalse()
    }

    @Test
    fun `given no store is selected, when access is checked, then access is denied and functions are disabled`() = testBlocking {
        selectedSiteState.value = null
        access.init()

        assertThat(access.isAllowed()).isFalse()
        verifyBlocking(availability) { setEnabled(false) }
    }

    @Test
    fun `given the switch is off, when access is checked, then access is denied and functions are disabled`() = testBlocking {
        whenever(appPrefsWrapper.isAppFunctionsEnabled).thenReturn(false)
        access = createAccess()
        access.init()

        assertThat(access.isAllowed()).isFalse()
        verifyBlocking(availability) { setEnabled(false) }
    }

    @Test
    fun `when the merchant turns the switch off, then it is saved and functions are disabled`() = testBlocking {
        access.init()
        access.onMerchantSwitchChanged(false)

        verify(appPrefsWrapper).isAppFunctionsEnabled = false
        assertThat(access.isAllowed()).isFalse()
        verifyBlocking(availability) { setEnabled(false) }
    }

    @Test
    fun `when the merchant turns the switch back on, then functions are enabled again`() = testBlocking {
        access.init()
        clearInvocations(availability)
        access.onMerchantSwitchChanged(false)
        access.onMerchantSwitchChanged(true)

        assertThat(access.isAllowed()).isTrue()
        verifyBlocking(availability) { setEnabled(true) }
    }

    @Test
    fun `when the selected store is cleared, then functions are disabled`() = testBlocking {
        access.init()
        selectedSiteState.value = null

        verifyBlocking(availability) { setEnabled(false) }
    }

    private fun createAccess() = WooAppFunctionsAccess(
        selectedSite = selectedSite,
        accountRepository = accountRepository,
        appPrefsWrapper = appPrefsWrapper,
        buildConfigWrapper = buildConfigWrapper,
        storeAppFunctionsAvailability = availability,
        appCoroutineScope = appScope,
    )

    private companion object {
        fun store() = SiteModel().apply {
            id = 7
            url = "https://bean.example"
        }
    }
}
