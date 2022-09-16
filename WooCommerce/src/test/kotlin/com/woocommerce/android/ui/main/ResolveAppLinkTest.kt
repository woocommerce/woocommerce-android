package com.woocommerce.android.ui.main

import android.net.Uri
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PATH
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_URL
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class ResolveAppLinkTest {

    private lateinit var sut: ResolveAppLink
    private val selectedSite = mock<SelectedSite>()
    private val tracker = mock<AnalyticsTrackerWrapper>()

    @Before
    fun setUp() {
        sut = ResolveAppLink(selectedSite, mock(), tracker)
    }

    @Test
    fun `do nothing if there's no deep link`() {
        val result = sut(null)

        assertThat(result).isEqualTo(ResolveAppLink.Action.DoNothing)
        verifyNoInteractions(tracker)
    }

    @Test
    fun `do nothing if deep link is not supported`() {
        val result = sut(
            mock {
                on { path } doReturn "some/random/not/supported/path"
            }
        )

        assertThat(result).isEqualTo(ResolveAppLink.Action.DoNothing)
        verifyNoInteractions(tracker)
    }

    @Test
    fun `view stats if there's not selected site`() {
        whenever(selectedSite.exists()).thenReturn(false)

        val uri = mockUri()
        val result = sut(uri)

        assertThat(result).isEqualTo(ResolveAppLink.Action.ViewStats)
        verify(tracker).track(AnalyticsEvent.UNIVERSAL_LINK_FAILED, mapOf(KEY_URL to uri.toString()))
    }

    @Test
    fun `view stats when query parameter is malformed`() {
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(selectedSite.getIfExists()).thenReturn(
            SiteModel().apply {
                siteId = TEST_BLOG_ID
            }
        )

        val uri = mockUri(orderId = "not_a_number")
        val result = sut(uri)

        assertThat(result).isEqualTo(ResolveAppLink.Action.ViewStats)
        verify(tracker).track(AnalyticsEvent.UNIVERSAL_LINK_FAILED, mapOf(KEY_URL to uri.toString()))
    }

    @Test
    fun `view order details event when app link contained valid data`() {
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(selectedSite.getIfExists()).thenReturn(
            SiteModel().apply {
                siteId = TEST_BLOG_ID
            }
        )

        val uri = mockUri()
        val result = sut(uri)

        assertThat(result).isEqualTo(ResolveAppLink.Action.ViewOrderDetail(TEST_ORDER_ID))
        verify(tracker).track(AnalyticsEvent.UNIVERSAL_LINK_OPENED, mapOf(KEY_PATH to uri.path))
    }

    @Test
    fun `restart activity event when app link points to a different than selected blog id`() {
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(selectedSite.getIfExists()).thenReturn(
            SiteModel().apply {
                siteId = 987
            }
        )

        val uri = mockUri()
        val result = sut(uri)

        assertThat(result).isEqualTo(ResolveAppLink.Action.ChangeSiteAndRestart(TEST_BLOG_ID, uri))
        verifyNoInteractions(tracker)
    }

    private fun mockUri(orderId: String = TEST_ORDER_ID.toString(), blogId: String = TEST_BLOG_ID.toString()): Uri {
        val uri = mock<Uri> {
            on { path } doReturn "orders/details"
            on { getQueryParameter("order_id") } doReturn orderId
            on { getQueryParameter("blog_id") } doReturn blogId
        }
        return uri
    }

    private companion object {
        const val TEST_ORDER_ID = 123L
        const val TEST_BLOG_ID = 345L
    }
}
