package com.woocommerce.android.ui.aisupportchat

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.BuildConfigWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class SupportChatContextProviderTest {
    private val selectedSite: SelectedSite = mock()
    private val buildConfigWrapper: BuildConfigWrapper = mock()

    private val contextProvider = SupportChatContextProvider(
        selectedSite = selectedSite,
        buildConfigWrapper = buildConfigWrapper
    )

    @Test
    fun `given no selected site, when building context, then site fields are omitted`() {
        whenever(selectedSite.getIfExists()).thenReturn(null)
        whenever(buildConfigWrapper.versionName).thenReturn(APP_VERSION)

        val context = contextProvider.buildInitialContext()

        assertThat(context["platform"].asString).isEqualTo("android")
        assertThat(context["app_version"].asString).isEqualTo(APP_VERSION)
        assertThat(context.has("site_id")).isFalse()
        assertThat(context.has("local_site_id")).isFalse()
        assertThat(context.has("site_url")).isFalse()
    }

    @Test
    fun `given selected site, when building context, then site fields are included`() {
        whenever(selectedSite.getIfExists()).thenReturn(
            SiteModel().apply {
                siteId = SITE_ID
                id = LOCAL_SITE_ID
                url = SITE_URL
            }
        )
        whenever(buildConfigWrapper.versionName).thenReturn(APP_VERSION)

        val context = contextProvider.buildInitialContext()

        assertThat(context["site_id"].asLong).isEqualTo(SITE_ID)
        assertThat(context["local_site_id"].asInt).isEqualTo(LOCAL_SITE_ID)
        assertThat(context["site_url"].asString).isEqualTo(SITE_URL)
    }

    private companion object {
        const val APP_VERSION = "1.2.3"
        const val SITE_ID = 123L
        const val LOCAL_SITE_ID = 456
        const val SITE_URL = "https://example.com"
    }
}
