package com.woocommerce.android.ui.login

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Source.DEFAULT
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.LOGIN
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedLoginTracker
@Inject constructor(private val analyticsTracker: AnalyticsTrackerWrapper) {
    private var currentSource: Source = DEFAULT
        set(value) {
            AppPrefs.setUnifiedLoginLastSource(value.value)
            field = value
        }
    private var currentFlow: Flow? = null
        set(value) {
            value?.let { flow -> AppPrefs.setUnifiedLoginLastFlow(flow.value) }
            field = value
        }
    var currentStep: Step? = null
        private set

    // Saves the previous step before going to the Help step.
    // Used for tracking purposes on the Login Help Center feature.
    var previousStepBeforeHelpStep: Step? = null
        private set

    @JvmOverloads
    fun track(
        flow: Flow? = currentFlow,
        step: Step
    ) {
        currentFlow = flow
        if (step == Step.HELP) {
            previousStepBeforeHelpStep = currentStep
        }
        currentStep = step

        if (currentFlow != null && currentStep != null) {
            analyticsTracker.track(
                stat = AnalyticsEvent.UNIFIED_LOGIN_STEP,
                properties = buildDefaultParams()
            )
        } else {
            handleMissingFlowOrStep("step: ${step.value}")
        }
    }

    fun trackFailure(error: String?) {
        if (currentFlow != null && currentStep != null) {
            currentFlow.let {
                analyticsTracker.track(
                    stat = AnalyticsEvent.UNIFIED_LOGIN_FAILURE,
                    properties = buildDefaultParams().apply {
                        error?.let {
                            put(FAILURE, error)
                        }
                    }
                )
            }
        } else {
            handleMissingFlowOrStep("failure: $error")
        }
    }

    fun trackClick(click: Click) {
        if (currentFlow != null && currentStep != null) {
            currentFlow.let {
                analyticsTracker.track(
                    stat = AnalyticsEvent.UNIFIED_LOGIN_INTERACTION,
                    properties = buildDefaultParams().apply {
                        put(CLICK, click.value)
                    }
                )
            }
        } else {
            handleMissingFlowOrStep("click: ${click.value}")
        }
    }

    private fun buildDefaultParams(): MutableMap<String, String> {
        val params = mutableMapOf(SOURCE to currentSource.value)
        currentFlow?.let {
            params[FLOW] = it.value
        }
        currentStep?.let {
            params[STEP] = it.value
        }
        return params
    }

    private fun handleMissingFlowOrStep(value: String?) {
        val errorMessage = "Trying to log an event $value with a missing ${if (currentFlow == null) "flow" else "step"}"
        if (BuildConfig.DEBUG) {
            throw IllegalStateException(errorMessage)
        } else {
            WooLog.e(LOGIN, errorMessage)
        }
    }

    fun setSource(source: Source) {
        currentSource = source
    }

    fun setSource(value: String) {
        Source.values().find { it.value == value }?.let {
            currentSource = it
        }
    }

    fun setFlow(value: String?) {
        currentFlow = Flow.values().find { it.value == value }
    }

    fun setStep(step: Step) {
        currentStep = step
    }

    fun setFlowAndStep(flow: Flow, step: Step) {
        currentFlow = flow
        currentStep = step
    }

    fun getSource(): Source = currentSource
    fun getFlow(): Flow? = currentFlow

    enum class Source(val value: String) {
        JETPACK("jetpack"),
        SHARE("share"),
        DEEPLINK("deeplink"),
        REAUTHENTICATION("reauthentication"),
        SELF_HOSTED("self_hosted"),
        ADD_WORDPRESS_COM_ACCOUNT("add_wordpress_com_account"),
        DEFAULT("default")
    }

    enum class Flow(val value: String) {
        PROLOGUE("prologue"),
        WORDPRESS_COM("wordpress_com"),
        GOOGLE_LOGIN("google_login"),
        LOGIN_MAGIC_LINK("login_magic_link"),
        LOGIN_PASSWORD("login_password"),
        LOGIN_STORE_CREDS("login_store_creds"),
        LOGIN_SITE_ADDRESS("login_site_address"),
        SIGNUP("signup"),
        GOOGLE_SIGNUP("google_signup"),
        EPILOGUE("epilogue")
    }

    enum class Step(val value: String) {
        PROLOGUE_CAROUSEL("prologue_carousel"),
        PROLOGUE("prologue"),
        START("start"),
        MAGIC_LINK_REQUESTED("magic_link_requested"),
        ENTER_SITE_ADDRESS("enter_site_address"),
        ENTER_EMAIL_ADDRESS("enter_email_address"),
        EMAIL_OPENED("email_opened"),
        USERNAME_PASSWORD("username_password"),
        SUCCESS("success"),
        HELP("help"),
        SHOW_EMAIL_HINTS("show_email_hints"),
        CONNECTION_ERROR("connection_error"),
        WRONG_WP_ACCOUNT("wrong_wordpress_account"),
        NO_WOO_STORES("no_woo_stores"),
        SITE_LIST("site_list"),
        JETPACK_NOT_CONNECTED("jetpack_not_connected"),
        NOT_WOO_STORE("not_woo_store"),
        NO_WPCOM_ACCOUNT_FOUND("no_wpcom_account_found"),
        NOT_WORDPRESS_SITE("not_wordpress_site");

        companion object {
            private val valueMap = values().associateBy(Step::value)

            fun fromValue(value: String) = valueMap[value]
        }
    }

    enum class Click(val value: String) {
        SUBMIT("submit"),
        DISMISS("dismiss"),
        CONTINUE_WITH_WORDPRESS_COM("continue_with_wordpress_com"),
        LOGIN_WITH_SITE_ADDRESS("login_with_site_address"),
        LOGIN_WITH_GOOGLE("login_with_google"),
        FORGOTTEN_PASSWORD("forgotten_password"),
        OPEN_EMAIL_CLIENT("open_email_client"),
        SHOW_HELP("show_help"),
        SEND_CODE_WITH_TEXT("send_code_with_text"),
        SUBMIT_2FA_CODE("submit_2fa_code"),
        REQUEST_MAGIC_LINK("request_magic_link"),
        LOGIN_WITH_PASSWORD("login_with_password"),
        HELP_FINDING_SITE_ADDRESS("help_finding_site_address"),
        SELECT_EMAIL_FIELD("select_email_field"),
        PICK_EMAIL_FROM_HINT("pick_email_from_hint"),
        LOGIN_WITH_SITE_CREDS("login_with_site_creds"),
        VIEW_CONNECTED_STORES("view_connected_stores"),
        TRY_ANOTHER_ACCOUNT("try_another_account"),
        TRY_ANOTHER_STORE("try_another_store"),
        HELP_FINDING_CONNECTED_EMAIL("help_finding_connected_email"),
        REFRESH_APP("refresh_app"),
        HELP_TROUBLESHOOTING_TIPS("help_troubleshooting_tips"),
        TRY_AGAIN("try_again"),
        WHAT_IS_WORDPRESS_COM("what_is_wordpress_com"),
        WHAT_IS_WORDPRESS_COM_ON_INVALID_EMAIL_SCREEN("what_is_wordpress_com_on_invalid_email_screen"),
        CREATE_ACCOUNT("create_account")
    }

    companion object {
        private const val SOURCE = "source"
        private const val FLOW = "flow"
        private const val STEP = "step"
        private const val FAILURE = "failure"
        private const val CLICK = "click"
    }
}
