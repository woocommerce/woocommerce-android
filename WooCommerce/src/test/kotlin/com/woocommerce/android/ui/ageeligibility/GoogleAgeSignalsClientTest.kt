package com.woocommerce.android.ui.ageeligibility

import android.app.Activity
import android.os.RemoteException
import com.google.android.gms.tasks.Tasks
import com.google.android.play.agesignals.AgeSignalsAccessRequest
import com.google.android.play.agesignals.AgeSignalsAccessResult
import com.google.android.play.agesignals.AgeSignalsException
import com.google.android.play.agesignals.AgeSignalsManager
import com.google.android.play.agesignals.AgeSignalsResult
import com.google.android.play.agesignals.model.AgeRangeSource
import com.google.android.play.agesignals.model.AgeSignalsErrorCode
import com.google.android.play.agesignals.model.AgeSignalsStatus
import com.google.android.play.agesignals.model.SignificantChangeStatus
import com.google.android.play.agesignals.testing.FakeAgeSignalsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class GoogleAgeSignalsClientTest {
    private val activity: Activity = mock()

    @Test
    fun `given shared access, when requested with fake manager, then all allowed response fields are mapped`() =
        runTest {
            val manager = FakeAgeSignalsManager().apply {
                setNextAgeSignalsAccessResult(accessResult(AgeSignalsStatus.SHARED))
                setNextAgeSignalsResult(
                    AgeSignalsResult.builder()
                        .setAgeLower(13)
                        .setAgeUpper(15)
                        .setAgeRangeSource(AgeRangeSource.TIER_B)
                        .setSignificantChangeStatus(SignificantChangeStatus.PENDING)
                        .setInstallId("must-not-be-exposed")
                        .setSignificantChangeApprovalDate(Date())
                        .build()
                )
            }

            val result = GoogleAgeSignalsClient(manager).requestAgeSignals(activity)

            assertThat(result).isEqualTo(
                AgeSignalsRequestResult(
                    accessStatus = AgeSignalsAccessStatus.SHARED,
                    ageSignals = SharedAgeSignals(
                        ageLower = 13,
                        ageUpper = 15,
                        ageRangeSource = AppAgeRangeSource.TIER_B,
                        significantChangeStatus = AppSignificantChangeStatus.PENDING
                    )
                )
            )
        }

    @Test
    fun `given access is not shared or verification is required, when requested, then signals are not checked`() =
        runTest {
            listOf(AgeSignalsStatus.NOT_SHARED, AgeSignalsStatus.VERIFICATION_REQUIRED).forEach { status ->
                val manager: AgeSignalsManager = mock()
                whenever(manager.requestAgeSignalsAccess(any())).thenReturn(Tasks.forResult(accessResult(status)))

                val result = GoogleAgeSignalsClient(manager).requestAgeSignals(activity)

                assertThat(result.ageSignals).isNull()
                verify(manager, never()).checkAgeSignals(any())
            }
        }

    @Test
    fun `when access is requested, then the current activity is supplied to Google`() = runTest {
        val manager: AgeSignalsManager = mock()
        whenever(manager.requestAgeSignalsAccess(any())).thenReturn(
            Tasks.forResult(accessResult(AgeSignalsStatus.NOT_SHARED))
        )

        GoogleAgeSignalsClient(manager).requestAgeSignals(activity)

        argumentCaptor<AgeSignalsAccessRequest>().apply {
            verify(manager).requestAgeSignalsAccess(capture())
            assertThat(firstValue.activity()).isSameAs(activity)
        }
    }

    @Test
    fun `given unknown SDK values, when requested, then they are mapped defensively`() = runTest {
        val manager = FakeAgeSignalsManager().apply {
            setNextAgeSignalsAccessResult(accessResult(AgeSignalsStatus.SHARED))
            setNextAgeSignalsResult(
                AgeSignalsResult.builder()
                    .setAgeLower(18)
                    .setAgeUpper(null)
                    .setAgeRangeSource(UNKNOWN_SDK_VALUE)
                    .setSignificantChangeStatus(UNKNOWN_SDK_VALUE)
                    .build()
            )
        }

        val result = GoogleAgeSignalsClient(manager).requestAgeSignals(activity)

        assertThat(result.ageSignals?.ageRangeSource).isEqualTo(AppAgeRangeSource.UNEXPECTED)
        assertThat(result.ageSignals?.significantChangeStatus).isEqualTo(AppSignificantChangeStatus.UNEXPECTED)
    }

    @Test
    fun `given unknown access status, when requested, then it is kept separate from not shared`() = runTest {
        val manager: AgeSignalsManager = mock()
        whenever(manager.requestAgeSignalsAccess(any())).thenReturn(
            Tasks.forResult(accessResult(UNKNOWN_SDK_VALUE))
        )

        val result = GoogleAgeSignalsClient(manager).requestAgeSignals(activity)

        assertThat(result.accessStatus).isEqualTo(AgeSignalsAccessStatus.UNEXPECTED)
        verify(manager, never()).checkAgeSignals(any())
    }

    @Test
    fun `given retryable access errors, when requested, then three total attempts use bounded backoff`() = runTest {
        RETRYABLE_ERROR_CODES.forEach { errorCode ->
            val manager: AgeSignalsManager = mock()
            whenever(manager.requestAgeSignalsAccess(any())).thenReturn(
                Tasks.forException(AgeSignalsException(errorCode))
            )
            val startTime = currentTime

            val failure = requestFailure(GoogleAgeSignalsClient(manager))

            assertThat(failure.errorCode.isRetryable).isTrue()
            assertThat(failure.retryCount).isEqualTo(2)
            assertThat(currentTime - startTime).isEqualTo(1_500L)
            verify(manager, times(3)).requestAgeSignalsAccess(any())
        }
    }

    @Test
    fun `given binder death, when requested, then it is retried with the same bounded budget`() = runTest {
        val manager: AgeSignalsManager = mock()
        whenever(manager.requestAgeSignalsAccess(any())).thenReturn(
            Tasks.forException(RemoteException())
        )

        val failure = requestFailure(GoogleAgeSignalsClient(manager))

        assertThat(failure.errorCode).isEqualTo(AppAgeSignalsErrorCode.BINDER_DIED)
        assertThat(failure.retryCount).isEqualTo(2)
        assertThat(currentTime).isEqualTo(1_500L)
        verify(manager, times(3)).requestAgeSignalsAccess(any())
    }

    @Test
    fun `given terminal access errors, when requested, then the request is not retried`() = runTest {
        TERMINAL_ERROR_CODES.forEach { errorCode ->
            val manager: AgeSignalsManager = mock()
            whenever(manager.requestAgeSignalsAccess(any())).thenReturn(
                Tasks.forException(AgeSignalsException(errorCode))
            )

            val failure = requestFailure(GoogleAgeSignalsClient(manager))

            assertThat(failure.errorCode.isRetryable).isFalse()
            assertThat(failure.retryCount).isZero()
            verify(manager).requestAgeSignalsAccess(any())
        }
    }

    @Test
    fun `given a retryable check error, when retried, then only the check stage runs again`() = runTest {
        // GIVEN
        val manager: AgeSignalsManager = mock()
        whenever(manager.requestAgeSignalsAccess(any())).thenReturn(
            Tasks.forResult(accessResult(AgeSignalsStatus.SHARED))
        )
        whenever(manager.checkAgeSignals(any())).thenReturn(
            Tasks.forException(AgeSignalsException(AgeSignalsErrorCode.CLIENT_TRANSIENT_ERROR)),
            Tasks.forException(AgeSignalsException(AgeSignalsErrorCode.INTERNAL_ERROR)),
            Tasks.forResult(eligibleSignalsResult())
        )

        // WHEN
        val result = GoogleAgeSignalsClient(manager).requestAgeSignals(activity)

        // THEN
        assertThat(result.retryCount).isEqualTo(2)
        assertThat(result.ageSignals?.ageLower).isEqualTo(18)
        assertThat(currentTime).isEqualTo(1_500L)
        verify(manager).requestAgeSignalsAccess(any())
        verify(manager, times(3)).checkAgeSignals(any())
    }

    private suspend fun requestFailure(client: GoogleAgeSignalsClient): AgeSignalsRequestFailure {
        val exception = runCatching { client.requestAgeSignals(activity) }.exceptionOrNull()
        assertThat(exception).isInstanceOf(AgeSignalsRequestFailure::class.java)
        return exception as AgeSignalsRequestFailure
    }

    private fun accessResult(status: Int) = AgeSignalsAccessResult.builder()
        .setAgeSignalsStatus(status)
        .build()

    private fun eligibleSignalsResult() = AgeSignalsResult.builder()
        .setAgeLower(18)
        .setAgeUpper(null)
        .setAgeRangeSource(AgeRangeSource.TIER_D)
        .setSignificantChangeStatus(SignificantChangeStatus.APPROVED)
        .build()

    companion object {
        private const val UNKNOWN_SDK_VALUE = 999

        private val RETRYABLE_ERROR_CODES = listOf(
            AgeSignalsErrorCode.CANNOT_BIND_TO_SERVICE,
            AgeSignalsErrorCode.CLIENT_TRANSIENT_ERROR,
            AgeSignalsErrorCode.INTERNAL_ERROR
        )

        private val TERMINAL_ERROR_CODES = listOf(
            AgeSignalsErrorCode.API_NOT_AVAILABLE,
            AgeSignalsErrorCode.PLAY_STORE_NOT_FOUND,
            AgeSignalsErrorCode.NETWORK_ERROR,
            AgeSignalsErrorCode.PLAY_SERVICES_NOT_FOUND,
            AgeSignalsErrorCode.PLAY_STORE_VERSION_OUTDATED,
            AgeSignalsErrorCode.PLAY_SERVICES_VERSION_OUTDATED,
            AgeSignalsErrorCode.APP_NOT_OWNED,
            AgeSignalsErrorCode.SDK_VERSION_OUTDATED,
            UNKNOWN_SDK_VALUE
        )
    }
}
