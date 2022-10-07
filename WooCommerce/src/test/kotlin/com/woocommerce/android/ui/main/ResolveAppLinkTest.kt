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
    fun `for order details link view stats if there's not selected site`() {
        whenever(selectedSite.exists()).thenReturn(false)

        val uri = mockOrderDetailsUri()
        val result = sut(uri)

        assertThat(result).isEqualTo(ResolveAppLink.Action.ViewStats)
        verify(tracker).track(AnalyticsEvent.UNIVERSAL_LINK_FAILED, mapOf(KEY_URL to uri.toString()))
    }

    @Test
    fun `for order details link view stats when query parameter is malformed`() {
        mockSiteSelected()

        val uri = mockOrderDetailsUri(orderId = "not_a_number")
        val result = sut(uri)

        assertThat(result).isEqualTo(ResolveAppLink.Action.ViewStats)
        verify(tracker).track(AnalyticsEvent.UNIVERSAL_LINK_FAILED, mapOf(KEY_URL to uri.toString()))
    }

    @Test
    fun `for order details link view order details event when app link contained valid data`() {
        mockSiteSelected()

        val uri = mockOrderDetailsUri()
        val result = sut(uri)

        assertThat(result).isEqualTo(ResolveAppLink.Action.ViewOrderDetail(TEST_ORDER_ID))
        verify(tracker).track(AnalyticsEvent.UNIVERSAL_LINK_OPENED, mapOf(KEY_PATH to uri.path))
    }

    @Test
    fun `for order details link restart activity event when link points to a different than selected blog id`() {
        mockSiteSelected(mockedSiteId = 987)

        val uri = mockOrderDetailsUri()
        val result = sut(uri)

        assertThat(result).isEqualTo(ResolveAppLink.Action.ChangeSiteAndRestart(TEST_BLOG_ID, uri))
        verifyNoInteractions(tracker)
    }

    @Test
    fun `for payments link view stats if there's not selected site`() {
        whenever(selectedSite.exists()).thenReturn(false)

        val uri = mockPaymentsUri()
        val result = sut(uri)

        assertThat(result).isEqualTo(ResolveAppLink.Action.ViewStats)
        verify(tracker).track(AnalyticsEvent.UNIVERSAL_LINK_FAILED, mapOf(KEY_URL to uri.toString()))
    }

    @Test
    fun `for payments link view stats when query parameter is malformed`() {
        mockSiteSelected()

        val uri = mockPaymentsUri(blogId = "not_a_number")
        val result = sut(uri)

        assertThat(result).isEqualTo(ResolveAppLink.Action.ViewStats)
        verify(tracker).track(AnalyticsEvent.UNIVERSAL_LINK_FAILED, mapOf(KEY_URL to uri.toString()))
    }

    @Test
    fun `for payments link view payments screen when app link contained valid data`() {
        mockSiteSelected()

        val uri = mockPaymentsUri()
        val result = sut(uri)

        assertThat(result).isEqualTo(ResolveAppLink.Action.ViewPayments)
        verify(tracker).track(AnalyticsEvent.UNIVERSAL_LINK_OPENED, mapOf(KEY_PATH to uri.path))
    }

    @Test
    fun `for payments link restart activity event when link points to a different than selected blog id`() {
        mockSiteSelected(mockedSiteId = 987)

        val uri = mockPaymentsUri()
        val result = sut(uri)

        assertThat(result).isEqualTo(ResolveAppLink.Action.ChangeSiteAndRestart(TEST_BLOG_ID, uri))
        verifyNoInteractions(tracker)
    }

    @Test
    fun `for payments link open payments even if blog id is null`() {
        mockSiteSelected()

        val uri = mockPaymentsUri(blogId = null)
        val result = sut(uri)

        assertThat(result).isEqualTo(ResolveAppLink.Action.ViewPayments)
        verify(tracker).track(AnalyticsEvent.UNIVERSAL_LINK_OPENED, mapOf(KEY_PATH to uri.path))
    }

    private fun mockSiteSelected(mockedSiteId: Long = TEST_BLOG_ID) {
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(selectedSite.getIfExists()).thenReturn(
            SiteModel().apply {
                siteId = mockedSiteId
            }
        )
    }

    private fun mockOrderDetailsUri(
        orderId: String = TEST_ORDER_ID.toString(),
        blogId: String = TEST_BLOG_ID.toString()
    ): Uri {
        val uri = mock<Uri> {
            on { path } doReturn "orders/details"
            on { getQueryParameter("order_id") } doReturn orderId
            on { getQueryParameter("blog_id") } doReturn blogId
        }
        return uri
    }

    private fun mockPaymentsUri(
        blogId: String? = TEST_BLOG_ID.toString()
    ): Uri {
        val uri = mock<Uri> {
            on { path } doReturn "payments"
            on { getQueryParameter("blog_id") } doReturn blogId
        }
        return uri
    }

    private companion object {
        const val TEST_ORDER_ID = 123L
        const val TEST_BLOG_ID = 345L
    }
}
