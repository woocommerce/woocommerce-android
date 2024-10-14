package com.woocommerce.android.ui.customfields.editor

import android.text.TextUtils
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
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
    private val analyticsTracker = mock<AnalyticsTrackerWrapper>()
    private lateinit var viewModel: CustomFieldsEditorViewModel

    private lateinit var htmlUtilsStaticMock: MockedStatic<HtmlUtils>

    @Before
    fun prepare() {
        /** [HtmlUtils.fastStripHtml] uses internally [TextUtils.isEmpty], so we need to mock it */
        htmlUtilsStaticMock = mockStatic(HtmlUtils::class.java)
        whenever(HtmlUtils.fastStripHtml(any())).thenAnswer { invocation -> invocation.arguments[0] }
    }

    @After
    fun tearDown() {
        htmlUtilsStaticMock.close()
    }

    suspend fun setup(editing: Boolean, prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = CustomFieldsEditorViewModel(
            savedStateHandle = CustomFieldsEditorFragmentArgs(
                parentItemId = PARENT_ITEM_ID,
                customField = if (editing) CustomFieldUiModel(CUSTOM_FIELD) else null
            ).toSavedStateHandle(),
            repository = repository,
            analyticsTracker = analyticsTracker
        )
    }

    @Test
    fun `when the screen is opened, then track loaded event`() = testBlocking {
        setup(editing = true)

        verify(analyticsTracker).track(
            stat = eq(AnalyticsEvent.CUSTOM_FIELD_EDITOR_LOADED),
            properties = any()
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
    fun `given editing an existing field, when key is changed, then enable done button`() = testBlocking {
        setup(editing = true)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onKeyChanged("new key")
        }.last()

        assertThat(state.enableDoneButton).isTrue()
    }

    @Test
    fun `given editing an existing field, when value is changed, then enable done button`() = testBlocking {
        setup(editing = true)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onValueChanged("new value")
        }.last()

        assertThat(state.enableDoneButton).isTrue()
    }

    @Test
    fun `given creating a new field, when the key is not empty, then enable done button`() = testBlocking {
        setup(editing = false)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onKeyChanged("key")
        }.last()

        assertThat(state.enableDoneButton).isTrue()
    }

    @Test
    fun `when key is empty, then disable done button`() = testBlocking {
        setup(editing = false)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onKeyChanged("")
        }.last()

        assertThat(state.enableDoneButton).isFalse()
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
    fun `given editing an existing field, when done is clicked, then exit with result`() = testBlocking {
        setup(editing = true)

        val events = viewModel.event.runAndCaptureValues {
            viewModel.onKeyChanged("new key")
            viewModel.onValueChanged("new value")
            viewModel.onDoneClicked()
        }.last()

        assertThat(events).isEqualTo(
            MultiLiveEvent.Event.ExitWithResult(
                data = CustomFieldsEditorViewModel.CustomFieldUpdateResult(
                    CUSTOM_FIELD.key,
                    CustomFieldUiModel(id = CUSTOM_FIELD_ID, key = "new key", value = "new value")
                ),
                key = CustomFieldsEditorViewModel.CUSTOM_FIELD_UPDATED_RESULT_KEY
            )
        )
        verify(analyticsTracker).track(
            stat = eq(AnalyticsEvent.CUSTOM_FIELD_EDITOR_DONE_TAPPED),
            properties = any()
        )
    }

    @Test
    fun `given creating a new field, when done is clicked, then exit with result`() = testBlocking {
        setup(editing = false) {
            whenever(repository.getDisplayableCustomFields(PARENT_ITEM_ID)).thenReturn(emptyList())
        }

        val events = viewModel.event.runAndCaptureValues {
            viewModel.onKeyChanged("key")
            viewModel.onValueChanged("value")
            viewModel.onDoneClicked()
        }.last()

        assertThat(events).isEqualTo(
            MultiLiveEvent.Event.ExitWithResult(
                data = CustomFieldUiModel(key = "key", value = "value"),
                key = CustomFieldsEditorViewModel.CUSTOM_FIELD_CREATED_RESULT_KEY
            )
        )
        verify(analyticsTracker).track(
            stat = eq(AnalyticsEvent.CUSTOM_FIELD_EDITOR_DONE_TAPPED),
            properties = any()
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
        assertThat(state.enableDoneButton).isFalse()
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
    fun `given editing an existing field, when delete is clicked, then return result`() = testBlocking {
        setup(editing = true)

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onDeleteClicked()
        }.last()

        assertThat(event).isEqualTo(
            MultiLiveEvent.Event.ExitWithResult(
                data = CustomFieldUiModel(CUSTOM_FIELD),
                key = CustomFieldsEditorViewModel.CUSTOM_FIELD_DELETED_RESULT_KEY
            )
        )
        verify(analyticsTracker).track(AnalyticsEvent.CUSTOM_FIELD_EDITOR_DELETE_TAPPED)
    }

    @Test
    fun `when key starts with underscore, then show error`() = testBlocking {
        setup(editing = true)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onKeyChanged("_key")
        }.last()

        assertThat(state.keyErrorMessage)
            .isEqualTo(UiString.UiStringRes(R.string.custom_fields_editor_key_error_underscore))
        assertThat(state.enableDoneButton).isFalse()
    }

    @Test
    fun `when tapping copy key, then copy key to clipboard`() = testBlocking {
        setup(editing = true)

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onCopyKeyClicked()
        }.last()

        assertThat(event).isEqualTo(
            CustomFieldsEditorViewModel.CopyContentToClipboard(
                R.string.custom_fields_editor_key_label,
                CUSTOM_FIELD.key
            )
        )
    }

    @Test
    fun `when tapping copy value, then copy value to clipboard`() = testBlocking {
        setup(editing = true)

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onCopyValueClicked()
        }.last()

        assertThat(event).isEqualTo(
            CustomFieldsEditorViewModel.CopyContentToClipboard(
                R.string.custom_fields_editor_value_label,
                CUSTOM_FIELD.valueAsString
            )
        )
    }

    @Test
    fun `when toggle editor mode, then update the state`() = testBlocking {
        setup(editing = true)

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onEditorModeChanged(true)
        }.last()

        assertThat(state.useHtmlEditor).isTrue()
        verify(analyticsTracker).track(
            stat = eq(AnalyticsEvent.CUSTOM_FIELD_EDITOR_PICKER_TAPPED),
            properties = any()
        )
    }
}
