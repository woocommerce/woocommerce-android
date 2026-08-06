package com.woocommerce.android.ui.ageeligibility

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AgeVerificationPlayStoreLauncherTest {
    private val context: Context = mock()
    private val launcher = AgeVerificationPlayStoreLauncher()
    private lateinit var uriStaticMock: MockedStatic<Uri>

    @Before
    fun setup() {
        uriStaticMock = mockStatic(Uri::class.java)
        whenever(context.packageName).thenReturn(PACKAGE_NAME)
        whenever(Uri.parse(MARKET_URI)).thenReturn(mock())
        whenever(Uri.parse(HTTPS_URI)).thenReturn(mock())
    }

    @After
    fun tearDown() {
        uriStaticMock.close()
    }

    @Test
    fun `when Play Store is available, then market URI is opened`() {
        val opened = launcher.open(context)

        verify(context).startActivity(any())
        assertThat(opened).isTrue()
        uriStaticMock.verify { Uri.parse(MARKET_URI) }
    }

    @Test
    fun `given market URI has no handler, when opened, then HTTPS fallback is used`() {
        var invocation = 0
        doAnswer {
            if (invocation++ == 0) throw ActivityNotFoundException()
            null
        }.whenever(context).startActivity(any())

        val opened = launcher.open(context)

        verify(context, times(2)).startActivity(any())
        assertThat(opened).isTrue()
        uriStaticMock.verify { Uri.parse(HTTPS_URI) }
    }

    @Test
    fun `given no Play URI handler, when opened, then failure is returned`() {
        doThrow(ActivityNotFoundException()).whenever(context).startActivity(any())

        val opened = launcher.open(context)

        assertThat(opened).isFalse()
    }

    companion object {
        private const val PACKAGE_NAME = "com.woocommerce.android"
        private const val MARKET_URI = "market://details?id=$PACKAGE_NAME"
        private const val HTTPS_URI = "https://play.google.com/store/apps/details?id=$PACKAGE_NAME"
    }
}
