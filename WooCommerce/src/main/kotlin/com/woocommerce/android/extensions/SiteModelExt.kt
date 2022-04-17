package com.woocommerce.android.extensions

import android.text.TextUtils
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SiteUiModel
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

fun List<SiteModel>.toAppUiModel(selectedSite: SiteModel): List<SiteUiModel> {
    return this.map {
        SiteUiModel(
            site = it,
            isSelected = selectedSite.id == it.id
        )
    }
}
