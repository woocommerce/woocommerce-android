package com.woocommerce.android.ui.ageeligibility

import android.app.Activity
import androidx.annotation.StringRes
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgeEligibilityChecker @Inject constructor(
    private val client: AgeSignalsClient,
    private val prefsWrapper: AppPrefsWrapper,
    private val accountRepository: AccountRepository,
    private val featureFlagRepository: FeatureFlagRepository,
    private val trackerWrapper: AnalyticsTrackerWrapper,
    private val evaluator: AgeEligibilityEvaluator,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {
    private val isCheckInProgress = AtomicBoolean(false)
    private val isStartupCheckPending = AtomicBoolean(true)
    private val retryAfterPlayStore = AtomicBoolean(false)
    private var persistedRestriction = readPersistedRestriction()

    private val _ageEligibilityState = MutableStateFlow(
        AgeEligibilityState(
            decision = persistedRestriction.toDecision(),
            ageRestrictedTitle = R.string.age_restriction_dialog_title,
            ageRestrictedMessage = persistedRestriction.toMessage()
        )
    )
    val ageEligibilityState: StateFlow<AgeEligibilityState> = _ageEligibilityState.asStateFlow()

    init {
        if (persistedRestriction == AgeRestrictionReason.LEGACY_RESTRICTION_UNKNOWN_REASON) {
            prefsWrapper.userAgeRestrictionReason = persistedRestriction?.name.orEmpty()
        }
    }

    suspend fun checkAge(activity: Activity, trigger: AgeCheckTrigger = AgeCheckTrigger.STARTUP) {
        runAgeCheckIfIdle(activity, trigger)
    }

    suspend fun checkAgeOnStartup(activity: Activity) {
        if (isStartupCheckPending.get() && runAgeCheckIfIdle(activity, AgeCheckTrigger.STARTUP)) {
            isStartupCheckPending.set(false)
        }
    }

    fun onPlayStoreOpenedForVerification() {
        retryAfterPlayStore.set(true)
    }

    suspend fun retryAfterReturningFromPlayStore(activity: Activity) {
        if (retryAfterPlayStore.get()) {
            runAgeCheckIfIdle(
                activity = activity,
                trigger = AgeCheckTrigger.RETURN_FROM_PLAY_STORE,
                onStarted = { retryAfterPlayStore.compareAndSet(true, false) },
                onCancelled = { retryAfterPlayStore.set(true) }
            )
        }
    }

    private suspend fun runAgeCheckIfIdle(
        activity: Activity,
        trigger: AgeCheckTrigger,
        onStarted: () -> Unit = {},
        onCancelled: () -> Unit = {}
    ): Boolean {
        if (!isCheckInProgress.compareAndSet(false, true)) {
            WooLog.i(WooLog.T.UTILS, "Skipping concurrent age check triggered by ${trigger.name}")
            return false
        }

        try {
            onStarted()
            checkAgeSingleFlight(activity)
            return true
        } catch (exception: CancellationException) {
            onCancelled()
            throw exception
        } finally {
            isCheckInProgress.set(false)
        }
    }

    private suspend fun checkAgeSingleFlight(activity: Activity) {
        if (!featureFlagRepository.isEnabled(FeatureFlag.AGE_ELIGIBILITY_CHECKS)) {
            _ageEligibilityState.update { it.copy(decision = AgeEligibilityDecision.Allowed) }
            return
        }

        val evaluation = try {
            val result = client.requestAgeSignals(activity)
            evaluator.evaluate(result, persistedRestriction)
        } catch (exception: AgeSignalsRequestFailure) {
            preservePriorRestriction(exception)
        }

        applyEvaluation(evaluation)

        val isAccessRestricted = evaluation.decision is AgeEligibilityDecision.Restricted
        trackerWrapper.track(
            AnalyticsEvent.ACCOUNT_AGE_RESTRICTION_CHECKED,
            properties = mapOf("access_restricted" to isAccessRestricted)
        )

        if (isAccessRestricted) {
            appCoroutineScope.launch {
                accountRepository.logout()
            }
        }
    }

    private fun applyEvaluation(evaluation: AgeEligibilityEvaluation) {
        val restriction = (evaluation.decision as? AgeEligibilityDecision.Restricted)?.reason
        _ageEligibilityState.update {
            it.copy(
                decision = evaluation.decision,
                ageRestrictedMessage = restriction.toMessage()
            )
        }

        if (evaluation.isAuthoritative) {
            persistedRestriction = restriction
            prefsWrapper.userAgeRestrictionReason = restriction?.name.orEmpty()
            prefsWrapper.isUserAgeEligibleForAppUse = restriction == null
        }
    }

    private fun preservePriorRestriction(exception: Exception): AgeEligibilityEvaluation {
        val failureSummary = if (exception is AgeSignalsRequestFailure) {
            "${exception.stage.name}/${exception.errorCode.name} after ${exception.retryCount} retries"
        } else {
            exception.javaClass.simpleName
        }
        WooLog.i(WooLog.T.UTILS, "Age eligibility check failed with $failureSummary; preserving prior decision")
        return if (_ageEligibilityState.value.decision is AgeEligibilityDecision.VerificationRequired) {
            AgeEligibilityEvaluation(
                decision = AgeEligibilityDecision.VerificationRequired,
                isAuthoritative = false
            )
        } else {
            evaluator.preservePriorRestriction(persistedRestriction)
        }
    }

    private fun readPersistedRestriction(): AgeRestrictionReason? {
        val typedRestriction = AgeRestrictionReason.entries.firstOrNull {
            it.name == prefsWrapper.userAgeRestrictionReason
        }
        return typedRestriction ?: if (prefsWrapper.isUserAgeEligibleForAppUse) {
            null
        } else {
            AgeRestrictionReason.LEGACY_RESTRICTION_UNKNOWN_REASON
        }
    }

    private fun AgeRestrictionReason?.toDecision(): AgeEligibilityDecision =
        this?.let(AgeEligibilityDecision::Restricted) ?: AgeEligibilityDecision.Allowed

    @StringRes
    private fun AgeRestrictionReason?.toMessage(): Int = if (this == AgeRestrictionReason.BELOW_MINIMUM_AGE) {
        R.string.age_restriction_user_below_tos_minimum_age_dialog_message
    } else {
        R.string.age_restriction_supervised_user_account_dialog_message
    }

    data class AgeEligibilityState(
        val decision: AgeEligibilityDecision,
        @StringRes val ageRestrictedTitle: Int,
        @StringRes val ageRestrictedMessage: Int
    ) {
        val isUserAgeRangeEligible: Boolean
            get() = decision is AgeEligibilityDecision.Allowed
    }
}
