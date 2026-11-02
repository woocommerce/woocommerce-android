package com.woocommerce.android.ui.login.storesuggestion

import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class InstallReferrerSourceTest : BaseUnitTest() {
    private val referrerDetails: ReferrerDetails = mock {
        on { installReferrer } doReturn CLIENT_REFERRER
        on { referrerClickTimestampSeconds } doReturn CLIENT_CLICK_TIMESTAMP_SECONDS
    }
    private val client: InstallReferrerClient = mock()
    private val clientFactory: InstallReferrerClientFactory = mock {
        on { create() } doReturn client
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private var readReferrerCalls = 0

    private val sut = InstallReferrerSource(
        clientFactory = clientFactory,
        appPrefsWrapper = appPrefsWrapper,
    )

    @Before
    fun setUp() {
        whenever(appPrefsWrapper.debugInstallReferrerOverride).thenReturn(DEBUG_REFERRER)
        whenever(appPrefsWrapper.debugInstallReferrerClickTimestampSeconds).thenReturn(DEBUG_CLICK_TIMESTAMP_SECONDS)
        whenever(client.installReferrer).thenReturn(referrerDetails)
        givenClientRespondsWith { onInstallReferrerSetupFinished(InstallReferrerResponse.OK) }
    }

    @Test
    fun `given OK response, when resultForResponseCode, then returns the read referrer`() {
        // WHEN
        val result = resultFor(InstallReferrerResponse.OK)

        // THEN
        assertThat(result).isEqualTo(READ_RESULT)
    }

    @Test
    fun `given OK response, when resultForResponseCode, then reads the referrer once`() {
        // WHEN
        resultFor(InstallReferrerResponse.OK)

        // THEN
        assertThat(readReferrerCalls).isEqualTo(1)
    }

    @Test
    fun `given feature not supported response, when resultForResponseCode, then returns unavailable`() {
        // WHEN
        val result = resultFor(InstallReferrerResponse.FEATURE_NOT_SUPPORTED)

        // THEN
        assertThat(result).isEqualTo(InstallReferrerResult.Unavailable)
    }

    @Test
    fun `given service unavailable response, when resultForResponseCode, then returns unavailable`() {
        // WHEN
        val result = resultFor(InstallReferrerResponse.SERVICE_UNAVAILABLE)

        // THEN
        assertThat(result).isEqualTo(InstallReferrerResult.Unavailable)
    }

    @Test
    fun `given non OK response, when resultForResponseCode, then referrer is not read`() {
        // WHEN
        NON_OK_RESPONSE_CODES.forEach { resultFor(it) }

        // THEN
        assertThat(readReferrerCalls).isZero()
    }

    @Test
    fun `given developer error response, when resultForResponseCode, then returns error`() {
        // WHEN
        val result = resultFor(InstallReferrerResponse.DEVELOPER_ERROR)

        // THEN
        assertThat(result).isEqualTo(InstallReferrerResult.Error)
    }

    @Test
    fun `given service disconnected response, when resultForResponseCode, then returns error`() {
        // WHEN
        val result = resultFor(InstallReferrerResponse.SERVICE_DISCONNECTED)

        // THEN
        assertThat(result).isEqualTo(InstallReferrerResult.Error)
    }

    @Test
    fun `given permission error response, when resultForResponseCode, then returns error`() {
        // WHEN
        val result = resultFor(InstallReferrerResponse.PERMISSION_ERROR)

        // THEN
        assertThat(result).isEqualTo(InstallReferrerResult.Error)
    }

    @Test
    fun `given unknown response, when resultForResponseCode, then returns error`() {
        // WHEN
        val result = resultFor(UNKNOWN_RESPONSE_CODE)

        // THEN
        assertThat(result).isEqualTo(InstallReferrerResult.Error)
    }

    @Test
    fun `given debug referrer override, when fetch, then returns the override`() = testBlocking {
        // WHEN
        val result = sut.fetch()

        // THEN
        assertThat(result).isEqualTo(
            InstallReferrerResult.Available(
                referrer = DEBUG_REFERRER,
                clickTimestampSeconds = DEBUG_CLICK_TIMESTAMP_SECONDS,
            )
        )
    }

    @Test
    fun `given debug referrer override, when fetch, then play install referrer client is not used`() =
        testBlocking {
            // WHEN
            sut.fetch()

            // THEN
            verifyNoInteractions(clientFactory)
        }

    @Test
    fun `given empty debug referrer override, when fetch, then play install referrer client is used`() =
        testBlocking {
            // GIVEN
            givenNoDebugOverride()

            // WHEN
            sut.fetch()

            // THEN
            verify(clientFactory).create()
        }

    @Test
    fun `given client returns referrer, when fetch, then returns the referrer details`() = testBlocking {
        // GIVEN
        givenNoDebugOverride()

        // WHEN
        val result = sut.fetch()

        // THEN
        assertThat(result).isEqualTo(
            InstallReferrerResult.Available(
                referrer = CLIENT_REFERRER,
                clickTimestampSeconds = CLIENT_CLICK_TIMESTAMP_SECONDS,
            )
        )
    }

    @Test
    fun `given client returns referrer, when fetch, then connection is ended`() = testBlocking {
        // GIVEN
        givenNoDebugOverride()

        // WHEN
        sut.fetch()

        // THEN
        verify(client).endConnection()
    }

    @Test
    fun `given reading referrer throws remote exception, when fetch, then returns error`() = testBlocking {
        // GIVEN
        givenNoDebugOverride()
        whenever(client.installReferrer).thenThrow(RemoteException())

        // WHEN
        val result = sut.fetch()

        // THEN
        assertThat(result).isEqualTo(InstallReferrerResult.Error)
    }

    @Test
    fun `given starting connection throws security exception, when fetch, then returns error`() = testBlocking {
        // GIVEN
        givenNoDebugOverride()
        doThrow(SecurityException()).whenever(client).startConnection(any())

        // WHEN
        val result = sut.fetch()

        // THEN
        assertThat(result).isEqualTo(InstallReferrerResult.Error)
    }

    @Test
    fun `given service disconnects, when fetch, then returns unavailable`() = testBlocking {
        // GIVEN
        givenNoDebugOverride()
        givenClientRespondsWith { onInstallReferrerServiceDisconnected() }

        // WHEN
        val result = sut.fetch()

        // THEN
        assertThat(result).isEqualTo(InstallReferrerResult.Unavailable)
    }

    @Test
    fun `given client never responds, when fetch times out, then returns unavailable`() = testBlocking {
        // GIVEN
        givenNoDebugOverride()
        givenClientRespondsWith { }

        // WHEN
        val result = async { sut.fetch() }
        advanceTimeBy(TIMEOUT_MILLIS + 1)
        runCurrent()

        // THEN
        assertThat(result.isCompleted).isTrue()
        assertThat(result.await()).isEqualTo(InstallReferrerResult.Unavailable)
    }

    @Test
    fun `given client never responds, when fetch times out, then connection is ended`() = testBlocking {
        // GIVEN
        givenNoDebugOverride()
        givenClientRespondsWith { }

        // WHEN
        async { sut.fetch() }
        advanceTimeBy(TIMEOUT_MILLIS + 1)
        runCurrent()

        // THEN
        verify(client).endConnection()
    }

    private fun givenNoDebugOverride() {
        whenever(appPrefsWrapper.debugInstallReferrerOverride).thenReturn("")
    }

    private fun givenClientRespondsWith(response: InstallReferrerStateListener.() -> Unit) {
        doAnswer { invocation ->
            invocation.getArgument<InstallReferrerStateListener>(0).response()
        }.whenever(client).startConnection(any())
    }

    private fun resultFor(responseCode: Int) =
        InstallReferrerSource.resultForResponseCode(responseCode) {
            readReferrerCalls++
            READ_RESULT
        }

    private companion object {
        const val DEBUG_REFERRER = "utm_campaign=store_login&site_url=https%3A%2F%2Fexample.com"
        const val DEBUG_CLICK_TIMESTAMP_SECONDS = 1_760_000_000L
        const val UNKNOWN_RESPONSE_CODE = 999
        const val TIMEOUT_MILLIS = 3_000L
        const val CLIENT_REFERRER = "utm_campaign=store_login&site_url=https%3A%2F%2Fstore.com"
        const val CLIENT_CLICK_TIMESTAMP_SECONDS = 1_770_000_000L
        val READ_RESULT = InstallReferrerResult.Available(referrer = "utm_campaign=c", clickTimestampSeconds = 1L)
        val NON_OK_RESPONSE_CODES = listOf(
            InstallReferrerResponse.FEATURE_NOT_SUPPORTED,
            InstallReferrerResponse.SERVICE_UNAVAILABLE,
            InstallReferrerResponse.DEVELOPER_ERROR,
            InstallReferrerResponse.SERVICE_DISCONNECTED,
            InstallReferrerResponse.PERMISSION_ERROR,
            UNKNOWN_RESPONSE_CODE,
        )
    }
}
