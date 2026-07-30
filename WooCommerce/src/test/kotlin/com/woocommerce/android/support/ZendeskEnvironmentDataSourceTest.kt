package com.woocommerce.android.support

import com.woocommerce.android.support.zendesk.ZendeskEnvironmentDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ZendeskEnvironmentDataSourceTest {
    @Test
    fun `given a size below a kilobyte, when formatted, then it is reported in bytes`() {
        assertThat(ZendeskEnvironmentDataSource.formatAvailableSpace(512)).isEqualTo("512.0 B")
    }

    @Test
    fun `given a size of a few megabytes, when formatted, then it is reported in megabytes`() {
        assertThat(ZendeskEnvironmentDataSource.formatAvailableSpace(5L * 1024 * 1024)).isEqualTo("5.0 MB")
    }

    @Test
    fun `given a size of several gigabytes, when formatted, then it is reported in gigabytes`() {
        val bytes = (12.4 * 1024 * 1024 * 1024).toLong()

        assertThat(ZendeskEnvironmentDataSource.formatAvailableSpace(bytes)).isEqualTo("12.4 GB")
    }

    /**
     * The reason this formatting is not delegated to `DeviceUtils.getTotalAvailableMemorySize()`, which stops at
     * megabytes and so reported a half-full flagship as `104,857MB`.
     */
    @Test
    fun `given a size a phone can actually have, when formatted, then it does not stop at megabytes`() {
        val bytes = 100L * 1024 * 1024 * 1024

        assertThat(ZendeskEnvironmentDataSource.formatAvailableSpace(bytes)).isEqualTo("100.0 GB")
    }

    @Test
    fun `given zero free space, when formatted, then it is reported as zero bytes`() {
        assertThat(ZendeskEnvironmentDataSource.formatAvailableSpace(0)).isEqualTo("0.0 B")
    }

    @Test
    fun `given a size beyond terabytes, when formatted, then it stays in terabytes`() {
        val bytes = 2048L * 1024 * 1024 * 1024 * 1024

        assertThat(ZendeskEnvironmentDataSource.formatAvailableSpace(bytes)).isEqualTo("2048.0 TB")
    }
}
