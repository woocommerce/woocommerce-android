package com.woocommerce.android.apifaker

import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import com.google.gson.Gson
import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.models.ApiType
import com.woocommerce.android.apifaker.models.MockedEndpoint
import com.woocommerce.android.apifaker.models.Request
import com.woocommerce.android.apifaker.models.Response
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.io.ByteArrayOutputStream

class EndpointExportManagerTest {
    private val mockedEndpoints = listOf(
        MockedEndpoint(
            request = Request(
                id = 1,
                type = ApiType.WPApi,
                path = "/wp/v2/posts"
            ),
            response = Response(
                endpointId = 1,
                statusCode = 200,
                body = "[]"
            )
        )
    )
    private val gson = Gson()
    private val contentResolver = mock<ContentResolver>()
    private val context = mock<Context> {
        on { contentResolver } doReturn contentResolver
    }
    private val endpointDao = mock<EndpointDao>()
    private val clipDataFactory = mock<ClipDataFactory> {
        on { createClipData(any()) } doReturn mock()
    }

    private val sut = EndpointExportManager(
        gson = gson,
        endpointDao = endpointDao,
        context = context,
        clipDataFactory = clipDataFactory
    )

    @Test
    fun `when exportEndpoints is called with file destination, it should export the endpoints to the given uri`() =
        runTest {
            val stream = ByteArrayOutputStream()
            given(contentResolver.openOutputStream(any())).willReturn(stream)

            val result = sut.exportEndpoints(mockedEndpoints, ExportImportDestination.File(mock()))
            val exportedEndpoints = gson.fromJson(stream.toString(), Array<MockedEndpoint>::class.java).toList()

            assertTrue(result.isSuccess)
            val expectedEndpoints = mockedEndpoints.map {
                it.copy(
                    request = it.request.copy(id = 0),
                    response = it.response.copy(endpointId = 0)
                )
            }
            assertEquals(expectedEndpoints, exportedEndpoints)
        }

    @Test
    fun `when importEndpoints is called with file destination, it should import the endpoints from the given uri`() =
        runTest {
            val stream = gson.toJson(mockedEndpoints).byteInputStream()
            given(contentResolver.openInputStream(any())).willReturn(stream)

            val result = sut.importEndpoints(ExportImportDestination.File(mock()))

            assertTrue(result.isSuccess)
            verify(endpointDao).insertEndpoints(mockedEndpoints)
        }

    @Test
    fun `when exportEndpoints is called with clipboard destination, it should export the endpoints to the clipboard`() =
        runTest {
            val clipboardManager = mock<ClipboardManager>()
            given(context.getSystemService(Context.CLIPBOARD_SERVICE)).willReturn(clipboardManager)

            val result = sut.exportEndpoints(mockedEndpoints, ExportImportDestination.Clipboard)

            assertTrue(result.isSuccess)
            verify(clipboardManager).setPrimaryClip(any())
        }

    @Test
    fun `when importEndpoints is called with clipboard destination, it should import the endpoints from the clipboard`() =
        runTest {
            val json = gson.toJson(mockedEndpoints)
            val clipItem = mock<android.content.ClipData.Item> {
                on { text } doReturn json
            }
            val clipData = mock<android.content.ClipData> {
                on { itemCount } doReturn 1
                on { getItemAt(0) } doReturn clipItem
            }
            val clipboardManager = mock<ClipboardManager> {
                on { primaryClip } doReturn clipData
            }
            given(context.getSystemService(Context.CLIPBOARD_SERVICE)).willReturn(clipboardManager)

            val result = sut.importEndpoints(ExportImportDestination.Clipboard)

            assertTrue(result.isSuccess)
            verify(endpointDao).insertEndpoints(mockedEndpoints)
        }
}
