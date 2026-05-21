package com.woocommerce.android.aiassistant.headless

import com.woocommerce.android.aiassistant.config.AssistantSystemPromptProvider
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.FinishReason
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.loop.BudgetedHistory
import com.woocommerce.android.aiassistant.core.loop.ConservativeRetryPolicy
import com.woocommerce.android.aiassistant.core.loop.HistoryBudgeter
import com.woocommerce.android.aiassistant.tools.DefaultToolCatalogSelector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import kotlin.time.TimeSource

internal object WooAiSmokeDeterministicSupportFixtures {
    const val SITE_ID = 2922L

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
    }

    fun stableOutputDirectory(): File {
        val userDir = requireNotNull(System.getProperty("user.dir")) {
            "Missing user.dir system property"
        }
        val workingDir = File(userDir).canonicalFile
        val repoRoot = generateSequence(workingDir) { file: File -> file.parentFile }.firstOrNull {
            File(it, "settings.gradle").isFile && File(it, "libs/ai-assistant/feature").isDirectory
        }
        val moduleDir = when {
            repoRoot != null -> File(repoRoot, "libs/ai-assistant/feature")
            workingDir.name == "feature" && workingDir.parentFile?.name == "ai-assistant" -> workingDir
            else -> error("Could not resolve :libs:ai-assistant:feature from working dir: $workingDir")
        }
        return File(moduleDir, "build/outputs/woo-ai-smoke/latest")
    }

    fun runner(outputDirectory: File) = runner(
        outputDirectory = outputDirectory,
        chatService = WooAiSmokeDeterministicSupportChatService(),
        toolRegistry = WooAiSmokeDeterministicSupportToolRegistry(),
    )

    fun runner(
        outputDirectory: File,
        chatService: ChatService,
        toolRegistry: ToolRegistry,
        config: WooAiSmokeConfig = deterministicConfig(),
    ) = WooAiSmokeRunner(
        chatService = chatService,
        toolRegistry = toolRegistry,
        toolCatalogSelector = DefaultToolCatalogSelector(),
        retryPolicy = ConservativeRetryPolicy,
        historyBudgeter = HistoryBudgeter { system, transcript, user ->
            BudgetedHistory(listOf(system) + transcript + user)
        },
        systemPromptProvider = StaticSystemPromptProvider,
        json = json,
        timeSource = TimeSource.Monotonic,
        config = config,
        selectedSiteId = SITE_ID,
        outputDirectory = outputDirectory,
        authProviderClass = "none",
        storeLabel = "deterministic-support",
        credentialSource = "support-fixtures",
        redactor = WooAiSmokeRedactor(
            siteUrl = "",
            username = "",
            appPassword = "",
        ),
    )

    fun deterministicConfig(
        scenarioResourceName: String = "deterministic-scenarios.json",
        sampleCount: Int = 1,
        scenarioIds: Set<String> = emptySet(),
        baseline: WooAiSmokeBaselineConfig? = null,
    ) = WooAiSmokeConfig(
        scenarioResourceName = scenarioResourceName,
        baseline = baseline,
        usePerRunDirectory = false,
        sampleCount = sampleCount,
        scenarioIds = scenarioIds,
    )

    private object StaticSystemPromptProvider : AssistantSystemPromptProvider {
        override fun systemPrompt(todayIsoDate: String?): String =
            "You are a WooCommerce store assistant for the no-device smoke harness."
    }
}

