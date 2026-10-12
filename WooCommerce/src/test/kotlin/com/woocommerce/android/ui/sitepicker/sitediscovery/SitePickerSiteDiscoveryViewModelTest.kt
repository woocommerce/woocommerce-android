package com.woocommerce.android.ui.sitepicker.sitediscovery

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.ui.sitepicker.SitePickerRepository
import com.woocommerce.android.util.UrlUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class SitePickerSiteDiscoveryViewModelTest : BaseUnitTest() {
    private val sitePickRepository: SitePickerRepository = mock()
    private val accountRepository: AccountRepository = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val unifiedLoginTracker: UnifiedLoginTracker = mock()
    private val urlUtils: UrlUtils = mock()

    private fun whenViewModelIsCreated(openedFromLogin: Boolean) {
        SitePickerSiteDiscoveryViewModel(
            savedStateHandle = SitePickerSiteDiscoveryFragmentArgs(openedFromLogin = openedFromLogin)
                .toSavedStateHandle(),
            sitePickRepository = sitePickRepository,
            accountRepository = accountRepository,
            resourceProvider = resourceProvider,
            analyticsTracker = analyticsTracker,
            unifiedLoginTracker = unifiedLoginTracker,
            urlUtils = urlUtils
        )
    }

    @Test
    fun `given the screen was opened from login, when it loads, then the site discovery flow is reported`() =
        testBlocking {
            // WHEN
            whenViewModelIsCreated(openedFromLogin = true)

            // THEN the flow matches what iOS reports for the same screen
            verify(unifiedLoginTracker).track(
                flow = eq(UnifiedLoginTracker.Flow.SITE_DISCOVERY),
                step = eq(UnifiedLoginTracker.Step.START),
                properties = any()
            )
        }

    @Test
    fun `given the screen was opened from the store switcher, when it loads, then no step is reported`() =
        testBlocking {
            // WHEN the merchant is connecting another store, which is not a login journey
            whenViewModelIsCreated(openedFromLogin = false)

            // THEN
            verify(unifiedLoginTracker, never()).track(
                flow = anyOrNull(),
                step = any(),
                properties = any()
            )
        }
}
