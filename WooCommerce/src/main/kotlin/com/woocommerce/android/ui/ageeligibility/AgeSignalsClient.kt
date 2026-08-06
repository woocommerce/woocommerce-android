package com.woocommerce.android.ui.ageeligibility

import android.app.Activity
import android.content.Context
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import com.google.android.play.agesignals.AgeSignalsRequest
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface AgeSignalsClient {
    suspend fun checkAge(activity: Activity): AgeCheckResult
}

data class AgeCheckResult(
    val verificationStatus: LegacyAgeVerificationStatus,
    val ageUpper: Int?
)

enum class LegacyAgeVerificationStatus {
    VERIFIED,
    SUPERVISED,
    SUPERVISED_APPROVAL_PENDING,
    SUPERVISED_APPROVAL_DENIED,
    UNKNOWN,
    UNEXPECTED
}

@Singleton
class GoogleAgeSignalsClient @Inject constructor(
    @ApplicationContext private val context: Context
) : AgeSignalsClient {
    override suspend fun checkAge(activity: Activity): AgeCheckResult {
        val manager = AgeSignalsManagerFactory.create(context)
        val result = manager.checkAgeSignals(AgeSignalsRequest.builder().build()).await()
        return AgeCheckResult(
            verificationStatus = result.userStatus().toLegacyAgeVerificationStatus(),
            ageUpper = result.ageUpper()
        )
    }

    private fun Int?.toLegacyAgeVerificationStatus(): LegacyAgeVerificationStatus = when (this) {
        AgeSignalsVerificationStatus.VERIFIED -> LegacyAgeVerificationStatus.VERIFIED
        AgeSignalsVerificationStatus.SUPERVISED -> LegacyAgeVerificationStatus.SUPERVISED
        AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING ->
            LegacyAgeVerificationStatus.SUPERVISED_APPROVAL_PENDING

        AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED ->
            LegacyAgeVerificationStatus.SUPERVISED_APPROVAL_DENIED

        AgeSignalsVerificationStatus.UNKNOWN -> LegacyAgeVerificationStatus.UNKNOWN
        else -> LegacyAgeVerificationStatus.UNEXPECTED
    }
}
