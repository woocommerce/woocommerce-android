package com.woocommerce.android.extensions

import android.text.TextUtils
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.SiteUtils.getNormalizedTimezone
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import java.time.Clock
import java.time.ZoneId

val SiteModel.logInformation: String
    get() {
        val typeLog = "Type: ($stateLogInformation)"
        val planLog = "Plan: $planShortName ($planId)"
        val jetpackVersionLog = if (isJetpackInstalled) "Jetpack-version: $jetpackVersion" else ""
        return listOf(typeLog, planLog, jetpackVersionLog)
            .filter { it != "" }
            .joinToString(separator = " ", prefix = "<", postfix = ">")
    }

val SiteModel.stateLogInformation: String
    get() {
        return when {
            isWpComStore -> "Store on WP.com"
            else -> "Self-hosted + Jetpack"
        }
    }

fun SiteModel.getSiteName(): String = if (!TextUtils.isEmpty(name)) name else ""

fun SiteModel?.getTitle(default: String): String {
    return when {
        this == null -> default
        displayName.isNotNullOrEmpty() -> displayName
        name.isNotNullOrEmpty() -> name
        else -> default
    }
}

// The isWPCom property is set as true only for pure WPCom sites that don't have Jetpack connection
val SiteModel.isSimpleWPComSite
    get() = isWPCom

val SiteModel.adminUrlOrDefault
    get() = adminUrl ?: url.slashJoin("wp-admin")

val SiteModel.clock: Clock
    @Suppress("TooGenericExceptionCaught")
    get() {
        val javaUtilsTimeZone = getNormalizedTimezone(timezone)

        val zoneId = try {
            ZoneId.of(javaUtilsTimeZone.id)
        } catch (e: Exception) {
            WooLog.e(WooLog.T.UTILS, e)
            ZoneId.systemDefault()
        }

        return Clock.system(zoneId)
    }

const val ECOMMERCE_TRIAL_PLAN_ID = 1052L
val SiteModel.isCurrentPlanEcommerceTrial
    get() = planId == ECOMMERCE_TRIAL_PLAN_ID
