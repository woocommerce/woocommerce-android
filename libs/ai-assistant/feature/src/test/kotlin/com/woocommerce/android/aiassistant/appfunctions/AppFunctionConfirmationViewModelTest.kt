package com.woocommerce.android.aiassistant.appfunctions

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.aiassistant.chat.assistantJsonForTests
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.safety.ConfirmationPreview
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewContext
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewProvider
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewProviderRegistry
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewRenderer
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewText
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreview
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class AppFunctionConfirmationViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()
    private val renderedPreview = RenderedConfirmationPreview("Stock change preview", emptyList())
    private val previewRenderer: ConfirmationPreviewRenderer = mock()
    private val previewProviders = FakePreviewRegistry()
    private var isAllowed = true
    private var openStore = store()
    private val selectedSite: SelectedSite = mock {
        on { getIfExists() } doAnswer { openStore }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        whenever(previewRenderer.render(any())).thenReturn(renderedPreview)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given a request for the open store, when the screen opens, then it shows the preview and runs no write`() {
        val registry = Registry()

        val viewModel = viewModel(registry)

        val state = viewModel.state.value as AppFunctionConfirmationState.Pending
        assertThat(state.card.preview).isEqualTo(renderedPreview)
        assertThat(registry.calls).isEmpty()
        assertThat(previewProviders.buildCount).isEqualTo(1)
    }

    @Test
    fun `given another store is open, when the screen opens, then it reports the store change`() {
        openStore = store(id = OTHER_LOCAL_ID, url = OTHER_STORE_URL)
        val registry = Registry()

        val viewModel = viewModel(registry)

        assertThat(viewModel.state.value).isEqualTo(AppFunctionConfirmationState.StoreChanged(STORE_NAME))
        assertThat(registry.calls).isEmpty()
        assertThat(previewProviders.buildCount).isZero()
    }

    @Test
    fun `given access is off, when an old confirmation opens, then the request is unavailable`() {
        isAllowed = false
        val registry = Registry()

        val viewModel = viewModel(registry)

        assertThat(viewModel.state.value).isEqualTo(AppFunctionConfirmationState.Unavailable)
        assertThat(registry.calls).isEmpty()
        assertThat(previewProviders.buildCount).isZero()
    }

    @Test
    fun `given request extras are missing, when the screen opens, then the request is unavailable`() {
        val registry = Registry()

        val viewModel = viewModel(registry, SavedStateHandle())

        assertThat(viewModel.state.value).isEqualTo(AppFunctionConfirmationState.Unavailable)
        assertThat(registry.calls).isEmpty()
    }

    @Test
    fun `given the request names an unknown tool, when the screen opens, then it is unavailable`() {
        val registry = Registry(descriptors = emptyList())

        val viewModel = viewModel(registry, savedState(toolName = "unknown_tool"))

        assertThat(viewModel.state.value).isEqualTo(AppFunctionConfirmationState.Unavailable)
        assertThat(previewProviders.buildCount).isZero()
    }

    @Test
    fun `given a pending change, when the merchant confirms, then that tool call runs and the screen is done`() {
        val registry = Registry()
        val viewModel = viewModel(registry)
        val expectedCall = (viewModel.state.value as AppFunctionConfirmationState.Pending).card.toolCall

        viewModel.onConfirm()

        assertThat(registry.calls).containsExactly(expectedCall)
        assertThat(viewModel.state.value).isInstanceOf(AppFunctionConfirmationState.Done::class.java)
    }

    @Test
    fun `given the save fails, when the merchant confirms, then the screen asks them to check the app`() {
        val registry = Registry { ToolResult.TransportError(it.id, retryable = true) }
        val viewModel = viewModel(registry)

        viewModel.onConfirm()

        assertThat(viewModel.state.value).isEqualTo(AppFunctionConfirmationState.CheckInApp)
    }

    @Test
    fun `given the store changed after opening, when the merchant confirms, then nothing runs`() {
        val registry = Registry()
        val viewModel = viewModel(registry)
        openStore = store(id = OTHER_LOCAL_ID, url = OTHER_STORE_URL)

        viewModel.onConfirm()

        assertThat(viewModel.state.value).isEqualTo(AppFunctionConfirmationState.StoreChanged(STORE_NAME))
        assertThat(registry.calls).isEmpty()
    }

    @Test
    fun `given access was turned off after opening, when the merchant confirms, then nothing runs`() {
        val registry = Registry()
        val viewModel = viewModel(registry)
        isAllowed = false

        viewModel.onConfirm()

        assertThat(viewModel.state.value).isEqualTo(AppFunctionConfirmationState.Unavailable)
        assertThat(registry.calls).isEmpty()
    }

    @Test
    fun `given a restored consumed request, when the screen opens, then it is not previewed or executed`() {
        val registry = Registry()
        val state = savedState().apply {
            set(AppFunctionConfirmationViewModel.KEY_REQUEST_CONSUMED, true)
        }

        val viewModel = viewModel(registry, state)

        assertThat(viewModel.state.value).isEqualTo(AppFunctionConfirmationState.CheckInApp)
        assertThat(previewProviders.buildCount).isZero()
        assertThat(registry.calls).isEmpty()
    }

    @Test
    fun `given confirm is tapped while saving and after done, when it is tapped again, then the write runs once`() = runTest {
        val started = CompletableDeferred<Unit>()
        val result = CompletableDeferred<ToolResult>()
        val registry = Registry {
            started.complete(Unit)
            result.await()
        }
        val viewModel = viewModel(registry)

        viewModel.onConfirm()
        started.await()
        viewModel.onConfirm()
        assertThat(viewModel.state.value).isInstanceOf(AppFunctionConfirmationState.Saving::class.java)
        assertThat(registry.calls).hasSize(1)

        result.complete(ToolResult.Success("request-1", buildJsonObject {}))
        assertThat(viewModel.state.value).isInstanceOf(AppFunctionConfirmationState.Done::class.java)
        viewModel.onConfirm()

        assertThat(registry.calls).hasSize(1)
    }

    private fun viewModel(
        registry: Registry,
        state: SavedStateHandle = savedState(),
    ) = AppFunctionConfirmationViewModel(
        savedStateHandle = state,
        toolRegistry = registry,
        storeAccess = AppFunctionsStoreAccess(selectedSite, AppFunctionsAccessPolicy { isAllowed }),
        previewProviders = previewProviders,
        previewRenderer = previewRenderer,
        json = assistantJsonForTests(),
    )

    private fun savedState(toolName: String = PRODUCTS_UPDATE) = SavedStateHandle(
        mapOf(
            EXTRA_REQUEST_ID to REQUEST_ID,
            EXTRA_TOOL_NAME to toolName,
            EXTRA_TOOL_ARGUMENTS to """{"id":12,"stock_quantity":5}""",
            EXTRA_SITE_LOCAL_ID to LOCAL_ID,
            EXTRA_STORE_URL to STORE_URL,
            EXTRA_STORE_NAME to STORE_NAME,
        ),
    )

    private fun store(id: Int = LOCAL_ID, url: String = STORE_URL) = SiteModel().apply {
        this.id = id
        this.url = url
    }

    private class Registry(
        descriptors: List<ToolDescriptor> = listOf(descriptor()),
        private val answer: suspend (ToolCall) -> ToolResult = {
            ToolResult.Success(it.id, buildJsonObject {})
        },
    ) : ToolRegistry {
        private val toolDescriptors = descriptors
        val calls = mutableListOf<ToolCall>()

        override fun descriptors() = toolDescriptors

        override suspend fun execute(call: ToolCall): ToolResult {
            calls += call
            return answer(call)
        }
    }

    private class FakePreviewRegistry : ConfirmationPreviewProviderRegistry {
        var buildCount = 0
            private set

        override fun providerFor(context: ConfirmationPreviewContext): ConfirmationPreviewProvider =
            throw UnsupportedOperationException("The view model calls buildPreview directly")

        override suspend fun buildPreview(context: ConfirmationPreviewContext): ConfirmationPreview {
            buildCount += 1
            return ConfirmationPreview(ConfirmationPreviewText.Raw("Stock change preview"))
        }
    }

    private companion object {
        const val REQUEST_ID = "request-1"
        const val LOCAL_ID = 7
        const val OTHER_LOCAL_ID = 8
        const val STORE_URL = "https://bean.example"
        const val OTHER_STORE_URL = "https://other.example"
        const val STORE_NAME = "Bean Shop"
        const val PRODUCTS_UPDATE = "products_update"

        fun descriptor() = ToolDescriptor(
            name = PRODUCTS_UPDATE,
            description = "Updates product stock.",
            inputSchema = buildJsonObject {},
            safetyLevel = ToolSafetyLevel.UNSAFE,
        )
    }
}
