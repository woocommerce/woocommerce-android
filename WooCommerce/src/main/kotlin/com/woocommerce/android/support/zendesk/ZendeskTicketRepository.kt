package com.woocommerce.android.support.zendesk

import android.content.Context
import android.os.Parcelable
import com.woocommerce.android.applicationpasswords.IsAppPasswordsSupportedForJetpackSite
import com.woocommerce.android.extensions.formatResult
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.zendesk.RequestConstants.APPLICATION_LOG_FILENAME
import com.woocommerce.android.support.zendesk.RequestConstants.DIAGNOSTIC_LOG_FILENAME
import com.woocommerce.android.support.zendesk.RequestConstants.MSR_FILENAME
import com.woocommerce.android.support.zendesk.RequestConstants.requestCreationIdentityNotSetErrorMessage
import com.woocommerce.android.support.zendesk.RequestConstants.requestCreationTimeoutErrorMessage
import com.woocommerce.android.support.zendesk.ZendeskException.IdentityNotSetException
import com.woocommerce.android.support.zendesk.ZendeskException.RequestCreationFailedException
import com.woocommerce.android.support.zendesk.ZendeskException.RequestCreationTimeoutException
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WCSSRModelCachingFetcher
import com.woocommerce.android.util.WooLog
import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import zendesk.support.CreateRequest
import zendesk.support.CustomField
import zendesk.support.Request
import zendesk.support.UploadResponse
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume

