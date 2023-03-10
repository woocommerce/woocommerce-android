package com.woocommerce.android.support

import com.woocommerce.android.support.ZendeskException.IdentityNotSetException
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.store.SiteStore

@OptIn(ExperimentalCoroutinesApi::class)
internal class ZendeskManagerTest : BaseUnitTest() {
    private lateinit var sut: ZendeskManager
    private lateinit var zendeskSettings: ZendeskSettings
    private lateinit var siteStore: SiteStore

    @Before
    fun setup() {
        zendeskSettings = mock {
            on { isIdentitySet } doReturn true
        }
        siteStore = mock {
            on { sites } doReturn emptyList()
        }
        createSUT()
    }

    @Test
    fun `when createRequest is called with no identity set, then an result with IdentityNotSetException is emitted`()
    = testBlocking {
        // Given
        zendeskSettings = mock { on { isIdentitySet } doReturn false }
        createSUT()

        // When
        val result = sut.createRequest(
            context = mock(),
            origin = HelpOrigin.LOGIN_HELP_NOTIFICATION,
            ticketType = TicketType.MobileApp,
            selectedSite = mock(),
            subject = "subject",
            description = "description",
            extraTags = emptyList()
        ).single()

        // Then
        assertThat(result).isNotNull
        assertThat(result.isFailure).isTrue
        assertThat(result.exceptionOrNull()).isEqualTo(IdentityNotSetException)
    }

    private fun createSUT() {
        sut = ZendeskManager(
            zendeskSettings = zendeskSettings,
            siteStore = siteStore,
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }
}
