package com.woocommerce.android.ui.login.storesuggestion

import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class InstallReferrerSourceTest : BaseUnitTest() {
    private val referrerDetails: ReferrerDetails = mock {
        on { installReferrer } doReturn CLIENT_REFERRER
        on { referrerClickTimestampSeconds } doReturn CLIENT_CLICK
        on { installBeginTimestampSeconds } doReturn CLIENT_INSTALL_BEGIN
    }
    private val client: InstallReferrerClient = mock()
    private val clientFactory: InstallReferrerClientFactory = mock {
        on { create() } doReturn client
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val ioDispatcher = StandardTestDispatcher(coroutinesTestRule.testDispatcher.scheduler)

    private val sut = InstallReferrerSource(
        clientFactory = clientFactory,
        appPrefsWrapper = appPrefsWrapper,
        dispatchers = CoroutineDispatchers(
            main = coroutinesTestRule.testDispatcher,
            computation = coroutinesTestRule.testDispatcher,
            io = ioDispatcher,
        ),
    )

    @Before
    fun setUp() {
        whenever(appPrefsWrapper.debugInstallReferrerOverride).thenReturn("")
        whenever(client.installReferrer).thenReturn(referrerDetails)
        givenClientRespondsWith { onInstallReferrerSetupFinished(InstallReferrerResponse.OK) }
    }

    @Test
    fun `given each response code, when resultForResponseCode, then only OK reads the referrer`() {
        val cases = mapOf(
            InstallReferrerResponse.OK to READ_RESULT,
            InstallReferrerResponse.FEATURE_NOT_SUPPORTED to InstallReferrerResult.Unavailable,
            InstallReferrerResponse.SERVICE_UNAVAILABLE to InstallReferrerResult.Unavailable,
            InstallReferrerResponse.DEVELOPER_ERROR to InstallReferrerResult.Error,
            InstallReferrerResponse.SERVICE_DISCONNECTED to InstallReferrerResult.Error,
            InstallReferrerResponse.PERMISSION_ERROR to InstallReferrerResult.Error,
            UNKNOWN_RESPONSE_CODE to InstallReferrerResult.Error,
        )

        cases.forEach { (responseCode, expected) ->
            // WHEN
            val result = InstallReferrerSource.resultForResponseCode(responseCode) { READ_RESULT }

            // THEN
            assertThat(result).describedAs("response $responseCode").isEqualTo(expected)
        }
    }

    @Test
    fun `given debug referrer override, when fetch, then returns it without using the play client`() = testBlocking {
        // GIVEN
        whenever(appPrefsWrapper.debugInstallReferrerOverride).thenReturn(DEBUG_REFERRER)
        whenever(appPrefsWrapper.debugInstallReferrerClickTimestampSeconds).thenReturn(DEBUG_CLICK)

        // WHEN
        val result = sut.fetch()

        // THEN
        assertThat(result).isEqualTo(InstallReferrerResult.Available(DEBUG_REFERRER, DEBUG_CLICK, DEBUG_CLICK))
        verifyNoInteractions(clientFactory)
    }

    @Test
    fun `given client returns referrer, when fetch, then reads it on io and ends the connection`() = testBlocking {
        // WHEN
        val pending = async { sut.fetch() }
        runCurrent()

        // THEN
        assertThat(pending.await()).isEqualTo(
            InstallReferrerResult.Available(CLIENT_REFERRER, CLIENT_CLICK, CLIENT_INSTALL_BEGIN)
        )
        verify(client).endConnection()
    }

    @Test
    fun `given OK response, when io has not run yet, then referrer is not read`() = testBlocking {
        // WHEN
        async { sut.fetch() }

        // THEN
        verify(client, never()).installReferrer
    }

    @Test
    fun `given client throws, when fetch, then returns error`() = testBlocking {
        val failures = listOf<() -> Unit>(
            { whenever(client.installReferrer).thenThrow(RemoteException()) },
            { whenever(client.installReferrer).thenThrow(IllegalStateException()) },
            { doThrow(SecurityException()).whenever(client).startConnection(any()) },
            { doThrow(IllegalStateException()).whenever(client).startConnection(any()) },
            { doThrow(IllegalArgumentException()).whenever(client).startConnection(any()) },
        )

        failures.forEachIndexed { index, givenFailure ->
            // GIVEN
            reset(client)
            givenClientRespondsWith { onInstallReferrerSetupFinished(InstallReferrerResponse.OK) }
            givenFailure()

            // WHEN
            val result = fetchAndRunIo()

            // THEN
            assertThat(result).describedAs("failure #$index").isEqualTo(InstallReferrerResult.Error)
        }
    }

    @Test
    fun `given service disconnects before reading, when fetch, then returns unavailable without reading`() =
        testBlocking {
            // GIVEN
            givenClientRespondsWith {
                onInstallReferrerSetupFinished(InstallReferrerResponse.OK)
                onInstallReferrerServiceDisconnected()
            }

            // WHEN
            val result = fetchAndRunIo()

            // THEN
            assertThat(result).isEqualTo(InstallReferrerResult.Unavailable)
            verify(client, never()).installReferrer
            verify(client).endConnection()
        }

    @Test
    fun `given service disconnects after the result, when fetch, then the first result wins`() = testBlocking {
        // WHEN
        val result = fetchAndRunIo()
        val listener = argumentCaptor<InstallReferrerStateListener>()
        verify(client).startConnection(listener.capture())
        listener.firstValue.onInstallReferrerServiceDisconnected()
        runCurrent()

        // THEN
        assertThat(result).isEqualTo(
            InstallReferrerResult.Available(CLIENT_REFERRER, CLIENT_CLICK, CLIENT_INSTALL_BEGIN)
        )
        verify(client).endConnection()
    }

    @Test
    fun `given client never responds, when fetch times out, then returns unavailable and ends the connection`() =
        testBlocking {
            // GIVEN
            givenClientRespondsWith { }

            // WHEN
            val pending = async { sut.fetch() }
            advanceTimeBy(TIMEOUT_MILLIS + 1)
            runCurrent()

            // THEN
            assertThat(pending.await()).isEqualTo(InstallReferrerResult.Unavailable)
            verify(client).endConnection()
        }

    private fun givenClientRespondsWith(response: InstallReferrerStateListener.() -> Unit) {
        doAnswer { invocation ->
            invocation.getArgument<InstallReferrerStateListener>(0).response()
        }.whenever(client).startConnection(any())
    }

    private suspend fun TestScope.fetchAndRunIo(): InstallReferrerResult {
        val result = async { sut.fetch() }
        runCurrent()
        return result.await()
    }

    private companion object {
        const val DEBUG_REFERRER = "utm_campaign=deeplinks_promote_app&site_host=example.com"
        const val DEBUG_CLICK = 1_760_000_000L
        const val UNKNOWN_RESPONSE_CODE = 999
        const val TIMEOUT_MILLIS = 3_000L
        const val CLIENT_REFERRER = "utm_campaign=deeplinks_promote_app&site_host=store.com"
        const val CLIENT_CLICK = 1_770_000_000L
        const val CLIENT_INSTALL_BEGIN = 1_770_000_060L
        val READ_RESULT = InstallReferrerResult.Available("utm_campaign=c", 1L, 2L)
    }
}
