package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MAIN
import javax.inject.Inject
import javax.inject.Singleton

private const val UNAUTHORIZED = 401
private const val CONFLICT = 409
private const val NOT_FOUND = 404

@Singleton
internal class ApplicationPasswordsManager @Inject constructor(
    private val applicationPasswordsStore: ApplicationPasswordsStore,
    private val jetpackApplicationPasswordsRestClient: JetpackApplicationPasswordsRestClient,
    private val wpApiApplicationPasswordsRestClient: WPApiApplicationPasswordsRestClient,
    private val configuration: ApplicationPasswordsConfiguration,
    private val currentTimeProvider: CurrentTimeProvider,
    private val appLogWrapper: AppLogWrapper
) {
    private val mutexLock = Mutex()
    private val validationMutex = Mutex()
    private var lastValidation: CachedValidation? = null

    private val applicationName
        get() = configuration.applicationName

    /**
     * Checks whether the site supports creating new Application Passwords using the API, and the different cases are:
     * 1. For Jetpack sites, we always can call the API using the WordPress.com token.
     * 2. For self-hosted sites, we need to check if we have persisted credentials, otherwise we can't create it. This
     *    case happens when a site's Application Password was saved directly to the [ApplicationPasswordsStore],
     *    which happens during the Web Authorization.
     */
    private val SiteModel.supportsApplicationPasswordsGeneration
        get() = origin == SiteModel.ORIGIN_WPCOM_REST ||
            (!username.isNullOrEmpty() && !password.isNullOrEmpty())

    suspend fun getApplicationCredentials(
        site: SiteModel
    ): ApplicationPasswordCreationResult = mutexLock.withLock {
        if (site.isWPCom) return ApplicationPasswordCreationResult.NotSupported(
            WPAPINetworkError(
                BaseNetworkError(
                    GenericErrorType.UNKNOWN,
                    "Simple WPCom sites don't support application passwords"
                )
            )
        )
        val existingPassword = applicationPasswordsStore.getCredentials(site)
        if (existingPassword != null) {
            return ApplicationPasswordCreationResult.Existing(existingPassword)
        }

        val usernamePayload = getOrFetchUsername(site)
        return if (usernamePayload.isError) {
            ApplicationPasswordCreationResult.Failure(usernamePayload.error)
        } else {
            createApplicationPassword(site, usernamePayload.userName).also {
                if (it is ApplicationPasswordCreationResult.Created) {
                    applicationPasswordsStore.saveCredentials(
                        site,
                        it.credentials
                    )
                }
            }
        }
    }

    /**
     * Decides whether a 401 received from an API endpoint means the saved application password has to be
     * replaced. A 401 is not proof of that on its own: plugins reuse the status code for their own
     * authorization failures, so the credentials are verified against the site before being deleted.
     *
     * When the check can't be completed we fall back to [supportsApplicationPasswordsGeneration], so that a
     * credential we wouldn't be able to replace is never destroyed on an inconclusive signal.
     */
    suspend fun shouldRegenerateApplicationPassword(
        site: SiteModel,
        credentials: ApplicationPasswordCredentials
    ): Boolean = when (checkValidity(site, credentials)) {
        ApplicationPasswordValidity.VALID -> {
            appLogWrapper.d(MAIN, "The application password is still valid, the 401 came from the endpoint")
            false
        }

        ApplicationPasswordValidity.INVALID -> {
            appLogWrapper.w(MAIN, "The application password was rejected by the site, it needs to be replaced")
            true
        }

        ApplicationPasswordValidity.UNKNOWN -> {
            appLogWrapper.w(MAIN, "Couldn't verify the application password")
            site.supportsApplicationPasswordsGeneration
        }
    }

    /**
     * A screen usually fans out several requests at once, so a single failing endpoint answers each of them
     * with its own 401. Serialize the check and reuse a fresh result so the site is asked once per burst
     * instead of once per response. The key includes the credentials, so a replaced password is never
     * matched against the previous verdict.
     *
     * The age is checked as a range because [currentTimeProvider] follows the wall clock: a backwards step
     * would otherwise leave the entry permanently "fresh".
     */
    private suspend fun checkValidity(
        site: SiteModel,
        credentials: ApplicationPasswordCredentials
    ): ApplicationPasswordValidity = validationMutex.withLock {
        val now = nowMillis()
        lastValidation
            ?.takeIf { it.matches(site, credentials) && now - it.checkedAt in 0 until VALIDATION_RESULT_TTL_MS }
            ?.let {
                appLogWrapper.d(MAIN, "Reusing the application password validity checked moments ago")
                return@withLock it.validity
            }

        wpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(site, credentials)
            .also { lastValidation = CachedValidation(site.id, credentials, it, checkedAt = nowMillis()) }
    }

    private fun nowMillis() = currentTimeProvider.currentDate().time

    /**
     * Drops the memoized verdict once a password is revoked on purpose, so neither it nor the credentials it
     * holds outlive the password itself.
     */
    private suspend fun forgetCachedValidity() = validationMutex.withLock {
        lastValidation = null
    }

    private suspend fun getOrFetchUsername(site: SiteModel): UsernameFetchPayload {
        return if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            jetpackApplicationPasswordsRestClient.fetchWPAdminUsername(site)
        } else {
            site.username?.let { UsernameFetchPayload(it) }
                ?: UsernameFetchPayload(
                    BaseNetworkError(
                        GenericErrorType.UNKNOWN,
                        "Username is missing for the site"
                    )
                )
        }
    }

    private suspend fun createApplicationPassword(
        site: SiteModel,
        username: String
    ): ApplicationPasswordCreationResult {
        if (!site.supportsApplicationPasswordsGeneration) {
            return ApplicationPasswordCreationResult.Failure(
                WPAPINetworkError(
                    BaseNetworkError(
                        GenericErrorType.NOT_AUTHENTICATED,
                        "Site password is missing. " +
                            "The application password was probably authorized using the Web flow",
                        VolleyError(
                            NetworkResponse(
                                UNAUTHORIZED, null, true, System.currentTimeMillis(), emptyList()
                            )
                        )
                    )
                )
            )
        }

        val payload = if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            jetpackApplicationPasswordsRestClient.createApplicationPassword(
                site = site,
                applicationName = applicationName
            )
        } else {
            wpApiApplicationPasswordsRestClient.createApplicationPassword(
                site = site,
                applicationName = applicationName
            )
        }

        return handleApplicationPasswordCreationResult(site, username, payload)
    }

    private suspend fun handleApplicationPasswordCreationResult(
        site: SiteModel,
        username: String,
        payload: ApplicationPasswordCreationPayload,
    ): ApplicationPasswordCreationResult {
        return when {
            !payload.isError -> ApplicationPasswordCreationResult.Created(
                ApplicationPasswordCredentials(
                    userName = username,
                    password = payload.password,
                    uuid = payload.uuid
                )
            )

            else -> {
                val statusCode = payload.error.volleyError?.networkResponse?.statusCode
                val errorCode = payload.error.let {
                    when (it) {
                        is WPComGsonNetworkError -> it.apiError
                        is WPAPINetworkError -> it.errorCode
                        else -> null
                    }
                }
                when {
                    statusCode == CONFLICT -> {
                        appLogWrapper.w(AppLog.T.MAIN, "Application Password already exists")
                        when (val deletionResult = deleteConflictedApplicationPassword(site)) {
                            ApplicationPasswordDeletionResult.Success ->
                                createApplicationPassword(site, username)

                            is ApplicationPasswordDeletionResult.Failure ->
                                ApplicationPasswordCreationResult.Failure(deletionResult.error)
                        }
                    }

                    statusCode == NOT_FOUND ||
                        errorCode == APPLICATION_PASSWORDS_DISABLED_ERROR_CODE ||
                        errorCode == APPLICATION_PASSWORDS_DISABLED_USER_ERROR_CODE -> {
                        appLogWrapper.w(
                            MAIN,
                            "Application Password feature not supported, " +
                                "status code: $statusCode, errorCode: $errorCode"
                        )
                        ApplicationPasswordCreationResult.NotSupported(payload.error)
                    }

                    else -> {
                        appLogWrapper.w(
                            AppLog.T.MAIN,
                            "Application Password creation failed ${payload.error.type}"
                        )
                        ApplicationPasswordCreationResult.Failure(payload.error)
                    }
                }
            }
        }
    }

    suspend fun deleteApplicationCredentials(
        site: SiteModel
    ): ApplicationPasswordDeletionResult {
        val credentials = applicationPasswordsStore.getCredentials(site)
        if (credentials == null) return ApplicationPasswordDeletionResult.Success

        val payload = wpApiApplicationPasswordsRestClient.deleteApplicationPassword(
            site = site,
            credentials = credentials
        )

        return payload.toResult().also {
            when (it) {
                is ApplicationPasswordDeletionResult.Success -> {
                    appLogWrapper.d(AppLog.T.MAIN, "Application password deleted")
                    applicationPasswordsStore.deleteCredentials(site)
                    forgetCachedValidity()
                }

                is ApplicationPasswordDeletionResult.Failure -> {
                    appLogWrapper.w(
                        AppLog.T.MAIN, "Application password deletion failed, error: " +
                                "${it.error.type} ${it.error.message}\n" +
                                "${it.error.volleyError?.toString()}"
                    )
                }
            }
        }
    }

    fun deleteLocalApplicationPassword(site: SiteModel, credentials: ApplicationPasswordCredentials) {
        if (applicationPasswordsStore.getCredentials(site) == credentials) {
            applicationPasswordsStore.deleteCredentials(site)
        }
    }

    private suspend fun deleteConflictedApplicationPassword(
        site: SiteModel
    ): ApplicationPasswordDeletionResult {
        suspend fun fetchApplicationPasswordUUID(
            site: SiteModel
        ): ApplicationPasswordUUIDFetchPayload {
            return if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
                jetpackApplicationPasswordsRestClient.fetchApplicationPasswordUUID(site, applicationName)
            } else {
                wpApiApplicationPasswordsRestClient.fetchApplicationPasswordUUID(site, applicationName)
            }
        }

        val uuid = fetchApplicationPasswordUUID(site).let {
            if (it.isError) return ApplicationPasswordDeletionResult.Failure(it.error)
            it.uuid
        }

        val result = if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            jetpackApplicationPasswordsRestClient.deleteApplicationPassword(
                site = site,
                uuid = uuid
            )
        } else {
            wpApiApplicationPasswordsRestClient.deleteApplicationPassword(
                site = site,
                uuid = uuid
            )
        }

        return result.toResult().also {
            if (it is ApplicationPasswordDeletionResult.Failure) {
                appLogWrapper.w(
                    AppLog.T.MAIN, "Conflicted application password deletion failed, error: " +
                        "${it.error.type} ${it.error.message}\n" +
                        "${it.error.volleyError?.toString()}"
                )
            } else {
                appLogWrapper.d(AppLog.T.MAIN, "Conflicted application password deleted")
            }
        }
    }

    private fun ApplicationPasswordDeletionPayload.toResult(): ApplicationPasswordDeletionResult =
        if (isError) {
            ApplicationPasswordDeletionResult.Failure(error)
        } else if (isDeleted) {
            ApplicationPasswordDeletionResult.Success
        } else {
            ApplicationPasswordDeletionResult.Failure(
                BaseNetworkError(
                    GenericErrorType.UNKNOWN,
                    "Deletion not confirmed by API"
                )
            )
        }

    private data class CachedValidation(
        val localSiteId: Int,
        val credentials: ApplicationPasswordCredentials,
        val validity: ApplicationPasswordValidity,
        /** When the check came back, not when it started — a slow check must not shorten the window. */
        val checkedAt: Long
    ) {
        fun matches(site: SiteModel, credentials: ApplicationPasswordCredentials) =
            localSiteId == site.id && this.credentials == credentials
    }

    companion object {
        private const val VALIDATION_RESULT_TTL_MS = 10_000L
        const val APPLICATION_PASSWORDS_DISABLED_ERROR_CODE = "application_passwords_disabled"
        const val APPLICATION_PASSWORDS_DISABLED_USER_ERROR_CODE = "application_passwords_disabled_for_user"
    }
}
