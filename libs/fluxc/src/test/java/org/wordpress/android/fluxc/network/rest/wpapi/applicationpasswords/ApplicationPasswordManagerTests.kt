package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UnexpectedStoreResponse
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseKind
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import java.util.Date
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ApplicationPasswordManagerTests {
    private val applicationName = "name"
    private val uuid = "uuid"
    private val testSite = SiteModel().apply {
        username = "username"
        password = "site-password"
        url = "http://test-site.com"
    }
    private val testCredentials = ApplicationPasswordCredentials(
        userName = "username",
        password = "password",
        uuid = "uuid"
    )
    private val applicationPasswordsStore: ApplicationPasswordsStore = mock()
    private val listener: ApplicationPasswordsListener = mock()
    private val mJetpackApplicationPasswordsRestClient: JetpackApplicationPasswordsRestClient = mock()
    private val mWpApiApplicationPasswordsRestClient: WPApiApplicationPasswordsRestClient = mock()

    private val applicationPasswordsConfiguration = object : ApplicationPasswordsConfiguration {
        override val applicationName: String = this@ApplicationPasswordManagerTests.applicationName
        override suspend fun isEnabledForJetpackAccess() = true
    }

    private var now = 0L
    private val currentTimeProvider: CurrentTimeProvider = mock {
        on { currentDate() } doAnswer { Date(now) }
    }

    private lateinit var mApplicationPasswordsManager: ApplicationPasswordsManager

    @Before
    fun setup() {
        mApplicationPasswordsManager = ApplicationPasswordsManager(
            applicationPasswordsStore = applicationPasswordsStore,
            jetpackApplicationPasswordsRestClient = mJetpackApplicationPasswordsRestClient,
            wpApiApplicationPasswordsRestClient = mWpApiApplicationPasswordsRestClient,
            configuration = applicationPasswordsConfiguration,
            currentTimeProvider = currentTimeProvider,
            appLogWrapper = mock(),
            listener = Optional.of(listener)
        )
    }

    @Test
    fun `given a local password exists, when we ask for a password, then return it`() = runTest {
        whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(testCredentials)
        val result = mApplicationPasswordsManager.getApplicationCredentials(
            testSite,
            REQUEST_PATH
        )

        assertEquals(ApplicationPasswordCreationResult.Existing(testCredentials), result)
    }

    @Test
    fun `given no local password is saved, when we ask for a password for a jetpack site, then create it`() =
        runTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_WPCOM_REST
            }

            whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(null)
            whenever(mJetpackApplicationPasswordsRestClient.fetchWPAdminUsername(site))
                .thenReturn(UsernameFetchPayload(testCredentials.userName))
            whenever(
                mJetpackApplicationPasswordsRestClient.createApplicationPassword(
                    site,
                    applicationName
                )
            )
                .thenReturn(
                    ApplicationPasswordCreationPayload(
                        testCredentials.password,
                        testCredentials.uuid!!
                    )
                )

            val result = mApplicationPasswordsManager.getApplicationCredentials(
                testSite,
                REQUEST_PATH
            )

            assertEquals(ApplicationPasswordCreationResult.Created(testCredentials), result)
            verify(applicationPasswordsStore).saveCredentials(testSite, testCredentials)
        }

    @Test
    fun `given no local password is saved, when we ask for a password for a non-jetpack site, then create it`() =
        runTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_XMLRPC
                username = testCredentials.userName
                password = "password"
            }

            whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(null)
            whenever(
                mWpApiApplicationPasswordsRestClient.createApplicationPassword(
                    site,
                    applicationName
                )
            )
                .thenReturn(
                    ApplicationPasswordCreationPayload(
                        testCredentials.password,
                        testCredentials.uuid!!
                    )
                )

            val result = mApplicationPasswordsManager.getApplicationCredentials(
                testSite,
                REQUEST_PATH
            )

            assertEquals(ApplicationPasswordCreationResult.Created(testCredentials), result)
        }

    @Test
    fun `when a jetpack site returns 404, then return feature not available`() =
        runTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_WPCOM_REST
            }
            val networkError = BaseNetworkError(VolleyError(NetworkResponse(404, null, true, 0, emptyList())))

            whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(null)
            whenever(mJetpackApplicationPasswordsRestClient.fetchWPAdminUsername(site))
                .thenReturn(UsernameFetchPayload(testCredentials.userName))
            whenever(mJetpackApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = mApplicationPasswordsManager.getApplicationCredentials(
                testSite,
                REQUEST_PATH
            )

            assertEquals(ApplicationPasswordCreationResult.NotSupported(networkError), result)
        }

    @Test
    fun `given a simple wpcom site, when requesting credentials, then return unsupported and notify`() = runTest {
        val site = SiteModel().apply { setIsWPCom(true) }

        val result = mApplicationPasswordsManager.getApplicationCredentials(site, REQUEST_PATH)

        assertIs<ApplicationPasswordCreationResult.NotSupported>(result)
        verify(listener).onFeatureUnavailable(
            eq(site),
            any(),
            eq(REQUEST_PATH),
            eq(ApplicationPasswordGenerationScenario.GENERATION)
        )
    }

    @Test
    fun `when a jetpack site returns application_passwords_disabled, then return feature not available`() =
        runTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_WPCOM_REST
            }
            val networkError = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.SERVER_ERROR)).apply {
                apiError = "application_passwords_disabled"
            }

            whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(null)
            whenever(mJetpackApplicationPasswordsRestClient.fetchWPAdminUsername(site))
                .thenReturn(UsernameFetchPayload(testCredentials.userName))
            whenever(mJetpackApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = mApplicationPasswordsManager.getApplicationCredentials(
                testSite,
                REQUEST_PATH
            )

            assertEquals(ApplicationPasswordCreationResult.NotSupported(networkError), result)
        }

    @Test
    fun `when a jetpack site returns application_passwords_disabled_for_user, then return feature not available`() =
        runTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_WPCOM_REST
            }
            val networkError = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.SERVER_ERROR)).apply {
                apiError = "application_passwords_disabled_for_user"
            }

            whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(null)
            whenever(mJetpackApplicationPasswordsRestClient.fetchWPAdminUsername(site))
                .thenReturn(UsernameFetchPayload(testCredentials.userName))
            whenever(mJetpackApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = mApplicationPasswordsManager.getApplicationCredentials(
                testSite,
                REQUEST_PATH
            )

            assertEquals(ApplicationPasswordCreationResult.NotSupported(networkError), result)
        }

    @Test
    fun `when a non-jetpack site returns 404, then return feature not available`() =
        runTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_XMLRPC
                username = testCredentials.userName
                password = "password"
            }
            val networkError = BaseNetworkError(VolleyError(NetworkResponse(404, null, true, 0, emptyList())))

            whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(null)
            whenever(mWpApiApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = mApplicationPasswordsManager.getApplicationCredentials(
                testSite,
                REQUEST_PATH
            )

            Assert.assertEquals(ApplicationPasswordCreationResult.NotSupported(networkError), result)
        }

    @Test
    fun `when a non-jetpack site returns a 404 page, then return failure`() =
        runTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_XMLRPC
                username = testCredentials.userName
                password = "password"
            }
            val networkError = BaseNetworkError(VolleyError(NetworkResponse(404, null, true, 0, emptyList()))).apply {
                unexpectedStoreResponse = UnexpectedStoreResponse(
                    kind = UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE,
                    statusCode = 404,
                    contentType = "text/html",
                    requestType = "POST /wp-json/wp/v2/users/me/application-passwords",
                    excerpt = null
                )
            }

            whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(null)
            whenever(mWpApiApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = mApplicationPasswordsManager.getApplicationCredentials(
                testSite,
                REQUEST_PATH
            )

            assertEquals(ApplicationPasswordCreationResult.Failure(networkError), result)
        }

    @Test
    fun `when a non-jetpack site returns application_passwords_disabled, then return feature not available`() =
        runTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_XMLRPC
                username = testCredentials.userName
                password = "password"
            }
            val networkError = WPComGsonNetworkError(BaseNetworkError(GenericErrorType.SERVER_ERROR)).apply {
                apiError = "application_passwords_disabled"
            }

            whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(null)
            whenever(mWpApiApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(networkError))

            val result = mApplicationPasswordsManager.getApplicationCredentials(
                testSite,
                REQUEST_PATH
            )

            assertEquals(ApplicationPasswordCreationResult.NotSupported(networkError), result)
        }

    @Test
    fun `given a duplicate password already exists, when creating a new password, then delete the previous one`() =
        runTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_XMLRPC
                username = testCredentials.userName
                password = "password"
            }
            val creationNetworkError = BaseNetworkError(VolleyError(NetworkResponse(409, null, true, 0, emptyList())))
            whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(null)
            whenever(mWpApiApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
                .thenReturn(ApplicationPasswordCreationPayload(creationNetworkError))
                .thenReturn(ApplicationPasswordCreationPayload(testCredentials.password, testCredentials.uuid!!))
            whenever(mWpApiApplicationPasswordsRestClient.fetchApplicationPasswordUUID(site, applicationName))
                .thenReturn(ApplicationPasswordUUIDFetchPayload(uuid))
            whenever(mWpApiApplicationPasswordsRestClient.deleteApplicationPassword(site, uuid))
                .thenReturn(ApplicationPasswordDeletionPayload(isDeleted = true))

            val result = mApplicationPasswordsManager.getApplicationCredentials(site, REQUEST_PATH)

            assertEquals(ApplicationPasswordCreationResult.Created(testCredentials), result)
            verify(mWpApiApplicationPasswordsRestClient).fetchApplicationPasswordUUID(site, applicationName)
            verify(mWpApiApplicationPasswordsRestClient).deleteApplicationPassword(site, uuid)
        }

    @Test
    fun `given application password exists locally, when deleting a password, then delete it using it itself`() =
        runTest {
            val site = testSite.apply {
                origin = SiteModel.ORIGIN_XMLRPC
                username = testCredentials.userName
            }
            whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(testCredentials)
            whenever(mWpApiApplicationPasswordsRestClient.deleteApplicationPassword(site, testCredentials))
                .thenReturn(ApplicationPasswordDeletionPayload(isDeleted = true))

            val result = mApplicationPasswordsManager.deleteApplicationCredentials(site)

            assertEquals(ApplicationPasswordDeletionResult.Success, result)
            verify(mWpApiApplicationPasswordsRestClient).deleteApplicationPassword(site, testCredentials)
        }

    @Test
    fun `given the password is still accepted by the site, when deciding whether to regenerate, then don't`() =
        runTest {
            val site = webFlowSite()
            whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(site, testCredentials))
                .thenReturn(ApplicationPasswordValidity.VALID)

            val result = mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials)

            assertFalse(result)
            verify(mWpApiApplicationPasswordsRestClient).checkApplicationPasswordValidity(site, testCredentials)
        }

    @Test
    fun `given the password is rejected by the site, when deciding whether to regenerate, then do`() = runTest {
        val site = webFlowSite()
        whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(site, testCredentials))
            .thenReturn(ApplicationPasswordValidity.INVALID)

        val result = mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials)

        assertTrue(result)
    }

    @Test
    fun `given the check is inconclusive for a web flow site, when deciding whether to regenerate, then don't`() =
        runTest {
            val site = webFlowSite()
            whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(site, testCredentials))
                .thenReturn(ApplicationPasswordValidity.UNKNOWN)

            val result = mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials)

            assertFalse(result)
        }

    @Test
    fun `given the check is inconclusive for a site with credentials, when deciding whether to regenerate, then do`() =
        runTest {
            val site = nativeCredentialsSite()
            whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(site, testCredentials))
                .thenReturn(ApplicationPasswordValidity.UNKNOWN)

            val result = mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials)

            assertTrue(result)
        }

    @Test
    fun `given the check is inconclusive for a jetpack site, when deciding whether to regenerate, then do`() = runTest {
        val site = SiteModel().apply {
            origin = SiteModel.ORIGIN_WPCOM_REST
            url = "http://test-site.com"
        }
        whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(site, testCredentials))
            .thenReturn(ApplicationPasswordValidity.UNKNOWN)

        val result = mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials)

        assertTrue(result)
    }

    @Test
    fun `given a web flow site without a local password, when we ask for a password, then fail with 401`() =
        runTest {
            val site = webFlowSite()
            whenever(applicationPasswordsStore.getCredentials(site)).thenReturn(null)

            val result = mApplicationPasswordsManager.getApplicationCredentials(site, REQUEST_PATH)

            val failure = assertIs<ApplicationPasswordCreationResult.Failure>(result)
            assertEquals(GenericErrorType.NOT_AUTHENTICATED, failure.error.type)
            assertEquals(401, failure.error.volleyError?.networkResponse?.statusCode)
        }

    @Test
    fun `given a recent failure, when requesting credentials within cooldown, then skip the API call`() = runTest {
        val error = serverError()
        givenCreationFailure(testSite, error)

        assertIs<ApplicationPasswordCreationResult.Failure>(
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        now = INITIAL_COOLDOWN_MS - 1

        val result = mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)

        assertEquals(ApplicationPasswordCreationResult.Failure(error), result)
        verify(mWpApiApplicationPasswordsRestClient, times(1))
            .createApplicationPassword(testSite, applicationName)
        verify(listener).onPasswordGenerationFailed(
            any(),
            eq(REQUEST_PATH),
            eq(ApplicationPasswordGenerationScenario.GENERATION)
        )
    }

    @Test
    fun `given repeated failures, when each cooldown expires, then double up to one hour`() = runTest {
        val error = serverError()
        givenCreationFailure(testSite, error)
        val cooldowns = listOf(
            10_000L,
            20_000L,
            40_000L,
            80_000L,
            160_000L,
            320_000L,
            640_000L,
            1_280_000L,
            2_560_000L,
            5_120_000L,
            10_240_000L,
            20_480_000L
        )

        cooldowns.forEach { cooldown ->
            assertIs<ApplicationPasswordCreationResult.Failure>(
                mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
            )
            now += cooldown
        }
        assertIs<ApplicationPasswordCreationResult.Failure>(
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        now += MAX_COOLDOWN_MS - 1

        assertEquals(
            ApplicationPasswordCreationResult.Failure(error),
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        now += 1

        assertIs<ApplicationPasswordCreationResult.Failure>(
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        now += MAX_COOLDOWN_MS - 1
        assertEquals(
            ApplicationPasswordCreationResult.Failure(error),
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )

        verify(mWpApiApplicationPasswordsRestClient, times(14))
            .createApplicationPassword(testSite, applicationName)
        verify(listener, times(14)).onPasswordGenerationFailed(
            any(),
            eq(REQUEST_PATH),
            eq(ApplicationPasswordGenerationScenario.GENERATION)
        )
    }

    @Test
    fun `given connection failures, when retries fail, then use ten seconds and double the next cooldown`() = runTest {
        val connectionError = BaseNetworkError(GenericErrorType.NETWORK_ERROR, "offline")
        val serverError = serverError()
        whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(null)
        whenever(mWpApiApplicationPasswordsRestClient.createApplicationPassword(testSite, applicationName))
            .thenReturn(
                ApplicationPasswordCreationPayload(connectionError),
                ApplicationPasswordCreationPayload(connectionError),
                ApplicationPasswordCreationPayload(serverError),
                ApplicationPasswordCreationPayload(serverError)
            )

        assertIs<ApplicationPasswordCreationResult.Failure>(
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        now = INITIAL_COOLDOWN_MS - 1
        assertEquals(
            ApplicationPasswordCreationResult.Failure(connectionError),
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        now += 1
        assertIs<ApplicationPasswordCreationResult.Failure>(
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        now += INITIAL_COOLDOWN_MS - 1
        assertEquals(
            ApplicationPasswordCreationResult.Failure(connectionError),
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        now += 1
        assertIs<ApplicationPasswordCreationResult.Failure>(
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        now += INITIAL_COOLDOWN_MS * 2 - 1
        assertEquals(
            ApplicationPasswordCreationResult.Failure(serverError),
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        now += 1
        assertIs<ApplicationPasswordCreationResult.Failure>(
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )

        verify(mWpApiApplicationPasswordsRestClient, times(4))
            .createApplicationPassword(testSite, applicationName)
        verify(listener, times(4)).onPasswordGenerationFailed(
            any(),
            eq(REQUEST_PATH),
            eq(ApplicationPasswordGenerationScenario.GENERATION)
        )
    }

    @Test
    fun `given an unsupported response, when requesting credentials again, then cooldown and notify once`() = runTest {
        val site = nativeCredentialsSite()
        val error = BaseNetworkError(VolleyError(NetworkResponse(404, null, true, 0, emptyList())))
        whenever(applicationPasswordsStore.getCredentials(site)).thenReturn(null)
        whenever(mWpApiApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
            .thenReturn(ApplicationPasswordCreationPayload(error))

        assertIs<ApplicationPasswordCreationResult.NotSupported>(
            mApplicationPasswordsManager.getApplicationCredentials(
                site,
                REQUEST_PATH,
                scenario = ApplicationPasswordGenerationScenario.REGENERATION
            )
        )
        val result = mApplicationPasswordsManager.getApplicationCredentials(
            site,
            REQUEST_PATH,
            scenario = ApplicationPasswordGenerationScenario.REGENERATION
        )

        assertEquals(ApplicationPasswordCreationResult.Failure(error), result)
        verify(mWpApiApplicationPasswordsRestClient, times(1)).createApplicationPassword(site, applicationName)
        verify(listener).onFeatureUnavailable(
            eq(site),
            any(),
            eq(REQUEST_PATH),
            eq(ApplicationPasswordGenerationScenario.REGENERATION)
        )
    }

    @Test
    fun `given username fetch fails, when requesting credentials again, then cooldown without creating`() = runTest {
        val site = webFlowSite().apply { username = null }
        whenever(applicationPasswordsStore.getCredentials(site)).thenReturn(null)

        val failure = assertIs<ApplicationPasswordCreationResult.Failure>(
            mApplicationPasswordsManager.getApplicationCredentials(site, REQUEST_PATH)
        )
        assertEquals(
            failure,
            mApplicationPasswordsManager.getApplicationCredentials(site, REQUEST_PATH)
        )

        verify(mWpApiApplicationPasswordsRestClient, never()).createApplicationPassword(site, applicationName)
        verify(listener).onPasswordGenerationFailed(
            any(),
            eq(REQUEST_PATH),
            eq(ApplicationPasswordGenerationScenario.GENERATION)
        )
    }

    @Test
    fun `given a backwards clock step, when requesting credentials, then retry immediately`() = runTest {
        givenCreationFailure(testSite, serverError())
        now = INITIAL_COOLDOWN_MS
        mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        now = 5_000L

        val result = mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)

        assertIs<ApplicationPasswordCreationResult.Failure>(result)
        verify(mWpApiApplicationPasswordsRestClient, times(2))
            .createApplicationPassword(testSite, applicationName)
    }

    @Test
    fun `given a password is saved during cooldown, when requesting credentials, then return it`() = runTest {
        givenCreationFailure(testSite, serverError())
        whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(null, testCredentials)
        mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)

        val result = mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)

        assertEquals(ApplicationPasswordCreationResult.Existing(testCredentials), result)
        verify(mWpApiApplicationPasswordsRestClient, times(1))
            .createApplicationPassword(testSite, applicationName)
    }

    @Test
    fun `given one site is cooling down, when another site requests credentials, then attempt creation`() = runTest {
        val otherSite = nativeCredentialsSite().apply { id = testSite.id + 1 }
        val error = serverError()
        whenever(applicationPasswordsStore.getCredentials(any())).thenReturn(null)
        whenever(mWpApiApplicationPasswordsRestClient.createApplicationPassword(any(), eq(applicationName)))
            .thenReturn(ApplicationPasswordCreationPayload(error))
        mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)

        val result = mApplicationPasswordsManager.getApplicationCredentials(otherSite, REQUEST_PATH)

        assertIs<ApplicationPasswordCreationResult.Failure>(result)
        verify(mWpApiApplicationPasswordsRestClient, times(2)).createApplicationPassword(any(), eq(applicationName))
    }

    @Test
    fun `given creation succeeds after cooldown, when it fails again, then start a fresh cooldown`() = runTest {
        val error = serverError()
        whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(null)
        whenever(mWpApiApplicationPasswordsRestClient.createApplicationPassword(testSite, applicationName))
            .thenReturn(
                ApplicationPasswordCreationPayload(error),
                ApplicationPasswordCreationPayload(testCredentials.password, testCredentials.uuid!!),
                ApplicationPasswordCreationPayload(error),
                ApplicationPasswordCreationPayload(error)
            )
        assertIs<ApplicationPasswordCreationResult.Failure>(
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        now = INITIAL_COOLDOWN_MS
        assertIs<ApplicationPasswordCreationResult.Created>(
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )

        assertIs<ApplicationPasswordCreationResult.Failure>(
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        now += INITIAL_COOLDOWN_MS - 1
        assertEquals(
            ApplicationPasswordCreationResult.Failure(error),
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        now += 1

        assertIs<ApplicationPasswordCreationResult.Failure>(
            mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)
        )
        verify(mWpApiApplicationPasswordsRestClient, times(4))
            .createApplicationPassword(testSite, applicationName)
        verify(listener, times(3)).onPasswordGenerationFailed(
            any(),
            eq(REQUEST_PATH),
            eq(ApplicationPasswordGenerationScenario.GENERATION)
        )
        verify(listener).onNewPasswordCreated(ApplicationPasswordGenerationScenario.GENERATION)
    }

    @Test
    fun `given an active cooldown, when login ensures a password, then attempt creation and report login path`() = runTest {
        givenCreationFailure(testSite, serverError())
        mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH)

        val result = mApplicationPasswordsManager.ensureApplicationPassword(testSite)

        assertIs<ApplicationPasswordCreationResult.Failure>(result)
        verify(mWpApiApplicationPasswordsRestClient, times(2))
            .createApplicationPassword(testSite, applicationName)
        verify(listener).onPasswordGenerationFailed(
            any(),
            eq("login"),
            eq(ApplicationPasswordGenerationScenario.GENERATION)
        )
    }

    @Test
    fun `given requests queued behind a failing attempt, when the first completes, then skip the second`() = runTest {
        val error = serverError()
        whenever(applicationPasswordsStore.getCredentials(testSite)).thenReturn(null)
        val attemptStarted = CompletableDeferred<Unit>()
        val attemptCanFinish = CompletableDeferred<Unit>()
        whenever(mWpApiApplicationPasswordsRestClient.createApplicationPassword(testSite, applicationName))
            .doSuspendableAnswer {
                attemptStarted.complete(Unit)
                attemptCanFinish.await()
                ApplicationPasswordCreationPayload(error)
            }
        val first = async { mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH) }
        attemptStarted.await()
        val second = async { mApplicationPasswordsManager.getApplicationCredentials(testSite, REQUEST_PATH) }
        runCurrent()
        assertTrue(second.isActive)

        attemptCanFinish.complete(Unit)

        assertIs<ApplicationPasswordCreationResult.Failure>(first.await())
        assertEquals(ApplicationPasswordCreationResult.Failure(error), second.await())
        verify(mWpApiApplicationPasswordsRestClient, times(1))
            .createApplicationPassword(testSite, applicationName)
        verify(listener, times(1)).onPasswordGenerationFailed(
            any(),
            eq(REQUEST_PATH),
            eq(ApplicationPasswordGenerationScenario.GENERATION)
        )
    }

    @Test
    fun `given parallel checks for the same password, when deciding whether to regenerate, then probe once`() =
        runTest {
            val site = webFlowSite()
            val probeStarted = CompletableDeferred<Unit>()
            val probeCanFinish = CompletableDeferred<Unit>()
            whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(site, testCredentials))
                .doSuspendableAnswer {
                    probeStarted.complete(Unit)
                    probeCanFinish.await()
                    ApplicationPasswordValidity.VALID
                }

            val first =
                async { mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials) }
            probeStarted.await()
            val second =
                async { mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials) }
            runCurrent()

            // The second caller is parked on the mutex, not served from a memo that isn't populated yet
            assertTrue(second.isActive)
            verify(mWpApiApplicationPasswordsRestClient, times(1))
                .checkApplicationPasswordValidity(site, testCredentials)

            probeCanFinish.complete(Unit)

            assertFalse(first.await())
            assertFalse(second.await())
            verify(mWpApiApplicationPasswordsRestClient, times(1))
                .checkApplicationPasswordValidity(site, testCredentials)
        }

    @Test
    fun `given the password was replaced, when deciding whether to regenerate, then probe again`() = runTest {
        val site = webFlowSite()
        val replacement = testCredentials.copy(password = "a-new-password")
        whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(any(), any()))
            .thenReturn(ApplicationPasswordValidity.VALID)

        mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials)
        mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, replacement)

        verify(mWpApiApplicationPasswordsRestClient).checkApplicationPasswordValidity(site, testCredentials)
        verify(mWpApiApplicationPasswordsRestClient).checkApplicationPasswordValidity(site, replacement)
    }

    @Test
    fun `given the cached result expired, when deciding whether to regenerate, then probe again`() = runTest {
        val site = webFlowSite()
        whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(site, testCredentials))
            .thenReturn(ApplicationPasswordValidity.VALID)

        mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials)
        now += 11_000L
        mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials)

        verify(mWpApiApplicationPasswordsRestClient, times(2))
            .checkApplicationPasswordValidity(site, testCredentials)
    }

    @Test
    fun `given a fresh verdict, when another 401 arrives within the window, then reuse it`() = runTest {
        val site = webFlowSite()
        whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(site, testCredentials))
            .thenReturn(ApplicationPasswordValidity.VALID)

        assertFalse(mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials))
        now += VALIDATION_TTL_MS - 1
        assertFalse(mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials))

        verify(mWpApiApplicationPasswordsRestClient, times(1))
            .checkApplicationPasswordValidity(site, testCredentials)
    }

    @Test
    fun `given a rejected password, when another 401 arrives within the window, then reuse that verdict too`() =
        runTest {
            val site = webFlowSite()
            whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(site, testCredentials))
                .thenReturn(ApplicationPasswordValidity.INVALID)

            assertTrue(mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials))
            assertTrue(mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials))

            verify(mWpApiApplicationPasswordsRestClient, times(1))
                .checkApplicationPasswordValidity(site, testCredentials)
        }

    @Test
    fun `given the check took the whole window, when another 401 arrives, then still reuse the verdict`() =
        runTest {
            val site = webFlowSite()
            whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(site, testCredentials))
                .doSuspendableAnswer {
                    now += VALIDATION_TTL_MS
                    ApplicationPasswordValidity.VALID
                }

            mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials)
            mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials)

            verify(mWpApiApplicationPasswordsRestClient, times(1))
                .checkApplicationPasswordValidity(site, testCredentials)
        }

    @Test
    fun `given another site hit 401, when deciding whether to regenerate, then probe that site as well`() = runTest {
        val siteA = webFlowSite().apply { id = 1 }
        val siteB = webFlowSite().apply { id = 2 }
        whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(siteA, testCredentials))
            .thenReturn(ApplicationPasswordValidity.VALID)
        whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(siteB, testCredentials))
            .thenReturn(ApplicationPasswordValidity.INVALID)

        assertFalse(mApplicationPasswordsManager.shouldRegenerateApplicationPassword(siteA, testCredentials))
        assertTrue(mApplicationPasswordsManager.shouldRegenerateApplicationPassword(siteB, testCredentials))

        verify(mWpApiApplicationPasswordsRestClient).checkApplicationPasswordValidity(siteA, testCredentials)
        verify(mWpApiApplicationPasswordsRestClient).checkApplicationPasswordValidity(siteB, testCredentials)
    }

    @Test
    fun `given the clock stepped backwards, when deciding whether to regenerate, then probe again`() = runTest {
        val site = webFlowSite()
        whenever(mWpApiApplicationPasswordsRestClient.checkApplicationPasswordValidity(site, testCredentials))
            .thenReturn(ApplicationPasswordValidity.VALID)

        mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials)
        now -= 60_000L
        mApplicationPasswordsManager.shouldRegenerateApplicationPassword(site, testCredentials)

        verify(mWpApiApplicationPasswordsRestClient, times(2))
            .checkApplicationPasswordValidity(site, testCredentials)
    }

    private fun webFlowSite() = SiteModel().apply {
        origin = SiteModel.ORIGIN_WPAPI
        url = "http://test-site.com"
        username = testCredentials.userName
    }

    private fun nativeCredentialsSite() = webFlowSite().apply {
        password = "site-password"
    }

    private fun serverError() = BaseNetworkError(GenericErrorType.SERVER_ERROR, "server failure")

    private suspend fun givenCreationFailure(site: SiteModel, error: BaseNetworkError) {
        whenever(applicationPasswordsStore.getCredentials(site)).thenReturn(null)
        whenever(mWpApiApplicationPasswordsRestClient.createApplicationPassword(site, applicationName))
            .thenReturn(ApplicationPasswordCreationPayload(error))
    }

    companion object {
        private const val REQUEST_PATH = "products"
        private const val VALIDATION_TTL_MS = 10_000L
        private const val INITIAL_COOLDOWN_MS = 10_000L
        private const val MAX_COOLDOWN_MS = 60 * 60 * 1_000L
    }
}
