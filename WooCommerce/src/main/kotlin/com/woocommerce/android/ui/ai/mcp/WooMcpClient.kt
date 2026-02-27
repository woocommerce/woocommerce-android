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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooMcpClient @Inject constructor(
    private val selectedSite: SelectedSite
) {
    private var mcpClient: Client? = null
    private var httpClient: HttpClient? = null
    private var cachedTools: List<Tool> = emptyList()

    val isConnected: Boolean
        get() = mcpClient != null

    val availableTools: List<Tool>
        get() = cachedTools

    suspend fun connect(
        consumerKey: String,
        consumerSecret: String
    ): Result<Unit> = runCatching {
        disconnect()

        val siteUrl = selectedSite.get().url.trimEnd('/')
        val mcpUrl = "$siteUrl/wp-json/woocommerce/mcp"

        val ktorClient = HttpClient(OkHttp) {
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

    companion object {
        private const val MCP_API_KEY_HEADER = "X-MCP-API-Key"
    }
}
