package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.NetworkResponse
import com.android.volley.NoConnectionError
import com.android.volley.RequestQueue
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticationEndpoints.AdminBaseVerification
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Available
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.CookieNonceErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.FailedRequest
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.Unknown
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import org.wordpress.android.util.HtmlUtils
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val NOT_FOUND_STATUS_CODE = 404
private const val MAX_ENDPOINT_REDIRECTS = 3
private typealias ValidatedEndpoints = CookieNonceAuthenticationEndpoints.ValidationResult.Valid

@Singleton
class NonceRestClient @Inject constructor(
    private val wpApiEncodedBodyRequestBuilder: WPAPIEncodedBodyRequestBuilder,
    private val currentTimeProvider: CurrentTimeProvider,
    dispatcher: Dispatcher,
    @Named("no-redirects") requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {
    private val nonceMap: MutableMap<String, Nonce> = mutableMapOf()

    fun getNonce(siteUrl: String, username: String?): Nonce? =
        nonceMap[siteUrl.nonceCacheIdentity()]?.takeIf { it.username == username }

    fun getNonce(site: SiteModel): Nonce? = getNonce(site.url, site.username)

    suspend fun requestNonce(site: SiteModel): Nonce {
        if (site.username == null || site.password == null) return Unknown(site.username)
        return requestNonce(CookieNonceAuthenticationEndpoints.from(site), site.username, site.password)
    }

    suspend fun requestNonce(siteUrl: String, username: String, password: String) =
        requestNonce(CookieNonceAuthenticationEndpoints(siteUrl), username, password)

    suspend fun requestNonce(
        endpoints: CookieNonceAuthenticationEndpoints,
        username: String,
        password: String
    ): Nonce {
        val validated = when (val result = endpoints.validate()) {
            is CookieNonceAuthenticationEndpoints.ValidationResult.Valid -> result
            is CookieNonceAuthenticationEndpoints.ValidationResult.Invalid -> {
                val type = if (result.endpoint == CookieNonceAuthenticationEndpoints.Endpoint.ADMIN) {
                    CookieNonceErrorType.CUSTOM_ADMIN_URL
                } else {
                    CookieNonceErrorType.CUSTOM_LOGIN_URL
                }
                return cache(endpoints.canonicalSiteOrigin, failed(username, type))
            }
        }
        val derivedNonceUrl = validated.adminBaseUrl.toString()
            .slashJoin("$ADMIN_AJAX_PATH_SEGMENT?$REST_NONCE_QUERY")
            .toNetworkUrlOrNull()
            ?: return cache(endpoints.canonicalSiteOrigin, failed(username, CookieNonceErrorType.CUSTOM_ADMIN_URL))

        val preflight = when (val result = preflightLogin(validated, username)) {
            is LoginPreflight.Success -> result
            is LoginPreflight.Failure -> return cache(endpoints.canonicalSiteOrigin, result.nonce)
        }
        val nonceUrl = derivedNonceUrl.upgradeForSecureLogin(
            preflight.transactionUrl,
            validated.canonicalSiteOrigin
        )
        val nonce = when (preflight) {
            is LoginPreflight.LoginForm -> postCredentials(
                validated,
                endpoints.adminBaseVerification,
                preflight.transactionUrl,
                nonceUrl,
                username,
                password
            )
            is LoginPreflight.AlreadyAuthenticated -> verifyAdminDashboardAndRequestNonce(
                validated,
                endpoints.adminBaseVerification,
                preflight.transactionUrl,
                nonceUrl,
                username
            )
        }
        return cache(endpoints.canonicalSiteOrigin, nonce.withVerifiedLoginEntry())
    }

    private suspend fun preflightLogin(endpoints: ValidatedEndpoints, username: String): LoginPreflight {
        var currentUrl = endpoints.loginEntryUrl
        var redirectsFollowed = 0
        while (true) {
            when (val response = wpApiEncodedBodyRequestBuilder.syncGetRequest(this, currentUrl.toString())) {
                is Success -> {
                    val expectedAdminUrl = endpoints.adminBaseUrl.upgradeForSecureLogin(
                        currentUrl,
                        endpoints.canonicalSiteOrigin
                    )
                    val isExpectedAdminUrl = currentUrl.asReusableAdminBase() == expectedAdminUrl
                    if (response.data.isWordPressAdminDashboard()) {
                        return if (isExpectedAdminUrl) LoginPreflight.AlreadyAuthenticated(currentUrl)
                        else LoginPreflight.Failure(failed(username, CookieNonceErrorType.CUSTOM_ADMIN_URL))
                    }
                    if (isExpectedAdminUrl) return LoginPreflight.AlreadyAuthenticated(currentUrl)
                    val submissionUrl = response.data.loginFormSubmissionUrl(
                        currentUrl,
                        endpoints.canonicalSiteOrigin
                    ) ?: return LoginPreflight.Failure(failed(username, CookieNonceErrorType.INVALID_RESPONSE))
                    return LoginPreflight.LoginForm(submissionUrl)
                }
                is Error -> {
                    if (response.error.volleyError is NoConnectionError) {
                        return LoginPreflight.Failure(Unknown(username))
                    }
                    val networkResponse = response.error.volleyError?.networkResponse
                    if (networkResponse?.statusCode?.isRedirect() != true) {
                        return LoginPreflight.Failure(loginFailure(username, response, networkResponse))
                    }
                    if (redirectsFollowed == MAX_ENDPOINT_REDIRECTS) {
                        return LoginPreflight.Failure(
                            failed(username, CookieNonceErrorType.CUSTOM_LOGIN_URL, response)
                        )
                    }
                    val redirectUrl = networkResponse.location()
                        ?.let(currentUrl::resolve)
                        ?.withoutFragment()
                        ?.takeIf { it.isSafeFor(endpoints.canonicalSiteOrigin, currentUrl) }
                        ?: return LoginPreflight.Failure(
                            failed(username, CookieNonceErrorType.CUSTOM_LOGIN_URL, response)
                        )
                    currentUrl = redirectUrl
                    redirectsFollowed++
                }
            }
        }
    }

    private suspend fun postCredentials(
        endpoints: ValidatedEndpoints,
        verification: AdminBaseVerification,
        loginUrl: HttpUrl,
        nonceUrl: HttpUrl,
        username: String,
        password: String
    ): Nonce {
        return when (val response = wpApiEncodedBodyRequestBuilder.syncPostRequest(
            restClient = this,
            url = loginUrl.toString(),
            body = mapOf(
                "log" to username,
                "pwd" to password,
                "redirect_to" to nonceUrl.toString()
            )
        )) {
            is Success -> loginBodyFailure(response, username)
            is Error -> handleCredentialError(response, endpoints, verification, loginUrl, nonceUrl, username)
        }
    }

    private suspend fun handleCredentialError(
        response: Error<String>,
        endpoints: ValidatedEndpoints,
        verification: AdminBaseVerification,
        loginUrl: HttpUrl,
        nonceUrl: HttpUrl,
        username: String
    ): Nonce {
        if (response.error.volleyError is NoConnectionError) return Unknown(username)

        val networkResponse = response.error.volleyError?.networkResponse
        if (networkResponse?.statusCode?.isRedirect() != true) {
            return loginFailure(username, response, networkResponse)
        }

        val redirectUrl = networkResponse.location()
            ?.let(loginUrl::resolve)
            ?.withoutFragment()
            ?.takeIf { it.isSafeFor(endpoints.canonicalSiteOrigin, loginUrl) }
        return when {
            redirectUrl == nonceUrl -> verifyAdminDashboardAndRequestNonce(
                endpoints, verification, loginUrl, nonceUrl, username
            )
            redirectUrl?.isNonceEndpoint() == true -> {
                failed(username, CookieNonceErrorType.CUSTOM_ADMIN_URL, response)
            }
            else -> failed(username, CookieNonceErrorType.INVALID_NONCE, response)
        }
    }

    private suspend fun verifyAdminDashboardAndRequestNonce(
        endpoints: ValidatedEndpoints,
        verification: AdminBaseVerification,
        loginUrl: HttpUrl,
        nonceUrl: HttpUrl,
        username: String
    ): Nonce {
        if (verification == AdminBaseVerification.AUTHENTICATED_DASHBOARD) {
            val dashboardUrl = endpoints.adminBaseUrl.upgradeForSecureLogin(loginUrl, endpoints.canonicalSiteOrigin)
            verifyAdminDashboard(endpoints, dashboardUrl, username)?.let { return it }
        }
        return requestNonce(nonceUrl, username)
    }

    private suspend fun verifyAdminDashboard(
        endpoints: ValidatedEndpoints,
        initialUrl: HttpUrl,
        username: String
    ): Nonce? {
        var currentUrl = initialUrl
        var redirectsFollowed = 0
        while (true) {
            when (val response = wpApiEncodedBodyRequestBuilder.syncGetRequest(this, currentUrl.toString())) {
                is Success -> {
                    return if (response.data.isWordPressAdminDashboard()) null
                    else failed(username, CookieNonceErrorType.CUSTOM_ADMIN_URL)
                }
                is Error -> {
                    if (response.error.volleyError is NoConnectionError) {
                        return Unknown(username)
                    }
                    val networkResponse = response.error.volleyError?.networkResponse
                    if (networkResponse?.statusCode?.isRedirect() != true) {
                        return failed(username, CookieNonceErrorType.CUSTOM_ADMIN_URL, response)
                    }
                    if (redirectsFollowed == MAX_ENDPOINT_REDIRECTS) {
                        return failed(username, CookieNonceErrorType.CUSTOM_ADMIN_URL, response)
                    }
                    val redirectUrl = networkResponse.location()
                        ?.let(currentUrl::resolve)
                        ?.withoutFragment()
                        ?.takeIf { it.isSafeFor(endpoints.canonicalSiteOrigin, currentUrl) }
                        ?: return failed(username, CookieNonceErrorType.CUSTOM_ADMIN_URL, response)
                    currentUrl = redirectUrl
                    redirectsFollowed++
                }
            }
        }
    }

    private fun loginBodyFailure(response: Success<String>, username: String): Nonce {
        val responseBody = response.data.orEmpty()
        val errorMessage = extractErrorMessage(responseBody)
        val errorType = if (responseBody.contains(INVALID_CREDENTIAL_HTML_PATTERN) &&
            errorMessage?.contains("captcha", ignoreCase = true) != true
        ) {
            CookieNonceErrorType.INVALID_CREDENTIALS
        } else {
            CookieNonceErrorType.INVALID_RESPONSE
        }
        return failed(username, errorType, errorMessage = errorMessage)
    }

    private fun loginFailure(
        username: String,
        response: Error<String>,
        networkResponse: NetworkResponse?
    ): Nonce = failed(username, getLoginErrorType(networkResponse), response)

    private fun getLoginErrorType(networkResponse: NetworkResponse?): CookieNonceErrorType = when {
        networkResponse?.statusCode == NOT_FOUND_STATUS_CODE ||
            networkResponse?.statusCode == 410 -> CookieNonceErrorType.CUSTOM_LOGIN_URL
        isBasicAuthError(networkResponse) -> CookieNonceErrorType.BASIC_AUTH_REQUIRED
        else -> CookieNonceErrorType.GENERIC_ERROR
    }

    private fun failed(
        username: String,
        type: CookieNonceErrorType,
        response: Error<String>? = null,
        errorMessage: String? = response?.error?.message
    ) = FailedRequest(
        timeOfResponse = currentTimeProvider.currentDate().time,
        username = username,
        type = type,
        networkError = response?.error,
        errorMessage = errorMessage
    )

    private fun isBasicAuthError(networkResponse: NetworkResponse?): Boolean =
        networkResponse?.headers?.keys?.any { it.equals(AUTH_HEADER_KEY, ignoreCase = true) } == true &&
            networkResponse.headers?.values?.any { it.contains(BASIC_AUTH_REALM, ignoreCase = true) } == true

    private suspend fun requestNonce(nonceUrl: HttpUrl, username: String): Nonce {
        return when (val response = wpApiEncodedBodyRequestBuilder.syncGetRequest(this, nonceUrl.toString())) {
            is Success -> if (response.data?.matches("[0-9a-zA-Z]{2,}".toRegex()) == true) {
                Available(value = response.data, username = username)
            } else failed(username, CookieNonceErrorType.INVALID_NONCE)

            is Error -> {
                val statusCode = response.error.volleyError?.networkResponse?.statusCode
                val errorType = if (statusCode == NOT_FOUND_STATUS_CODE) {
                    CookieNonceErrorType.CUSTOM_ADMIN_URL
                } else {
                    CookieNonceErrorType.GENERIC_ERROR
                }
                failed(username, errorType, response)
            }
        }
    }

    private fun String.toNetworkUrlOrNull() = toHttpUrlOrNull()?.takeIf { it.scheme == "http" || it.scheme == "https" }

    private fun HttpUrl.isSafeFor(canonicalOrigin: HttpUrl, previousUrl: HttpUrl? = null): Boolean {
        return CookieNonceAuthenticationEndpoints.isSafeFor(this, canonicalOrigin, previousUrl)
    }

    private fun HttpUrl.upgradeForSecureLogin(loginUrl: HttpUrl, canonicalOrigin: HttpUrl): HttpUrl {
        return if (canonicalOrigin.scheme == "http" && canonicalOrigin.port == HTTP_DEFAULT_PORT &&
            scheme == "http" && port == HTTP_DEFAULT_PORT && loginUrl.scheme == "https"
        ) {
            newBuilder().scheme("https").port(HTTPS_DEFAULT_PORT).build()
        } else {
            this
        }
    }

    private fun HttpUrl.withoutFragment(): HttpUrl = newBuilder().fragment(null).build()

    private fun HttpUrl.asReusableAdminBase(): HttpUrl = takeUnless {
        encodedPathSegments.lastOrNull().equals(ADMIN_INDEX, ignoreCase = true)
    } ?: newBuilder().removePathSegment(encodedPathSegments.lastIndex).addPathSegment("").build()

    private fun HttpUrl.isNonceEndpoint(): Boolean =
        encodedPathSegments.lastOrNull() == ADMIN_AJAX_PATH_SEGMENT &&
            encodedQuery == REST_NONCE_QUERY

    private fun Int.isRedirect(): Boolean = this in 300..399

    private fun NetworkResponse.location(): String? = allHeaders
        ?.firstOrNull { it.name.equals(LOCATION_HEADER, ignoreCase = true) }
        ?.value

    private fun <T : Nonce> cache(siteIdentity: String, nonce: T): T = nonce.also {
        nonceMap[siteIdentity.nonceCacheIdentity()] = it
    }

    private fun Nonce.withVerifiedLoginEntry(): Nonce = when (this) {
        is Available -> this
        is FailedRequest -> copy(loginEntryVerified = true)
        is Unknown -> copy(loginEntryVerified = true)
    }

    private fun String.nonceCacheIdentity(): String {
        val siteBase = toNetworkUrlOrNull()?.withoutFragment() ?: return this
        val normalizedPath = siteBase.encodedPath.trimEnd('/').ifEmpty { "/" }
        return siteBase.newBuilder().encodedPath(normalizedPath).build().toString()
    }

    private fun extractErrorMessage(htmlResponse: String): String? {
        val regex = Regex("<div[^>]*id=\"login_error\"[^>]*>([\\s\\S]+?)</div>")
        val loginErrorDiv = regex.find(htmlResponse)?.groupValues?.get(1) ?: return null
        val urlRegex = Regex("<a[^>]*href=\".*\"[^>]*>[\\s\\S]+?</a>")
        val errorHtml = loginErrorDiv.replace(urlRegex, "")
        return HtmlUtils.fastStripHtml(errorHtml).trim(' ', '\n')
    }

    private fun String?.loginFormSubmissionUrl(documentUrl: HttpUrl, canonicalOrigin: HttpUrl): HttpUrl? {
        val renderedHtml = this?.replace(NON_RENDERED_REGION_PATTERN, "") ?: return null
        if (NON_RENDERED_MARKER_PATTERN.containsMatchIn(renderedHtml)) return null

        val forms = FORM_REGION_PATTERN.findAll(renderedHtml).toList()
        if (FORM_TAG_PATTERN.findAll(renderedHtml).count() != forms.size * 2) return null
        val credentialForms = forms.map { form ->
            val formAttributes = form.groupValues[1].htmlAttributes() ?: return null
            val inputAttributes = form.groupValues[2].inputAttributes() ?: return null
            formAttributes to inputAttributes
        }.filter { (formAttributes, inputAttributes) ->
            formAttributes.isWordPressLoginForm() && inputAttributes.hasWordPressLoginFields()
        }.take(2)
        val formAttributes = credentialForms.singleOrNull()?.first ?: return null
        if (!formAttributes.value("method").equals(POST_METHOD, ignoreCase = true)) return null

        val action = formAttributes.value("action")?.let(StringEscapeUtils::unescapeHtml4)?.trim().orEmpty()
        val submissionUrl = if (action.isEmpty()) documentUrl else documentUrl.resolve(action) ?: return null
        return submissionUrl.withoutFragment().takeIf { it.isSafeFor(canonicalOrigin, documentUrl) }
    }

    private fun String.inputAttributes(): List<List<MatchResult>>? = INPUT_PATTERN.findAll(this).toList().map { input ->
        input.groupValues[1].htmlAttributes() ?: return null
    }

    private fun List<MatchResult>.isWordPressLoginForm(): Boolean =
        value(ID_ATTRIBUTE) == LOGIN_FORM_ID && value(NAME_ATTRIBUTE) == LOGIN_FORM_NAME

    private fun List<List<MatchResult>>.hasWordPressLoginFields(): Boolean {
        val loginInput = filter { it.isEnabledInput(LOGIN_FIELD_NAME) }.singleOrNull()
        val passwordInput = filter { it.isEnabledInput(PASSWORD_FIELD_NAME) }.singleOrNull()
        return loginInput?.hasLoginSemantics(LOGIN_FIELD_ID, TEXT_INPUT_TYPE) == true &&
            passwordInput?.hasLoginSemantics(PASSWORD_FIELD_ID, PASSWORD_INPUT_TYPE) == true
    }

    private fun List<MatchResult>.isEnabledInput(name: String): Boolean =
        !has(FORM_ATTRIBUTE) &&
            !has(DISABLED_ATTRIBUTE) &&
            value(NAME_ATTRIBUTE) == name

    private fun List<MatchResult>.hasLoginSemantics(id: String, type: String): Boolean =
        value(ID_ATTRIBUTE) == id && value(TYPE_ATTRIBUTE).equals(type, ignoreCase = true)

    private fun String.htmlAttributes(): List<MatchResult>? {
        val attributes = ATTRIBUTE_PATTERN.findAll(this).toList()
        return attributes.takeIf {
            ATTRIBUTE_PATTERN.replace(this, "").isBlank() &&
                attributes.map { match -> match.groupValues[1].lowercase() }.distinct().size == attributes.size
        }
    }

    private fun List<MatchResult>.has(name: String) = any { it.groupValues[1].equals(name, ignoreCase = true) }

    private fun List<MatchResult>.value(name: String): String? = firstOrNull {
        it.groupValues[1].equals(name, ignoreCase = true)
    }?.groups?.drop(2)?.firstNotNullOfOrNull { it?.value }

    private fun String?.isWordPressAdminDashboard(): Boolean {
        val body = this ?: return false
        val bodyClasses = BODY_CLASS_PATTERN.find(body)?.groupValues?.get(2) ?: return false
        val classNames = bodyClasses.lowercase().split(WHITESPACE_PATTERN).toSet()
        return "wp-admin" in classNames &&
            "index-php" in classNames &&
            DASHBOARD_WIDGETS_PATTERN.containsMatchIn(body)
    }

    private sealed interface LoginPreflight {
        sealed interface Success : LoginPreflight {
            val transactionUrl: HttpUrl
        }

        data class LoginForm(override val transactionUrl: HttpUrl) : Success
        data class AlreadyAuthenticated(override val transactionUrl: HttpUrl) : Success
        data class Failure(val nonce: Nonce) : LoginPreflight
    }

    companion object {
        const val INVALID_CREDENTIAL_HTML_PATTERN = "document.querySelector('form').classList.add('shake')"
        const val AUTH_HEADER_KEY = "WWW-Authenticate"
        const val BASIC_AUTH_REALM = "Basic realm"
        private val BODY_CLASS_PATTERN = Regex(
            """<body\b[^>]*\bclass\s*=\s*(['"])(.*?)\1""", RegexOption.IGNORE_CASE
        )
        private val DASHBOARD_WIDGETS_PATTERN = Regex(
            pattern = """<[^>]*\bid\s*=\s*(['"])dashboard-widgets-wrap\1[^>]*>""",
            option = RegexOption.IGNORE_CASE
        )
        private val NON_RENDERED_REGION_PATTERN = Regex(
            pattern = """<!--.*?-->|<(script|style|template|textarea|title|iframe|noembed|noframes|xmp|svg|math)""" +
                """\b[^>]*>.*?</\1\s*>""",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        private val NON_RENDERED_MARKER_PATTERN = Regex(
            pattern = """<!--|</?(?:script|style|template|textarea|title|iframe|noembed|noframes|xmp|svg|math)\b""",
            option = RegexOption.IGNORE_CASE
        )
        private val FORM_REGION_PATTERN = Regex(
            pattern = """<form\b([^>]*)>(.*?)</form\s*>""",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        private val FORM_TAG_PATTERN = Regex("""</?form\b[^>]*>""", RegexOption.IGNORE_CASE)
        private val INPUT_PATTERN = Regex(
            pattern = """<input\b([^>]*?)\s*/?>""",
            option = RegexOption.IGNORE_CASE
        )
        private val ATTRIBUTE_PATTERN = Regex(
            pattern = """(?:^|\s+)([A-Za-z_:][A-Za-z0-9_.:-]*)""" +
                """(?:\s*=\s*(?:\"([^\"]*)\"|'([^']*)'|([^\s\"'=<>`]+)))?"""
        )
        private val WHITESPACE_PATTERN = Regex("\\s+")
        private const val ID_ATTRIBUTE = "id"
        private const val NAME_ATTRIBUTE = "name"
        private const val TYPE_ATTRIBUTE = "type"
        private const val FORM_ATTRIBUTE = "form"
        private const val DISABLED_ATTRIBUTE = "disabled"
        private const val LOGIN_FORM_ID = "loginform"
        private const val LOGIN_FORM_NAME = "loginform"
        private const val LOGIN_FIELD_NAME = "log"
        private const val LOGIN_FIELD_ID = "user_login"
        private const val PASSWORD_FIELD_NAME = "pwd"
        private const val PASSWORD_FIELD_ID = "user_pass"
        private const val TEXT_INPUT_TYPE = "text"
        private const val PASSWORD_INPUT_TYPE = "password"
        private const val POST_METHOD = "post"
        private const val ADMIN_INDEX = "index.php"
        private const val LOCATION_HEADER = "Location"
        private const val HTTP_DEFAULT_PORT = 80
        private const val HTTPS_DEFAULT_PORT = 443
        private const val ADMIN_AJAX_PATH_SEGMENT = "admin-ajax.php"
        private const val REST_NONCE_QUERY = "action=rest-nonce"
    }
}
