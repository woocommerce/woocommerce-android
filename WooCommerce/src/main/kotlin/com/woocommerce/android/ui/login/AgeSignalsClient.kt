package com.woocommerce.android.ui.login

import android.content.Context
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import com.google.android.play.agesignals.AgeSignalsRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface AgeSignalsClient {
    suspend fun checkAge(): AgeCheckResult
}

data class AgeCheckResult(
    val userStatus: Int?,
    val ageUpper: Int?
)

class DefaultAgeSignalsClient @Inject constructor(
    @ApplicationContext private val context: Context
) : AgeSignalsClient {
    override suspend fun checkAge(): AgeCheckResult {
        val manager = AgeSignalsManagerFactory.create(context)
        val result = manager.checkAgeSignals(AgeSignalsRequest.builder().build()).await()
        return AgeCheckResult(result.userStatus(), result.ageUpper())
    }
}
