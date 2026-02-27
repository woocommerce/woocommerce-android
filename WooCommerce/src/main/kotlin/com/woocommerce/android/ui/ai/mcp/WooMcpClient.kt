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
import okio.Buffer
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

        val siteUrl = selectedSite.get().url.trimEnd('/')
        val mcpUrl = "$siteUrl/wp-json/woocommerce/mcp"

        val ktorClient = HttpClient(OkHttp) {
            engine {
                addInterceptor(createIntegerIdInterceptor())
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
     * The WooCommerce MCP server (wp-mcp-adapter) expects JSON-RPC `id` fields to be integers,
     * but the MCP Kotlin SDK sends them as UUID strings. This interceptor rewrites string IDs
     * to sequential integers in outgoing requests.
     */
    private fun createIntegerIdInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request()
        val body = request.body
        val contentType = body?.contentType()?.toString() ?: ""

        if (request.method != "POST" || !contentType.contains("json")) {
            return@Interceptor chain.proceed(request)
        }

        val buffer = Buffer()
        body?.writeTo(buffer)
        val originalJson = buffer.readUtf8()

        val rewrittenJson = STRING_ID_PATTERN.replace(originalJson) {
            val nextId = requestIdCounter.getAndIncrement()
            "\"id\":$nextId"
        }

        val newBody = rewrittenJson.toRequestBody(JSON_MEDIA_TYPE)
        val newRequest = request.newBuilder()
            .method(request.method, newBody)
            .build()

        chain.proceed(newRequest)
    }

    companion object {
        private const val MCP_API_KEY_HEADER = "X-MCP-API-Key"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val STRING_ID_PATTERN = """"id"\s*:\s*"[^"]+"""".toRegex()
    }
}
