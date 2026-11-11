package com.woocommerce.android.ui.woopos.lock

import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WooPosLockedPOSStoreTest {
    private val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
        override fun getNoBackupFilesDir() = File(super.getNoBackupFilesDir(), "locked-pos-test").apply { mkdirs() }
    }
    private val store = WooPosFileLockedPOSStore(context, CoroutineDispatchers())
    private val directory get() = File(context.noBackupFilesDir, "woopos-locked")

    @Before
    fun setUp(): Unit = runBlocking { store.clearAll().getOrThrow() }

    @After
    fun tearDown(): Unit = runBlocking { store.clearAll().getOrThrow() }

    @Test
    fun markersSurviveRecreationAndAreStoreScoped(): Unit = runBlocking {
        // GIVEN
        store.mark(1).getOrThrow()

        // WHEN
        val recreated = WooPosFileLockedPOSStore(context, CoroutineDispatchers())

        // THEN
        assertThat(recreated.isMarked(1).getOrThrow()).isTrue()
        assertThat(recreated.isMarked(2).getOrThrow()).isFalse()
        assertThat(directory.canonicalPath).startsWith(context.noBackupFilesDir.canonicalPath)
        store.clear(1).getOrThrow()
        assertThat(recreated.isMarked(1).getOrThrow()).isFalse()
    }

    @Test
    fun corruptMarkerStaysConservativeUntilDurablyRepaired(): Unit = runBlocking {
        // GIVEN
        store.mark(1).getOrThrow()
        val marker = File(directory, "1.locked")
        listOf(byteArrayOf(), byteArrayOf(2), byteArrayOf(1, 1)).forEach { corrupt ->
            marker.writeBytes(corrupt)
            assertThat(store.isMarked(1).isFailure).isTrue()

            // WHEN
            store.mark(1).getOrThrow()

            // THEN
            assertThat(store.isMarked(1).getOrThrow()).isTrue()
            assertThat(WooPosFileLockedPOSStore(context, CoroutineDispatchers()).isMarked(1).getOrThrow()).isTrue()
        }
    }

    @Test
    fun corruptMarkerCannotBeRepairedWhenDurableWriteFails(): Unit = runBlocking {
        // GIVEN
        store.mark(1).getOrThrow()
        File(directory, "1.locked").writeBytes(byteArrayOf(2))
        check(directory.setWritable(false))
        try {
            // THEN
            assertThat(store.mark(1).isFailure).isTrue()
            assertThat(store.isMarked(1).isFailure).isTrue()
        } finally {
            check(directory.setWritable(true))
        }
    }

    @Test
    fun failedMarkerWriteAndClearAreReported(): Unit = runBlocking {
        // GIVEN
        store.mark(1).getOrThrow()
        check(directory.setWritable(false))
        try {
            // THEN
            assertThat(store.mark(2).isFailure).isTrue()
            assertThat(store.clear(1).isFailure).isTrue()
            assertThat(store.isMarked(1).getOrThrow()).isTrue()
        } finally {
            check(directory.setWritable(true))
        }
    }
}
