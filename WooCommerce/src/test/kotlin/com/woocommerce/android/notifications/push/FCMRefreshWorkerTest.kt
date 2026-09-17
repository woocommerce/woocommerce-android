package com.woocommerce.android.notifications.push

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkRequest
import androidx.work.impl.WorkManagerImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FCMRefreshWorkerTest {
    private val context: Context = mock()
    private val workManager: WorkManagerImpl = mock()

    @Test
    fun `when scheduling periodic refresh, then only delayed periodic work is enqueued`() {
        mockStatic(WorkManagerImpl::class.java).use {
            whenever(WorkManagerImpl.getInstance(context)).thenReturn(workManager)

            FCMRefreshWorker.schedule(context)
        }

        val periodicWorkCaptor = argumentCaptor<PeriodicWorkRequest>()
        verify(workManager).enqueueUniquePeriodicWork(
            eq(PERIODIC_WORK_NAME),
            eq(ExistingPeriodicWorkPolicy.KEEP),
            periodicWorkCaptor.capture(),
        )
        verify(workManager, never()).enqueueUniqueWork(
            any<String>(),
            any<ExistingWorkPolicy>(),
            any<OneTimeWorkRequest>(),
        )
        assertThat(periodicWorkCaptor.firstValue.workSpec.constraints.requiredNetworkType)
            .isEqualTo(NetworkType.CONNECTED)
        assertThat(periodicWorkCaptor.firstValue.workSpec.initialDelay)
            .isEqualTo(periodicWorkCaptor.firstValue.workSpec.intervalDuration)
    }

    @Test
    fun `when requesting immediate refresh, then only immediate work with default retry backoff is enqueued`() {
        mockStatic(WorkManagerImpl::class.java).use {
            whenever(WorkManagerImpl.getInstance(context)).thenReturn(workManager)

            FCMRefreshWorker.run(context)
        }

        val immediateWorkCaptor = argumentCaptor<OneTimeWorkRequest>()
        verify(workManager, never()).enqueueUniquePeriodicWork(
            any<String>(),
            any<ExistingPeriodicWorkPolicy>(),
            any<PeriodicWorkRequest>(),
        )
        verify(workManager).enqueueUniqueWork(
            eq(IMMEDIATE_WORK_NAME),
            eq(ExistingWorkPolicy.KEEP),
            immediateWorkCaptor.capture(),
        )
        assertThat(immediateWorkCaptor.firstValue.workSpec.constraints.requiredNetworkType)
            .isEqualTo(NetworkType.CONNECTED)
        assertThat(immediateWorkCaptor.firstValue.workSpec.initialDelay).isZero()
        assertThat(immediateWorkCaptor.firstValue.workSpec.backoffDelayDuration)
            .isEqualTo(WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS)
    }

    private companion object {
        const val PERIODIC_WORK_NAME = "FCMRefreshWorker"
        const val IMMEDIATE_WORK_NAME = "FCMRefreshWorkerImmediate"
    }
}
