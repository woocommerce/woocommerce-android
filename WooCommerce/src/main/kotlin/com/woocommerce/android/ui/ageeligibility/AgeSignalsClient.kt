package com.woocommerce.android.ui.ageeligibility

import android.content.Context
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import com.google.android.play.agesignals.AgeSignalsRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface AgeSignalsClient {
    suspend fun checkAge(): AgeCheckResult
}

data class AgeCheckResult(
    val userStatus: Int?,
    val ageUpper: Int?
)

@Singleton
class GoogleAgeSignalsClient @Inject constructor(
    @ApplicationContext private val context: Context
) : AgeSignalsClient {
    override suspend fun checkAge(): AgeCheckResult {
        val manager = AgeSignalsManagerFactory.create(context)
        val result = manager.checkAgeSignals(AgeSignalsRequest.builder().build()).await()
        return AgeCheckResult(result.userStatus(), result.ageUpper())
    }
}
