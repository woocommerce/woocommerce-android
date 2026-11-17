package com.woocommerce.android.ui.woopos.roles.pin

import android.app.Application
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE, sdk = [28])
class WooPosPINStorageFilesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `given only an atomic backup, when reading, then the existing record is recovered`() {
        // GIVEN
        val files = WooPosPINStorageFiles(temporaryFolder.newFolder(), "attempts")
        val file = files.file(1)
        val record = byteArrayOf(1, 2, 3)
        File(file.baseFile.path + ".bak").writeBytes(record)

        // THEN
        assertThat(files.exists(file)).isTrue()
        assertThat(file.openRead().use { it.readBytes() }).containsExactly(*record)
        assertThat(file.baseFile.readBytes()).containsExactly(*record)
    }

    @Test
    fun `given a file where the directory should be, when checking access, then storage fails closed`() {
        // GIVEN
        val files = WooPosPINStorageFiles(temporaryFolder.newFile(), "pins")

        // THEN
        assertThatThrownBy { files.checkDirectoryAccess() }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `given no saved data, when checking for a record, then no directory or record is created`() {
        // GIVEN
        val directory = File(temporaryFolder.root, "missing")
        val files = WooPosPINStorageFiles(directory, "pins")

        // WHEN
        files.checkDirectoryAccess()

        // THEN
        assertThat(files.exists(files.file(1))).isFalse()
        assertThat(directory.exists()).isFalse()
    }

    @Test
    fun `given an invalid site identity, when requesting a file, then no record is created`() {
        // GIVEN
        val files = WooPosPINStorageFiles(temporaryFolder.root, "pins")

        // THEN
        listOf(0, -1).forEach { siteId ->
            assertThatThrownBy { files.file(siteId) }.isInstanceOf(IllegalArgumentException::class.java)
        }
        assertThat(temporaryFolder.root.listFiles()).isEmpty()
    }
}
