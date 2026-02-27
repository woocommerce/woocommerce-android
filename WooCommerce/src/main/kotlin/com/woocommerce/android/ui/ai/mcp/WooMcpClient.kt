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
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooMcpClient @Inject constructor(
    private val selectedSite: SelectedSite
) {
    @Volatile private var mcpClient: Client? = null
    @Volatile private var httpClient: HttpClient? = null
    @Volatile private var cachedTools: List<Tool> = emptyList()
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
            val rewrittenJson = rewriteRequestId(originalJson)
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

        val responseBody = response.body.string()
        val fixedBody = fixResponseBody(responseBody)

        response.newBuilder()
            .body(fixedBody.toResponseBody(responseMediaType))
            .build()
    }

    /**
     * Replaces the top-level JSON-RPC "id" (a UUID string) with a sequential integer
     * and stores the mapping for restoring it in the response.
     */
    private fun rewriteRequestId(json: String): String {
        val jsonObject = runCatching { JSONObject(json) }.getOrNull() ?: return json
        val originalId = jsonObject.opt("id") as? String ?: return json
        val nextId = requestIdCounter.getAndIncrement()
        idMapping[nextId] = originalId
        jsonObject.put("id", nextId)
        return jsonObject.toString()
    }

    /**
     * Fixes the response body for SDK compatibility:
     * - Restores the original UUID string "id" from the integer the server echoed back.
     * - Replaces empty JSON arrays with objects for capability fields.
     */
    private fun fixResponseBody(body: String): String {
        var fixed = CAPABILITY_EMPTY_ARRAY_PATTERN.replace(body, "$1:{}")
        val jsonObject = runCatching { JSONObject(fixed) }.getOrNull() ?: return fixed
        val responseId = jsonObject.opt("id")
        if (responseId is Number) {
            val originalUuid = idMapping.remove(responseId.toLong())
            if (originalUuid != null) {
                jsonObject.put("id", originalUuid)
                fixed = jsonObject.toString()
            }
        }
        return fixed
    }

    companion object {
        private const val MCP_API_KEY_HEADER = "X-MCP-API-Key"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val CAPABILITY_EMPTY_ARRAY_PATTERN =
            """("(?:tools|resources|prompts|logging|completions)")\s*:\s*\[]""".toRegex()
    }
}
