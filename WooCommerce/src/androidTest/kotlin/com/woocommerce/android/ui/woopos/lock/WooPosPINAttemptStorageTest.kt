package com.woocommerce.android.ui.woopos.lock

import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WooPosPINAttemptStorageTest {
    private val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
        override fun getNoBackupFilesDir() = File(super.getNoBackupFilesDir(), "attempt-test").apply { mkdirs() }
    }
    private val storage = WooPosFilePINAttemptStorage(context)
    private val directory get() = File(context.noBackupFilesDir, "woopos-pin-attempts")

    @Before
    fun setUp() = storage.clearAll()

    @After
    fun tearDown() = storage.clearAll()

    @Test
    fun recordsSurviveRecreationAndRemainStoreScopedOutsideBackup() {
        // GIVEN
        val state = WooPosPINAttemptState(5, 1, 1000, 30_000)
        storage.write(1, state)

        // WHEN
        val recreated = WooPosFilePINAttemptStorage(context)

        // THEN
        assertThat(recreated.read(1)).isEqualTo(state)
        assertThat(recreated.read(2)).isEqualTo(WooPosPINAttemptState())
        assertThat(directory.canonicalPath).startsWith(context.noBackupFilesDir.canonicalPath)
        assertThat(directory.canonicalPath).doesNotStartWith(context.filesDir.canonicalPath)
    }

    @Test
    fun corruptAndTruncatedRecordsFailClosed() {
        // GIVEN
        storage.write(1, WooPosPINAttemptState(5, 1, 1000, 30_000))
        val file = File(directory, "1.attempts")
        val original = file.readBytes()

        // WHEN
        listOf(byteArrayOf(0), original + byteArrayOf(0), original.copyOf(10)).forEach { corrupt ->
            file.writeBytes(corrupt)

            // THEN
            assertThatThrownBy { storage.read(1) }.isInstanceOf(Exception::class.java)
        }
    }

    @Test
    fun failedWritePreservesPreviousPenaltyAndDoesNotReportSuccess() {
        // GIVEN
        val state = WooPosPINAttemptState(5, 1, 1000, 30_000)
        storage.write(1, state)
        File(directory, "1.attempts.new").writeBytes(byteArrayOf(0))
        check(directory.setWritable(false))
        try {
            // THEN
            assertThatThrownBy { storage.write(1, WooPosPINAttemptState()) }.isInstanceOf(Exception::class.java)
            assertThat(storage.read(1)).isEqualTo(state)
        } finally {
            check(directory.setWritable(true))
        }
    }

    @Test
    fun unreadableDirectoryDoesNotBecomeAnEmptyBudget() {
        // GIVEN
        storage.write(1, WooPosPINAttemptState(5, 1, 1000, 30_000))
        check(directory.setExecutable(false))
        try {
            // THEN
            assertThatThrownBy { storage.read(1) }.isInstanceOf(Exception::class.java)
        } finally {
            check(directory.setExecutable(true))
        }
    }

    @Test
    fun clearingErasesEveryStoresPenalty() {
        // GIVEN
        storage.write(1, WooPosPINAttemptState(5, 1, 1000, 30_000))
        storage.write(2, WooPosPINAttemptState(10, 1, 1000, 300_000))

        // WHEN
        storage.clearAll()

        // THEN
        assertThat(storage.read(1)).isEqualTo(WooPosPINAttemptState())
        assertThat(storage.read(2)).isEqualTo(WooPosPINAttemptState())
    }
}