class ZendeskTicketRepository @Inject constructor(
    private val zendeskSettings: ZendeskSettings,
    private val envDataSource: ZendeskEnvironmentDataSource,
    private val siteStore: SiteStore,
    private val dispatchers: CoroutineDispatchers,
    private val wooLog: WooLog,
    private val ssrFetcher: WCSSRModelCachingFetcher,
    private val isAppPasswordsSupportedForJetpackSite: IsAppPasswordsSupportedForJetpackSite,
    private val mobileStatusProvider: MobileStatusProvider
) {
    /**
     * This function creates a new customer Support Request through the Zendesk API Providers.
     */
    @Suppress("LongParameterList")
    fun createRequest(
        context: Context,
        origin: HelpOrigin,
        ticketType: TicketType,
        selectedSite: SiteModel?,
        subject: String,
        description: String,
        extraTags: List<String>,
        siteAddress: String,
        diagnosticLog: String? = null
    ) = callbackFlow {
        if (zendeskSettings.isIdentitySet.not()) {
            trySend(Result.failure(IdentityNotSetException))
            close()
            return@callbackFlow
        }

        val tags = (ticketType.tags + extraTags)

        val ssr: String? = selectedSite?.let { fetchSSR(it) }

        val msr = mobileStatusProvider(selectedSite, siteAddress)

        val deviceLogs = envDataSource.getFullDeviceLogs()

        val attachmentTokens = coroutineScope {
            listOfNotNull(
                diagnosticLog?.let { async { uploadTextAttachment(DIAGNOSTIC_LOG_FILENAME, it) } },
                async { uploadTextAttachment(APPLICATION_LOG_FILENAME, deviceLogs) },
                async { uploadTextAttachment(MSR_FILENAME, msr) }
            ).awaitAll().filterNotNull()
        }

        val requestCallback = object : ZendeskCallback<Request>() {
            override fun onSuccess(result: Request?) {
                trySend(Result.success(result))
                close()
            }

            override fun onError(error: ErrorResponse) {
                trySend(Result.failure(RequestCreationFailedException(error.reason)))
                close()
            }
        }

        CreateRequest().apply {
            this.ticketFormId = ticketType.form
            this.subject = subject
            this.description = description
            if (attachmentTokens.isNotEmpty()) {
                this.attachments = attachmentTokens
            }
            this.tags = buildZendeskTags(selectedSite, siteStore.sites, origin, tags)
                .filter { ticketType.excludedTags.contains(it).not() }
            val zendeskCustomFieldsParams = ZendeskCustomFieldsParams(
                context = context,
                ticketType = ticketType,
                allSites = siteStore.sites,
                selectedSite = selectedSite,
                ssr = ssr,
                siteAddress = siteAddress,
                deviceLogs = deviceLogs,
                msr = msr
            )

            this.customFields = buildZendeskCustomFields(zendeskCustomFieldsParams)
        }.let { request -> zendeskSettings.requestProvider?.createRequest(request, requestCallback) }

        // Sets a timeout since the callback might not be called from Zendesk API
        launch {
            delay(RequestConstants.requestCreationTimeout)
            trySend(Result.failure(RequestCreationTimeoutException))
            close()
        }

        awaitClose()
    }.flowOn(dispatchers.io)

    private suspend fun fetchSSR(selectedSite: SiteModel): String? {
        wooLog.i(WooLog.T.SUPPORT, "Fetching SSR")
        val result = ssrFetcher.load(selectedSite)
        if (result.isError) {
            wooLog.e(WooLog.T.SUPPORT, "Error fetching SSR")
        } else {
            wooLog.i(WooLog.T.SUPPORT, "SSR fetched successfully")
        }
        return result.model?.formatResult()
    }

    private suspend fun uploadTextAttachment(
        fileName: String,
        content: String
    ): String? {
        // Preparing the file is best-effort: an I/O failure (e.g. low disk space) must not block
        // ticket creation, so we log it and continue without the attachment.
        val tempFile = withContext(dispatchers.io) {
            runCatching {
                File.createTempFile(fileName, null).also { it.writeText(content) }
            }.onFailure {
                wooLog.e(WooLog.T.SUPPORT, "Failed to prepare attachment file: $fileName")
            }.getOrNull()
        } ?: return null

        return try {
            // Guard against the Zendesk SDK never invoking either callback, which would otherwise
            // block ticket creation indefinitely. On timeout we treat it as a failed upload (null).
            withTimeoutOrNull(RequestConstants.attachmentUploadTimeout) {
                suspendCancellableCoroutine { continuation ->
                    zendeskSettings.uploadProvider?.uploadAttachment(
                        fileName,
                        tempFile,
                        "text/plain",
                        object : ZendeskCallback<UploadResponse>() {
                            override fun onSuccess(result: UploadResponse?) {
                                if (continuation.isActive) {
                                    continuation.resume(result?.token)
                                }
                            }

                            override fun onError(error: ErrorResponse?) {
                                wooLog.e(WooLog.T.SUPPORT, "Failed to upload attachment: $fileName")
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        }
                    ) ?: run {
                        continuation.resume(null)
                    }
                }
            }
        } finally {
            tempFile.delete()
        }
    }

    /**
     * This is a helper function which builds a list of `CustomField`s which will be used during ticket creation. They
     * will be used to fill the custom fields we have setup in Zendesk UI for Happiness Engineers.
     */
    private suspend fun buildZendeskCustomFields(params: ZendeskCustomFieldsParams): List<CustomField> {
        return listOf(
            CustomField(TicketCustomField.appVersion, envDataSource.generateVersionName(params.context)),
            CustomField(TicketCustomField.deviceFreeSpace, envDataSource.totalAvailableMemorySize),
            CustomField(TicketCustomField.networkInformation, envDataSource.generateNetworkInformation(params.context)),
            CustomField(TicketCustomField.logs, envDataSource.trimDeviceLogs(params.deviceLogs)),
            CustomField(TicketCustomField.ssr, params.ssr),
            CustomField(TicketCustomField.currentSite, envDataSource.generateHostData(params.selectedSite)),
            CustomField(TicketCustomField.sourcePlatform, ZendeskEnvironmentDataSource.sourcePlatform),
            CustomField(TicketCustomField.appLanguage, envDataSource.deviceLanguage),
            CustomField(TicketCustomField.categoryId, params.ticketType.categoryName),
            CustomField(TicketCustomField.subcategoryId, params.ticketType.subcategoryName),
            CustomField(
                TicketCustomField.blogList,
                envDataSource.generateCombinedLogInformationOfSites(params.allSites)
            ),
            CustomField(TicketCustomField.siteAddress, params.siteAddress),
            CustomField(TicketCustomField.msr, params.msr)
        )
    }

    /**
     * This is a helper function which returns a set of pre-defined tags depending on some conditions. It accepts a list of
     * custom tags to be added for special cases.
     */
    private suspend fun buildZendeskTags(
        selectedSite: SiteModel?,
        allSites: List<SiteModel>?,
        origin: HelpOrigin,
        extraTags: List<String>
    ): List<String> {
        val tags = ArrayList<String>()
        selectedSite?.let {
            if (selectedSite.connectionType == SiteConnectionType.ApplicationPasswords) {
                tags.add(ZendeskTags.applicationPasswordAuthenticated)
            } else if (isAppPasswordsSupportedForJetpackSite(it)) {
                tags.add(ZendeskTags.jetpackSiteUsingAppPasswords)
            }
        }

        allSites?.let { it ->
            // Add wpcom tag if at least one site is WordPress.com site
            if (it.any { it.isWPCom }) {
                tags.add(ZendeskTags.wpComTag)
            }

            // Add Jetpack tag if at least one site is Jetpack connected. Even if a site is Jetpack connected,
            // it does not necessarily mean that user is connected with the REST API, but we don't care about that here
            if (it.any { it.isJetpackConnected }) {
                tags.add(ZendeskTags.jetpackTag)
            }

            // Find distinct plans and add them
            val plans = it.asSequence().mapNotNull { it.planShortName }.distinct().toList()
            tags.addAll(plans)
        }
        tags.add(origin.toString())
        // We rely on this platform tag to filter tickets in Zendesk
        tags.add(ZendeskTags.platformTag)
        tags.addAll(extraTags)
        return tags
    }
}

sealed class TicketType(
    val form: Long,
    val categoryName: String,
    val subcategoryName: String,
    val tags: List<String> = emptyList(),
    val excludedTags: List<String> = emptyList()
) : Parcelable {
    @Parcelize
    object MobileApp : TicketType(
        form = TicketCustomField.wooMobileFormID,
        categoryName = ZendeskConstants.mobileAppCategory,
        subcategoryName = ZendeskConstants.mobileSubcategoryValue,
        tags = listOf(ZendeskTags.mobileApp)
    )

    @Parcelize
    object InPersonPayments : TicketType(
        form = TicketCustomField.wooMobileFormID,
        categoryName = ZendeskConstants.mobileAppCategory,
        subcategoryName = ZendeskConstants.mobileSubcategoryValue,
        tags = listOf(
            ZendeskTags.woocommerceMobileApps,
            ZendeskTags.productAreaAppsInPersonPayments
        )
    )

    @Parcelize
    object Payments : TicketType(
        form = TicketCustomField.wooFormID,
        categoryName = ZendeskConstants.supportCategory,
        subcategoryName = ZendeskConstants.paymentsSubcategoryValue,
        tags = listOf(
            ZendeskTags.paymentsProduct,
            ZendeskTags.paymentsProductArea,
            ZendeskTags.mobileAppWooTransfer,
            ZendeskTags.supportCategoryTag,
            ZendeskTags.paymentSubcategoryTag
        ),
        excludedTags = listOf(ZendeskTags.jetpackTag)
    )

    @Parcelize
    object WooPlugin : TicketType(
        form = TicketCustomField.wooFormID,
        categoryName = ZendeskConstants.supportCategory,
        subcategoryName = "",
        tags = listOf(
            ZendeskTags.woocommerceCore,
            ZendeskTags.mobileAppWooTransfer,
            ZendeskTags.supportCategoryTag
        ),
        excludedTags = listOf(ZendeskTags.jetpackTag)
    )

    @Parcelize
    object OtherPlugins : TicketType(
        form = TicketCustomField.wooFormID,
        categoryName = ZendeskConstants.supportCategory,
        subcategoryName = ZendeskConstants.storeSubcategoryValue,
        tags = listOf(
            ZendeskTags.productAreaWooExtensions,
            ZendeskTags.mobileAppWooTransfer,
            ZendeskTags.supportCategoryTag,
            ZendeskTags.storeSubcategoryTag
        ),
        excludedTags = listOf(ZendeskTags.jetpackTag)
    )
}

private object ZendeskConstants {
    const val supportCategory = "Support"
    const val mobileAppCategory = "Mobile App"
    const val mobileSubcategoryValue = "WooCommerce Mobile Apps"
    const val paymentsSubcategoryValue = "Payment"
    const val storeSubcategoryValue = "Store"
}

object TicketCustomField {
    const val appVersion = 360000086866L
    const val blogList = 360000087183L
    const val deviceFreeSpace = 360000089123L
    const val wooMobileFormID = 360000010286L
    const val wooFormID = 189946L
    const val categoryId = 25176003L
    const val subcategoryId = 25176023L
    const val logs = 10901699622036L

    // SSR refers to WooCommerce System Status Report
    const val ssr = 22871957L
    const val networkInformation = 360000086966L
    const val currentSite = 360000103103L
    const val appLanguage = 360008583691L
    const val sourcePlatform = 360009311651L
    const val siteAddress = 22054927L

    // MSR refers to the Mobile Status Report, the app-level counterpart to the SSR above.
    const val msr = 51884914117268L
}

object ZendeskTags {
    const val applicationPasswordAuthenticated = "application_password_authenticated"
    const val jetpackSiteUsingAppPasswords = "jetpack_site_using_app_passwords"
    const val mobileApp = "mobile_app"
    const val woocommerceCore = "woocommerce_core"
    const val paymentsProduct = "woocommerce_payments"
    const val paymentsProductArea = "product_area_woo_payment_gateway"
    const val mobileAppWooTransfer = "mobile_app_woo_transfer"
    const val woocommerceMobileApps = "woocommerce_mobile_apps"
    const val productAreaWooExtensions = "product_area_woo_extensions"
    const val productAreaAppsInPersonPayments = "product_area_apps_in_person_payments"
    const val storeSubcategoryTag = "store"
    const val supportCategoryTag = "support"
    const val paymentSubcategoryTag = "payment"
    const val jetpackTag = "jetpack"
    const val platformTag = "Android"
    const val wpComTag = "wpcom"
    const val freeTrialTag = "trial"
}

sealed class ZendeskException(message: String) : Exception(message) {
    object IdentityNotSetException : ZendeskException(requestCreationTimeoutErrorMessage)
    object RequestCreationTimeoutException : ZendeskException(requestCreationIdentityNotSetErrorMessage)
    data class RequestCreationFailedException(private val errorMessage: String) : ZendeskException(errorMessage)
}

private object RequestConstants {
    const val requestCreationTimeout = 10000L
    const val attachmentUploadTimeout = 30000L
    const val requestCreationTimeoutErrorMessage = "Request creation timed out"
    const val requestCreationIdentityNotSetErrorMessage = "Request creation failed: identity not set"
    const val DIAGNOSTIC_LOG_FILENAME = "connectivitytest_log.txt"
    const val APPLICATION_LOG_FILENAME = "application_log.txt"
    const val MSR_FILENAME = "mobile_status_report.txt"
}

private data class ZendeskCustomFieldsParams(
    val context: Context,
    val ticketType: TicketType,
    val allSites: List<SiteModel>?,
    val selectedSite: SiteModel?,
    val ssr: String?,
    val siteAddress: String,
    val deviceLogs: String,
    val msr: String
)
