package com.woocommerce.android.ui.subscriptions

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.SubscriptionProductCreationSettingsEntity
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class GetSubscriptionProductCreationStatusTest : BaseUnitTest() {
    private lateinit var sut: GetSubscriptionProductCreationStatus
    private lateinit var isEligibleForSubscriptions: IsEligibleForSubscriptions
    private lateinit var wooCommerceStore: WooCommerceStore
    private val siteModel = SiteModel()

    @Before
    fun setUp() {
        isEligibleForSubscriptions = mock()
        wooCommerceStore = mock()
        val selectedSite: SelectedSite = mock {
            on { get() }.thenReturn(siteModel)
        }
        sut = GetSubscriptionProductCreationStatus(isEligibleForSubscriptions, selectedSite, wooCommerceStore)
    }

    @Test
    fun `given subscriptions plugin is not active, when invoked, then both types are not creatable`() = testBlocking {
        whenever(isEligibleForSubscriptions()).thenReturn(false)

        val result = sut()

        assertThat(result.isSimpleSubscriptionCreatable).isFalse()
        assertThat(result.isVariableSubscriptionCreatable).isFalse()
    }

    @Test
    fun `given cached settings exist, when invoked, then cached values are used without fetching`() = testBlocking {
        whenever(isEligibleForSubscriptions()).thenReturn(true)
        stubCachedSettings(simpleEnabled = true, variableEnabled = false)

        val result = sut()

        assertThat(result.isSimpleSubscriptionCreatable).isTrue()
        assertThat(result.isVariableSubscriptionCreatable).isFalse()
        verify(wooCommerceStore, never()).fetchSubscriptionProductCreationSettings(siteModel)
    }

    @Test
    fun `given no cached settings, when invoked, then settings are fetched and used`() = testBlocking {
        whenever(isEligibleForSubscriptions()).thenReturn(true)
        whenever(wooCommerceStore.getSubscriptionProductCreationSettings(siteModel)).thenReturn(null)
        whenever(wooCommerceStore.fetchSubscriptionProductCreationSettings(siteModel)).thenReturn(
            WooResult(
                SubscriptionProductCreationSettingsEntity(
                    localSiteId = siteModel.localId(),
                    isSimpleSubscriptionCreationEnabled = false,
                    isVariableSubscriptionCreationEnabled = true
                )
            )
        )

        val result = sut()

        assertThat(result.isSimpleSubscriptionCreatable).isFalse()
        assertThat(result.isVariableSubscriptionCreatable).isTrue()
    }

    @Test
    fun `given no cached settings and fetch fails, when invoked, then both types stay creatable`() = testBlocking {
        whenever(isEligibleForSubscriptions()).thenReturn(true)
        whenever(wooCommerceStore.getSubscriptionProductCreationSettings(siteModel)).thenReturn(null)
        whenever(wooCommerceStore.fetchSubscriptionProductCreationSettings(siteModel))
            .thenReturn(WooResult(WooError(GENERIC_ERROR, UNKNOWN)))

        val result = sut()

        assertThat(result.isSimpleSubscriptionCreatable).isTrue()
        assertThat(result.isVariableSubscriptionCreatable).isTrue()
    }

    @Test
    fun `given site does not report the settings, when invoked, then both types stay creatable`() = testBlocking {
        whenever(isEligibleForSubscriptions()).thenReturn(true)
        stubCachedSettings(simpleEnabled = null, variableEnabled = null)

        val result = sut()

        assertThat(result.isSimpleSubscriptionCreatable).isTrue()
        assertThat(result.isVariableSubscriptionCreatable).isTrue()
    }

    @Test
    fun `given both settings disabled, when invoked, then both types are not creatable`() = testBlocking {
        whenever(isEligibleForSubscriptions()).thenReturn(true)
        stubCachedSettings(simpleEnabled = false, variableEnabled = false)

        val result = sut()

        assertThat(result.isSimpleSubscriptionCreatable).isFalse()
        assertThat(result.isVariableSubscriptionCreatable).isFalse()
    }

    @Test
    fun `given only variable setting enabled, when invoked, then only variable type is creatable`() = testBlocking {
        whenever(isEligibleForSubscriptions()).thenReturn(true)
        stubCachedSettings(simpleEnabled = false, variableEnabled = true)

        val result = sut()

        assertThat(result.isSimpleSubscriptionCreatable).isFalse()
        assertThat(result.isVariableSubscriptionCreatable).isTrue()
    }

    @Test
    fun `given both settings enabled, when invoked, then both types are creatable`() = testBlocking {
        whenever(isEligibleForSubscriptions()).thenReturn(true)
        stubCachedSettings(simpleEnabled = true, variableEnabled = true)

        val result = sut()

        assertThat(result.isSimpleSubscriptionCreatable).isTrue()
        assertThat(result.isVariableSubscriptionCreatable).isTrue()
    }

    private suspend fun stubCachedSettings(simpleEnabled: Boolean?, variableEnabled: Boolean?) {
        whenever(wooCommerceStore.getSubscriptionProductCreationSettings(siteModel)).thenReturn(
            SubscriptionProductCreationSettingsEntity(
                localSiteId = siteModel.localId(),
                isSimpleSubscriptionCreationEnabled = simpleEnabled,
                isVariableSubscriptionCreationEnabled = variableEnabled
            )
        )
    }
}
