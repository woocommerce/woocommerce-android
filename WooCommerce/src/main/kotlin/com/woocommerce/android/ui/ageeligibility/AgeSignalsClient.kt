package com.woocommerce.android.ui.ageeligibility

import android.app.Activity
import android.os.RemoteException
import com.google.android.play.agesignals.AgeSignalsAccessRequest
import com.google.android.play.agesignals.AgeSignalsException
import com.google.android.play.agesignals.AgeSignalsManager
import com.google.android.play.agesignals.AgeSignalsRequest
import com.google.android.play.agesignals.model.AgeRangeSource
import com.google.android.play.agesignals.model.AgeSignalsErrorCode
import com.google.android.play.agesignals.model.AgeSignalsStatus
import com.google.android.play.agesignals.model.SignificantChangeStatus
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface AgeSignalsClient {
    suspend fun requestAgeSignals(activity: Activity): AgeSignalsRequestResult
}

@Singleton
class GoogleAgeSignalsClient @Inject constructor(
    private val manager: AgeSignalsManager
) : AgeSignalsClient {
    @Suppress("SwallowedException")
    override suspend fun requestAgeSignals(activity: Activity): AgeSignalsRequestResult {
        var retryCount = 0
        while (true) {
            try {
                return requestAgeSignalsOnce(activity).copy(retryCount = retryCount)
            } catch (exception: StageException) {
                val errorCode = exception.originalException.toAgeSignalsErrorCode()
                if (errorCode.isRetryable && retryCount < MAX_RETRY_COUNT) {
                    delay(RETRY_BACKOFF_MILLIS[retryCount])
                    retryCount++
                } else {
                    throw AgeSignalsRequestFailure(
                        stage = exception.stage,
                        errorCode = errorCode,
                        retryCount = retryCount,
                        cause = exception.originalException
                    )
                }
            }
        }
    }

    private suspend fun requestAgeSignalsOnce(activity: Activity): AgeSignalsRequestResult {
        val accessRequest = AgeSignalsAccessRequest.builder()
            .setActivity(activity)
            .build()
        val accessResult = executeStage(AgeSignalsRequestStage.ACCESS) {
            manager.requestAgeSignalsAccess(accessRequest).await()
        }
        val rawAccessStatus = accessResult.ageSignalsStatus()
        val accessStatus = rawAccessStatus.toAgeSignalsAccessStatus()

        if (accessStatus == AgeSignalsAccessStatus.UNEXPECTED) {
            WooLog.w(WooLog.T.UTILS, "Unexpected Age Signals access status: $rawAccessStatus")
        }

        return if (accessStatus == AgeSignalsAccessStatus.SHARED) {
            val signalsResult = executeStage(AgeSignalsRequestStage.CHECK) {
                manager.checkAgeSignals(AgeSignalsRequest.builder().build()).await()
            }
            AgeSignalsRequestResult(
                accessStatus = accessStatus,
                ageSignals = SharedAgeSignals(
                    ageLower = signalsResult.ageLower(),
                    ageUpper = signalsResult.ageUpper(),
                    ageRangeSource = signalsResult.ageRangeSource().toAgeRangeSource(),
                    significantChangeStatus = signalsResult.significantChangeStatus()
                        .toSignificantChangeStatus()
                )
            )
        } else {
            AgeSignalsRequestResult(accessStatus = accessStatus)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> executeStage(
        stage: AgeSignalsRequestStage,
        block: suspend () -> T
    ): T = try {
        block()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        throw StageException(stage, exception)
    }

    private fun Int?.toAgeSignalsAccessStatus(): AgeSignalsAccessStatus = when (this) {
        AgeSignalsStatus.UNSPECIFIED -> AgeSignalsAccessStatus.UNSPECIFIED
        AgeSignalsStatus.SHARED -> AgeSignalsAccessStatus.SHARED
        AgeSignalsStatus.NOT_SHARED -> AgeSignalsAccessStatus.NOT_SHARED
        AgeSignalsStatus.VERIFICATION_REQUIRED -> AgeSignalsAccessStatus.VERIFICATION_REQUIRED
        else -> AgeSignalsAccessStatus.UNEXPECTED
    }

    private fun Int?.toAgeRangeSource(): AppAgeRangeSource = when (this) {
        null,
        AgeRangeSource.UNSPECIFIED -> AppAgeRangeSource.UNSPECIFIED

        AgeRangeSource.TIER_A -> AppAgeRangeSource.TIER_A
        AgeRangeSource.TIER_B -> AppAgeRangeSource.TIER_B
        AgeRangeSource.TIER_C -> AppAgeRangeSource.TIER_C
        AgeRangeSource.TIER_D -> AppAgeRangeSource.TIER_D
        else -> AppAgeRangeSource.UNEXPECTED
    }

    private fun Int?.toSignificantChangeStatus(): AppSignificantChangeStatus = when (this) {
        null,
        SignificantChangeStatus.UNSPECIFIED -> AppSignificantChangeStatus.UNSPECIFIED

        SignificantChangeStatus.APPROVED -> AppSignificantChangeStatus.APPROVED
        SignificantChangeStatus.PENDING -> AppSignificantChangeStatus.PENDING
        SignificantChangeStatus.DECLINED -> AppSignificantChangeStatus.DECLINED
        else -> AppSignificantChangeStatus.UNEXPECTED
    }

    private fun Exception.toAgeSignalsErrorCode(): AppAgeSignalsErrorCode = when (this) {
        is RemoteException -> AppAgeSignalsErrorCode.BINDER_DIED
        is AgeSignalsException -> errorCode.toAgeSignalsErrorCode()
        else -> AppAgeSignalsErrorCode.UNEXPECTED
    }

    private fun Int.toAgeSignalsErrorCode(): AppAgeSignalsErrorCode = when (this) {
        AgeSignalsErrorCode.API_NOT_AVAILABLE -> AppAgeSignalsErrorCode.API_NOT_AVAILABLE
        AgeSignalsErrorCode.PLAY_STORE_NOT_FOUND -> AppAgeSignalsErrorCode.PLAY_STORE_NOT_FOUND
        AgeSignalsErrorCode.NETWORK_ERROR -> AppAgeSignalsErrorCode.NETWORK_ERROR
        AgeSignalsErrorCode.PLAY_SERVICES_NOT_FOUND -> AppAgeSignalsErrorCode.PLAY_SERVICES_NOT_FOUND
        AgeSignalsErrorCode.CANNOT_BIND_TO_SERVICE -> AppAgeSignalsErrorCode.CANNOT_BIND_TO_SERVICE
        AgeSignalsErrorCode.PLAY_STORE_VERSION_OUTDATED -> AppAgeSignalsErrorCode.PLAY_STORE_VERSION_OUTDATED
        AgeSignalsErrorCode.PLAY_SERVICES_VERSION_OUTDATED -> AppAgeSignalsErrorCode.PLAY_SERVICES_VERSION_OUTDATED
        AgeSignalsErrorCode.CLIENT_TRANSIENT_ERROR -> AppAgeSignalsErrorCode.CLIENT_TRANSIENT_ERROR
        AgeSignalsErrorCode.APP_NOT_OWNED -> AppAgeSignalsErrorCode.APP_NOT_OWNED
        AgeSignalsErrorCode.SDK_VERSION_OUTDATED -> AppAgeSignalsErrorCode.SDK_VERSION_OUTDATED
        AgeSignalsErrorCode.INTERNAL_ERROR -> AppAgeSignalsErrorCode.INTERNAL_ERROR
        else -> AppAgeSignalsErrorCode.UNEXPECTED
    }

    private class StageException(
        val stage: AgeSignalsRequestStage,
        val originalException: Exception
    ) : Exception(null, originalException)

    companion object {
        private const val MAX_RETRY_COUNT = 2
        private val RETRY_BACKOFF_MILLIS = longArrayOf(500L, 1_000L)
    }
}

data class AgeSignalsRequestResult(
    val accessStatus: AgeSignalsAccessStatus,
    val ageSignals: SharedAgeSignals? = null,
    val retryCount: Int = 0
)

data class SharedAgeSignals(
    val ageLower: Int?,
    val ageUpper: Int?,
    val ageRangeSource: AppAgeRangeSource,
    val significantChangeStatus: AppSignificantChangeStatus
)

enum class AgeSignalsAccessStatus {
    UNSPECIFIED,
    SHARED,
    NOT_SHARED,
    VERIFICATION_REQUIRED,
    UNEXPECTED
}

enum class AppAgeRangeSource {
    UNSPECIFIED,
    TIER_A,
    TIER_B,
    TIER_C,
    TIER_D,
    UNEXPECTED
}

enum class AppSignificantChangeStatus {
    UNSPECIFIED,
    APPROVED,
    PENDING,
    DECLINED,
    UNEXPECTED
}

enum class AgeSignalsRequestStage {
    ACCESS,
    CHECK
}

enum class AppAgeSignalsErrorCode(val isRetryable: Boolean) {
    API_NOT_AVAILABLE(false),
    PLAY_STORE_NOT_FOUND(false),
    NETWORK_ERROR(false),
    PLAY_SERVICES_NOT_FOUND(false),
    CANNOT_BIND_TO_SERVICE(true),
    PLAY_STORE_VERSION_OUTDATED(false),
    PLAY_SERVICES_VERSION_OUTDATED(false),
    CLIENT_TRANSIENT_ERROR(true),
    APP_NOT_OWNED(false),
    SDK_VERSION_OUTDATED(false),
    INTERNAL_ERROR(true),
    BINDER_DIED(true),
    UNEXPECTED(false)
}

class AgeSignalsRequestFailure(
    val stage: AgeSignalsRequestStage,
    val errorCode: AppAgeSignalsErrorCode,
    val retryCount: Int,
    cause: Exception
) : Exception(null, cause)
