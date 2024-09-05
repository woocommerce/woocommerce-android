package com.woocommerce.android.ui.jitm

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse

@OptIn(ExperimentalCoroutinesApi::class)
class JitmStoreInMemoryCacheTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(mock())
    }
    private var pathsProvider: JitmMessagePathsProvider = mock()
    private val jitmStore: JitmStoreWrapper = mock()
    private val jitmQueryParamsEncoder: JitmQueryParamsEncoder = mock {
        on { getEncodedQueryParams() }.thenReturn("")
    }
    private val jitmTracker: JitmTracker = mock()

    private val cache = JitmStoreInMemoryCache(
        selectedSite,
        pathsProvider,
        jitmStore,
        jitmQueryParamsEncoder,
        jitmTracker,
        CoroutineScope(UnconfinedTestDispatcher()),
    )

    @Test
    fun `given valid site, when init, then should fetch messages`() = testBlocking {
        // GIVEN
        val messagePath = "path:screen:1"
        val jitmResponseArray = arrayOf(mock<JITMApiResponse>())
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(pathsProvider.paths).thenReturn(listOf(messagePath))
        whenever(jitmStore.fetchJitmMessage(any(), any(), any())).thenReturn(WooResult(jitmResponseArray))

        // WHEN
        cache.init()

        // THEN
        verify(jitmStore).fetchJitmMessage(eq(selectedSite.get()), eq(messagePath), any())
    }

    @Test
    fun `given site doesnt exist, when init, then should not fetch messages`() = testBlocking {
        // GIVEN
        whenever(selectedSite.exists()).thenReturn(false)

        // WHEN
        cache.init()

        // THEN
        verify(jitmStore, never()).fetchJitmMessage(any(), any(), any())
    }

    @Test
    fun `given site exists and initstatus is started, when init, then should not fetch messages`() = testBlocking {
        // GIVEN
        val messagePath = "path:screen:1"
        val jitmResponseArray = arrayOf(mock<JITMApiResponse>())
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(pathsProvider.paths).thenReturn(listOf(messagePath))
        whenever(jitmStore.fetchJitmMessage(any(), any(), any())).thenReturn(WooResult(jitmResponseArray))

        // WHEN
        cache.init()
        cache.init()

        // THEN
        verify(jitmStore, times(1)).fetchJitmMessage(any(), any(), any())
    }

    @Test
    fun `given 2 paths, when init, then should fetch messages for every path`() = testBlocking {
        // GIVEN
        val messagePath1 = "path:screen:1"
        val messagePath2 = "path:screen:2"
        val jitmResponseArray = arrayOf(mock<JITMApiResponse>())
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(pathsProvider.paths).thenReturn(listOf(messagePath1, messagePath2))
        whenever(jitmStore.fetchJitmMessage(any(), any(), any())).thenReturn(WooResult(jitmResponseArray))

        // WHEN
        cache.init()

        // THEN
        verify(jitmStore).fetchJitmMessage(eq(selectedSite.get()), eq(messagePath1), any())
        verify(jitmStore).fetchJitmMessage(eq(selectedSite.get()), eq(messagePath2), any())
    }

    @Test
    fun `given error response from fetching, when init, then track jitm fetch failure tracked`() = testBlocking {
        // GIVEN
        val messagePath = "path:screen:1"
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(pathsProvider.paths).thenReturn(listOf(messagePath))
        val type = WooErrorType.API_ERROR
        val error = WooError(type, BaseRequest.GenericErrorType.NETWORK_ERROR)
        whenever(jitmStore.fetchJitmMessage(any(), any(), any())).thenReturn(WooResult(error))

        // WHEN
        cache.init()

        // THEN
        verify(jitmTracker).trackJitmFetchFailure("screen", type, null)
    }

    @Test
    fun `given success response from fetching, when init, then track jitm fetch success tracked`() = testBlocking {
        // GIVEN
        val messagePath = "path:screen:1"
        val jitmApiResponse = mock<JITMApiResponse> {
            on { id }.thenReturn("1")
        }
        val jitmResponseArray = arrayOf(jitmApiResponse)
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(pathsProvider.paths).thenReturn(listOf(messagePath))
        whenever(jitmStore.fetchJitmMessage(any(), any(), any())).thenReturn(WooResult(jitmResponseArray))

        // WHEN
        cache.init()

        // THEN
        verify(jitmTracker).trackJitmFetchSuccess("screen", "1", 1)
    }

    @Test
    fun `given success response from fetching, when init, then response is cached`() = testBlocking {
        // GIVEN
        val messagePath = "path:screen:1"
        val jitmApiResponse = mock<JITMApiResponse>()
        val jitmResponseArray = arrayOf(jitmApiResponse)
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(pathsProvider.paths).thenReturn(listOf(messagePath))
        whenever(jitmStore.fetchJitmMessage(any(), any(), any())).thenReturn(WooResult(jitmResponseArray))

        // WHEN
        cache.init()

        // THEN
        cache.getMessagesForPath(messagePath = messagePath).first().let {
            assertThat(jitmApiResponse).isEqualTo(it)
        }
    }

    @Test
    fun `given 2 messages in cache, when getting message, then messages returned`() = testBlocking {
        // GIVEN
        val messagePath = "path:screen:1"
        val jitmApiResponse1 = mock<JITMApiResponse>()
        val jitmApiResponse2 = mock<JITMApiResponse>()
        val jitmResponseArray = arrayOf(jitmApiResponse1, jitmApiResponse2)
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(pathsProvider.paths).thenReturn(listOf(messagePath))
        whenever(jitmStore.fetchJitmMessage(any(), any(), any())).thenReturn(WooResult(jitmResponseArray))
        cache.init()

        // WHEN
        val messages = cache.getMessagesForPath(messagePath = messagePath)

        // THEN
        assertThat(jitmResponseArray.toList()).isEqualTo(messages)
    }

    @Test
    fun `given 1 message in cache, when dissmiss, then empty list returned`() = testBlocking {
        // GIVEN
        val messagePath = "path:screen:1"
        val jitmApiResponse1 = mock<JITMApiResponse>()
        val jitmResponseArray = arrayOf(jitmApiResponse1)
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(pathsProvider.paths).thenReturn(listOf(messagePath))
        whenever(jitmStore.fetchJitmMessage(any(), any(), any())).thenReturn(WooResult(jitmResponseArray))
        cache.init()

        // WHEN
        cache.dismissJitmMessage(messagePath, "", "")

        // THEN
        assertThat(cache.getMessagesForPath(messagePath = messagePath)).isEmpty()
    }

    @Test
    fun `when onCtaClicked, then message evicted from cache`() = testBlocking {
        // GIVEN
        val messagePath = "path:screen:1"
        val jitmApiResponse1 = mock<JITMApiResponse>()
        val jitmResponseArray = arrayOf(jitmApiResponse1)
        whenever(selectedSite.exists()).thenReturn(true)
        whenever(pathsProvider.paths).thenReturn(listOf(messagePath))
        whenever(jitmStore.fetchJitmMessage(any(), any(), any())).thenReturn(WooResult(jitmResponseArray))
        cache.init()

        // WHEN
        cache.onCtaClicked(messagePath)

        // THEN
        assertThat(cache.getMessagesForPath(messagePath = messagePath)).isEmpty()
    }

    @Test
    fun `given concurrent calls to getMessagesForPath, when init is not complete, then IllegalStateException is thrown`() =
        testBlocking {
            // GIVEN
            val messagePath = "path:screen:1"
            val jitmResponseArray = arrayOf(mock<JITMApiResponse>())
            whenever(selectedSite.exists()).thenReturn(true)
            whenever(pathsProvider.paths).thenReturn(listOf(messagePath))
            whenever(jitmStore.fetchJitmMessage(any(), any(), any())).thenAnswer {
                runBlocking { delay(10) }
                WooResult(jitmResponseArray)
            }

            val exceptions = mutableListOf<Throwable>()

            // WHEN
            cache.init()
            val jobs = List(100) {
                async(Dispatchers.Default) {
                    try {
                        cache.getMessagesForPath(messagePath)
                    } catch (e: Exception) {
                        exceptions.add(e)
                    }
                }
            }

            jobs.forEach { it.await() }

            // THEN
            assertThat(exceptions).isEmpty()
        }
}
