package com.woocommerce.android.ui.customfields.editor

import android.text.TextUtils
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.customfields.CustomField
import com.woocommerce.android.ui.customfields.CustomFieldUiModel
import com.woocommerce.android.ui.customfields.CustomFieldsRepository
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.util.HtmlUtils

@OptIn(ExperimentalCoroutinesApi::class)
class CustomFieldsEditorViewModelTest : BaseUnitTest() {
    companion object {
        private const val PARENT_ITEM_ID = 0L
        private const val CUSTOM_FIELD_ID = 1L
        private val CUSTOM_FIELD = CustomField(id = CUSTOM_FIELD_ID, key = "key", value = "value")
    }

    private val repository = mock<CustomFieldsRepository>()
    private lateinit var viewModel: CustomFieldsEditorViewModel
    private lateinit var textUtilsStaticMock: MockedStatic<TextUtils>

    @Before
    fun prepare() {
        /** [HtmlUtils.fastStripHtml] uses internally [TextUtils.isEmpty], so we need to mock it */
        textUtilsStaticMock = mockStatic(TextUtils::class.java)
        whenever(TextUtils.isEmpty(any())).thenAnswer { invocation ->
            invocation.getArgument<String>(0).isEmpty()
        }
    }

    @After
    fun tearDown() {
        textUtilsStaticMock.close()
    }

    suspend fun setup(editing: Boolean, prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = CustomFieldsEditorViewModel(
            savedStateHandle = CustomFieldsEditorFragmentArgs(
                parentItemId = PARENT_ITEM_ID,
                customField = if (editing) CustomFieldUiModel(CUSTOM_FIELD) else null
            ).toSavedStateHandle(),
            repository = repository
        )
    }

    @Test
    fun `given editing an existing field, when the screen is opened, then load the existing field`() = testBlocking {
        setup(editing = true)

        val state = viewModel.state.getOrAwaitValue()

        assertThat(state.customField.id).isEqualTo(CUSTOM_FIELD_ID)
        assertThat(state.customField.key).isEqualTo("key")
        assertThat(state.customField.value).isEqualTo("value")
    }

    @Test
    fun `given creating a new field, when the screen is opened, then load an empty field`() = testBlocking {
        setup(editing = false)

        val state = viewModel.state.getOrAwaitValue()

        assertThat(state.customField.id).isNull()
        assertThat(state.customField.key).isEmpty()
        assertThat(state.customField.value).isEmpty()
    }

    @Test
    fun `when the key is changed, then update the key`() = testBlocking {
        setup(editing = true)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onKeyChanged("new key")
        }.last()

        assertThat(state.customField.key).isEqualTo("new key")
    }

    @Test
    fun `when the value is changed, then update the value`() = testBlocking {
        setup(editing = true)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onValueChanged("new value")
        }.last()

        assertThat(state.customField.value).isEqualTo("new value")
    }

    @Test
    fun `given editing an existing field, when key is changed, then show done button`() = testBlocking {
        setup(editing = true)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onKeyChanged("new key")
        }.last()

        assertThat(state.showDoneButton).isTrue()
    }

    @Test
    fun `given editing an existing field, when value is changed, then show done button`() = testBlocking {
        setup(editing = true)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onValueChanged("new value")
        }.last()

        assertThat(state.showDoneButton).isTrue()
    }

    @Test
    fun `given creating a new field, when the key is not empty, then show done button`() = testBlocking {
        setup(editing = false)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onKeyChanged("key")
        }.last()

        assertThat(state.showDoneButton).isTrue()
    }

    @Test
    fun `when key is empty, then hide done button`() = testBlocking {
        setup(editing = false)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onKeyChanged("")
        }.last()

        assertThat(state.showDoneButton).isFalse()
    }

    @Test
    fun `given no changes, when back is clicked, then exit`() = testBlocking {
        setup(editing = true)

        val events = viewModel.event.runAndCaptureValues {
            viewModel.onBackClick()
        }.last()

        assertThat(events).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `given changes, when back is clicked, then show discard dialog`() = testBlocking {
        setup(editing = true)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onKeyChanged("new key")
            viewModel.onBackClick()
        }.last()

        assertThat(state.discardChangesDialogState).isNotNull
    }

    @Test
    fun `when done is clicked, then exit with result`() = testBlocking {
        setup(editing = true)

        val events = viewModel.event.runAndCaptureValues {
            viewModel.onKeyChanged("new key")
            viewModel.onValueChanged("new value")
            viewModel.onDoneClicked()
        }.last()

        assertThat(events).isEqualTo(
            MultiLiveEvent.Event.ExitWithResult(
                CustomFieldUiModel(id = CUSTOM_FIELD_ID, key = "new key", value = "new value")
            )
        )
    }

    @Test
    fun `given adding a new field, when key is duplicate, then show error`() = testBlocking {
        setup(editing = false) {
            whenever(repository.getDisplayableCustomFields(PARENT_ITEM_ID)).thenReturn(
                listOf(CUSTOM_FIELD)
            )
        }

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onKeyChanged("key")
            viewModel.onDoneClicked()
        }.last()

        assertThat(state.keyErrorMessage)
            .isEqualTo(UiString.UiStringRes(R.string.custom_fields_editor_key_error_duplicate))
        assertThat(state.showDoneButton).isFalse()
    }

    @Test
    fun `given editing an existing field, when key is duplicate, then do not show error`() = testBlocking {
        setup(editing = true)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onKeyChanged("key")
            viewModel.onDoneClicked()
        }.last()

        assertThat(state.keyErrorMessage).isNull()
    }

    @Test
    fun `when key starts with underscore, then show error`() = testBlocking {
        setup(editing = true)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onKeyChanged("_key")
        }.last()

        assertThat(state.keyErrorMessage)
            .isEqualTo(UiString.UiStringRes(R.string.custom_fields_editor_key_error_underscore))
        assertThat(state.showDoneButton).isFalse()
    }
}