internal class WooAiSmokeDeterministicSupportChatService : ChatService {
    private val responses = mutableListOf(
        toolResponse(
            "Checking recent orders.",
            toolCall(0, "orders_list_1", "orders_list", """{"limit":3}"""),
            toolCall(1, "show_cards_orders_1", "show_cards", """{"cards":[{"type":"order","id":"1001"}]}"""),
        ),
        textResponse("Here are the latest three orders."),
        toolResponse(
            "Searching products.",
            toolCall(0, "products_list_1", "products_list", """{"search":"Cappuccino"}"""),
            toolCall(1, "show_cards_products_1", "show_cards", """{"cards":[{"type":"product","id":"2001"}]}"""),
        ),
        textResponse("I found the Cappuccino product and included its card."),
        toolResponse(
            "Checking this month's order analytics.",
            toolCall(0, "analytics_orders_1", "analytics_orders", """{"period":"month"}"""),
        ),
        textResponse("This month has 12 orders and 123.45 in revenue."),
        toolResponse(
            "Finding the newest pending order before preparing the note.",
            toolCall(0, "orders_list_write_1", "orders_list", """{"status":"pending","limit":1}"""),
            toolCall(
                1,
                "orders_update_1",
                "orders_update",
                """{"id":1001,"note":"woo-ai-smoke dry run"}""",
            ),
        ),
        textResponse("I can only help with WooCommerce store tasks, so I cannot write a poem about castles."),
    )

    override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> = flow {
        check(responses.isNotEmpty()) {
            "No scripted no-device response left for request with ${request.tools.size} tools"
        }
        responses.removeAt(0).forEach { emit(it) }
    }

    private companion object {
        fun toolResponse(
            text: String,
            vararg calls: AssistantEvent.ToolCallDelta,
        ): List<AssistantEvent> =
            listOf(AssistantEvent.TextDelta(text)) + calls + AssistantEvent.Finish(FinishReason.TOOL_CALLS)

        fun textResponse(text: String): List<AssistantEvent> =
            listOf(AssistantEvent.TextDelta(text), AssistantEvent.Finish(FinishReason.STOP))

        fun toolCall(
            index: Int,
            id: String,
            name: String,
            arguments: String,
        ) = AssistantEvent.ToolCallDelta(
            index = index,
            id = id,
            name = name,
            argumentsDelta = arguments,
        )
    }
}

internal class WooAiSmokeDeterministicSupportToolRegistry : ToolRegistry {
    val executedCalls = mutableListOf<ToolCall>()

    override fun descriptors(): List<ToolDescriptor> = DESCRIPTORS

    override suspend fun execute(call: ToolCall): ToolResult {
        executedCalls += call
        return ToolResult.Success(
            toolCallId = call.id,
            structured = structuredResultFor(call),
        )
    }

    private fun structuredResultFor(call: ToolCall): JsonObject = when (call.name) {
        "orders_list" -> buildJsonObject {
            put("count", 3)
            put("newestPendingOrderId", 1001)
        }
        "products_list" -> buildJsonObject {
            put("count", 1)
            put("productName", "Cappuccino")
        }
        "analytics_orders" -> buildJsonObject {
            put("orders", 12)
            put("revenue", "123.45")
        }
        "show_cards" -> buildJsonObject {
            put("shown", true)
        }
        else -> buildJsonObject {
            put("ok", true)
        }
    }

    private companion object {
        val DESCRIPTORS = listOf(
            descriptor("orders_list", ToolSafetyLevel.SAFE),
            descriptor("orders_get", ToolSafetyLevel.SAFE),
            descriptor("orders_update", ToolSafetyLevel.UNSAFE),
            descriptor("orders_bulk_update", ToolSafetyLevel.UNSAFE),
            descriptor("products_list", ToolSafetyLevel.SAFE),
            descriptor("products_get", ToolSafetyLevel.SAFE),
            descriptor("products_update", ToolSafetyLevel.UNSAFE),
            descriptor("products_bulk_update", ToolSafetyLevel.UNSAFE),
            descriptor("product_variations_list", ToolSafetyLevel.SAFE),
            descriptor("product_variations_update", ToolSafetyLevel.UNSAFE),
            descriptor("analytics_orders", ToolSafetyLevel.SAFE),
            descriptor("customers_list", ToolSafetyLevel.SAFE),
            descriptor("show_cards", ToolSafetyLevel.SAFE),
        )

        fun descriptor(
            name: String,
            safetyLevel: ToolSafetyLevel,
        ) = ToolDescriptor(
            name = name,
            description = "No-device smoke fixture for $name",
            inputSchema = buildJsonObject { },
            safetyLevel = safetyLevel,
        )
    }
}
