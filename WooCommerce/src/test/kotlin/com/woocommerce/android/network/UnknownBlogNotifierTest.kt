package com.woocommerce.android.network

import app.cash.turbine.test
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UnknownBlogNotifierTest : BaseUnitTest() {
    private val sut = UnknownBlogNotifier()

    @Test
    fun `when onUnknownBlog is called, then the site id is emitted`() = testBlocking {
        sut.unknownBlogEvents.test {
            sut.onUnknownBlog(SITE_ID)

            assertThat(awaitItem()).isEqualTo(SITE_ID)
        }
    }

    companion object {
        private const val SITE_ID = 789L
    }
}
