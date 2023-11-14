package com.woocommerce.android.ui.jitm

import android.content.Context
import com.google.gson.Gson
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import org.wordpress.android.fluxc.store.JitmStore
import javax.inject.Inject

class JitmStoreWrapper @Inject constructor(
    private val realStore: JitmStore,
    private val wrapperData: JitmStoreWrapperData,
    private val jsonReader: JitmStoreWrapperJsonReader,
) {
    @Suppress("TooGenericExceptionCaught")
    suspend fun fetchJitmMessage(
        site: SiteModel,
        messagePath: String,
        query: String,
    ): WooResult<Array<JITMApiResponse>> {
        return if (wrapperData.isTestingModeEnabled) {
            val jsonFileName = wrapperData.jsonFileName
            delay(RESPONSE_DELAY)
            try {
                WooLog.d(WooLog.T.JITM, "Using JITM JSON file: $jsonFileName")
                WooResult(jsonReader.parseJsonFile(jsonFileName))
            } catch (e: Exception) {
                WooLog.e(WooLog.T.JITM, e)
                error("Failed to parse JITM JSON file: $jsonFileName")
            }
        } else {
            realStore.fetchJitmMessage(site, messagePath, query)
        }
    }

    suspend fun dismissJitmMessage(
        site: SiteModel,
        jitmId: String,
        featureClass: String,
    ): WooResult<Boolean> {
        return if (wrapperData.isTestingModeEnabled) {
            WooLog.d(WooLog.T.JITM, "Dismissing JITM message in test mode")
            WooResult(true)
        } else {
            realStore.dismissJitmMessage(site, jitmId, featureClass)
        }
    }

    private companion object {
        private const val RESPONSE_DELAY = 1000L
    }
}

class JitmStoreWrapperJsonReader @Inject constructor(
    private val context: Context,
    private val gson: Gson,
) {
    fun parseJsonFile(fileName: String): Array<JITMApiResponse>? {
        val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
        return gson.fromJson(json, Array<JITMApiResponse>::class.java)
    }
}

class JitmStoreWrapperData @Inject constructor() {
    val jsonFileName = BuildConfig.JITM_TESTING_JSON_FILE_NAME
    val isTestingModeEnabled = PackageUtils.isDebugBuild() && jsonFileName.isNotBlank()
}
