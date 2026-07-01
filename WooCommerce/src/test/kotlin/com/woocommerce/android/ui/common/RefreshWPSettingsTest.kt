package com.woocommerce.android.ui.common

import com.woocommerce.android.model.User
import com.woocommerce.android.model.UserRole
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WPSettingsStore

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshWPSettingsTest : BaseUnitTest() {
    private val userEligibilityFetcher: UserEligibilityFetcher = mock()
    private val wpSettingsStore: WPSettingsStore = mock()
    private val refreshWPSettings = RefreshWPSettings(
        userEligibilityFetcher = userEligibilityFetcher,
        wpSettingsStore = wpSettingsStore
    )

    @Test
    fun `given administrator user, when refreshing wp settings, then settings are fetched`() = testBlocking {
        // GIVEN
        val site = SiteModel()
        whenever(userEligibilityFetcher.getUser()).thenReturn(userWithRoles(UserRole.Administrator))

        // WHEN
        refreshWPSettings(site)

        // THEN
        verifyBlocking(wpSettingsStore) {
            fetchSiteSettings(site)
        }
    }

    @Test
    fun `given shop manager user, when refreshing wp settings, then settings are not fetched`() = testBlocking {
        // GIVEN
        val site = SiteModel()
        whenever(userEligibilityFetcher.getUser()).thenReturn(userWithRoles(UserRole.ShopManager))

        // WHEN
        refreshWPSettings(site)

        // THEN
        verifyBlocking(wpSettingsStore, never()) {
            fetchSiteSettings(site)
        }
    }

    private fun userWithRoles(vararg roles: UserRole) = User(
        id = 1,
        firstName = "",
        lastName = "",
        username = "user",
        email = "user@example.com",
        roles = roles.toList()
    )
}
