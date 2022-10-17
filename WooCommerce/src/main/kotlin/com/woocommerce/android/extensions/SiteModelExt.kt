package com.woocommerce.android.extensions

import android.text.TextUtils
import org.wordpress.android.fluxc.model.SiteModel

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
