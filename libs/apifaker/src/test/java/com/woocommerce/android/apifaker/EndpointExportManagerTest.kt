package com.woocommerce.android.apifaker

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

    private val sut = EndpointExportManager(
        gson = gson,
        endpointDao = endpointDao,
        context = context
    )

    @Test
    fun `when exportEndpoints is called, it should export the endpoints to the given uri`() = runTest {
        val stream = ByteArrayOutputStream()
        given(contentResolver.openOutputStream(any())).willReturn(stream)

        val result = sut.exportEndpoints(mockedEndpoints, mock())
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
    fun `when importEndpoints is called, it should import the endpoints from the given uri`() = runTest {
        val stream = gson.toJson(mockedEndpoints).byteInputStream()
        given(contentResolver.openInputStream(any())).willReturn(stream)

        val result = sut.importEndpoints(mock())

        assertTrue(result.isSuccess)
        verify(endpointDao).insertEndpoints(mockedEndpoints)
    }
}
