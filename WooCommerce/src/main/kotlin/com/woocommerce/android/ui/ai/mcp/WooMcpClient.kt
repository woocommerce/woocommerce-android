package com.woocommerce.android.ui.ai.mcp

import com.woocommerce.android.tools.SelectedSite
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooMcpClient @Inject constructor(
    private val selectedSite: SelectedSite
) {
    private var mcpClient: Client? = null
    private var httpClient: HttpClient? = null
    private var cachedTools: List<Tool> = emptyList()
    private val requestIdCounter = AtomicLong(1)
    private val idMapping = ConcurrentHashMap<Long, String>()

    val isConnected: Boolean
        get() = mcpClient != null

    val availableTools: List<Tool>
        get() = cachedTools

    suspend fun connect(
        consumerKey: String,
        consumerSecret: String
    ): Result<Unit> = runCatching {
        disconnect()
        requestIdCounter.set(1)
        idMapping.clear()

        val siteUrl = selectedSite.get().url.trimEnd('/')
        val mcpUrl = "$siteUrl/wp-json/woocommerce/mcp"

        val ktorClient = HttpClient(OkHttp) {
            engine {
                addInterceptor(createCompatibilityInterceptor())
            }
            install(SSE)
        }
        httpClient = ktorClient

        val transport = StreamableHttpClientTransport(
            client = ktorClient,
            url = mcpUrl,
        ) {
            headers.append(MCP_API_KEY_HEADER, "$consumerKey:$consumerSecret")
        }

        val client = Client(
            clientInfo = Implementation(
                name = "woocommerce-android",
                version = "1.0.0"
            )
        )

        client.connect(transport)
        mcpClient = client
    }

    suspend fun discoverTools(): Result<List<Tool>> = runCatching {
        val client = requireNotNull(mcpClient) { "MCP client not connected" }
        val result = client.listTools()
        cachedTools = result.tools
        result.tools
    }

    suspend fun executeTool(
        name: String,
        arguments: Map<String, Any?>
    ): Result<String> = runCatching {
        val client = requireNotNull(mcpClient) { "MCP client not connected" }
        val result: CallToolResult = client.callTool(name, arguments)
        extractTextFromResult(result)
    }

    suspend fun disconnect() {
        runCatching { mcpClient?.close() }
        mcpClient = null
        httpClient?.close()
        httpClient = null
        cachedTools = emptyList()
    }

    private fun extractTextFromResult(result: CallToolResult): String {
        return result.content.joinToString("\n") { block ->
            when (block) {
                is TextContent -> block.text
                else -> block.toString()
            }
        }
    }

    /**
     * Interceptor that patches WooCommerce MCP server compatibility issues:
     *
     * Request: The SDK sends UUID string IDs but the server expects integer IDs.
     *          The mapping is stored so response IDs can be restored to the original UUIDs.
     * Response: Integer IDs are mapped back to the original UUID strings so the SDK can
     *           match responses to pending requests.
     *           PHP's json_encode outputs [] for empty associative arrays, but the SDK
     *           expects {} (JSON objects) for capability fields like "tools", "resources", etc.
     */
    private fun createCompatibilityInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request()
        val body = request.body
        val contentType = body?.contentType()?.toString() ?: ""

        val newRequest = if (request.method == "POST" && contentType.contains("json")) {
            val buffer = Buffer()
            body?.writeTo(buffer)
            val originalJson = buffer.readUtf8()
            val rewrittenJson = STRING_ID_PATTERN.replace(originalJson) { matchResult ->
                val originalUuid = matchResult.groupValues[1]
                val nextId = requestIdCounter.getAndIncrement()
                idMapping[nextId] = originalUuid
                "\"id\":$nextId"
            }
            request.newBuilder()
                .method(request.method, rewrittenJson.toRequestBody(JSON_MEDIA_TYPE))
                .build()
        } else {
            request
        }

        val response = chain.proceed(newRequest)

        val responseMediaType = response.body.contentType()
        val responseContentType = responseMediaType?.toString() ?: ""
        if (!responseContentType.contains("json")) {
            return@Interceptor response
        }

        var responseBody = response.body.string()
        responseBody = CAPABILITY_EMPTY_ARRAY_PATTERN.replace(responseBody, "$1:{}")
        responseBody = INTEGER_ID_PATTERN.replace(responseBody) { matchResult ->
            val integerId = matchResult.groupValues[1].toLongOrNull()
            val originalUuid = integerId?.let { idMapping.remove(it) }
            if (originalUuid != null) {
                "\"id\":\"$originalUuid\""
            } else {
                matchResult.value
            }
        }

        response.newBuilder()
            .body(responseBody.toResponseBody(responseMediaType))
            .build()
    }

    companion object {
        private const val MCP_API_KEY_HEADER = "X-MCP-API-Key"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val STRING_ID_PATTERN = """"id"\s*:\s*"([^"]+)"""".toRegex()
        private val INTEGER_ID_PATTERN = """"id"\s*:\s*(\d+)""".toRegex()
        private val CAPABILITY_EMPTY_ARRAY_PATTERN =
            """("(?:tools|resources|prompts|logging|completions)")\s*:\s*\[]""".toRegex()
    }
}
