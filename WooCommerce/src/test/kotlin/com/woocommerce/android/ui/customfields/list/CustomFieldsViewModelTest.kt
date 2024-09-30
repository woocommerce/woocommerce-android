package com.woocommerce.android.ui.customfields.list

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.ui.customfields.CustomField
import com.woocommerce.android.ui.customfields.CustomFieldContentType
import com.woocommerce.android.ui.customfields.CustomFieldUiModel
import com.woocommerce.android.ui.customfields.CustomFieldsRepository
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType

@OptIn(ExperimentalCoroutinesApi::class)
class CustomFieldsViewModelTest : BaseUnitTest() {
    companion object {
        private const val PARENT_ITEM_ID = 1L
        private val PARENT_ITEM_TYPE = MetaDataParentItemType.ORDER
        private val CUSTOM_FIELDS = listOf(
            CustomField(
                id = 1,
                key = "key",
                value = "value"
            ),
            CustomField(
                id = 2,
                key = "key2",
                value = "value2"
            )
        )
    }

    private val repository: CustomFieldsRepository = mock {
        onBlocking { observeDisplayableCustomFields(PARENT_ITEM_ID) }.thenReturn(flowOf(CUSTOM_FIELDS))
    }
    private val appPrefs: AppPrefsWrapper = mock {
        val bannerDismissed = MutableStateFlow(false)
        on { observePrefs() }.thenReturn(bannerDismissed.map { Unit })
        on { isCustomFieldsTopBannerDismissed } doAnswer { bannerDismissed.value }
        on { isCustomFieldsTopBannerDismissed = any() }.then { invocation ->
            bannerDismissed.update { invocation.arguments[0] as Boolean }
        }
    }
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.getArgument<Int>(0).toString() }
    }
    private lateinit var viewModel: CustomFieldsViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = CustomFieldsViewModel(
            savedStateHandle = CustomFieldsFragmentArgs(PARENT_ITEM_ID, PARENT_ITEM_TYPE).toSavedStateHandle(),
            repository = repository,
            appPrefs = appPrefs,
            resourceProvider = resourceProvider
        )
    }

    @Test
    fun `when opening screen, then custom fields are loaded`() = testBlocking {
        setup()

        val state = viewModel.state.getOrAwaitValue()

        assertThat(state.customFields).hasSize(CUSTOM_FIELDS.size)
        CUSTOM_FIELDS.forEachIndexed { index, customField ->
            assertThat(state.customFields[index].key).isEqualTo(customField.key)
            assertThat(state.customFields[index].value).isEqualTo(customField.valueAsString)
        }
    }

    @Test
    fun `when pull to refresh, then custom fields are refreshed`() = testBlocking {
        setup {
            whenever(repository.refreshCustomFields(PARENT_ITEM_ID, PARENT_ITEM_TYPE)).doSuspendableAnswer {
                delay(10L)
                Result.success(Unit)
            }
        }

        val states = viewModel.state.runAndCaptureValues {
            viewModel.onPullToRefresh()
            advanceUntilIdle()
        }.drop(1)

        verify(repository).refreshCustomFields(PARENT_ITEM_ID, PARENT_ITEM_TYPE)
        assertThat(states.first().isRefreshing).isTrue()
        assertThat(states.last().isRefreshing).isFalse()
    }

    @Test
    fun `when refresh fails, then error message is shown`() = testBlocking {
        setup {
            whenever(repository.refreshCustomFields(PARENT_ITEM_ID, PARENT_ITEM_TYPE)).doSuspendableAnswer {
                delay(10L)
                Result.failure(Exception())
            }
        }

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onPullToRefresh()
            advanceUntilIdle()
        }.last()

        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.custom_fields_list_loading_error))
    }

    @Test
    fun `when back button is clicked, then exit event is triggered`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onBackClick()
        }.last()

        assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `given custom field contains URL, when custom field value is clicked, then URL is opened`() = testBlocking {
        val customField = CustomField(
            id = 1,
            key = "key",
            value = "https://example.com"
        )
        setup {
            whenever(repository.observeDisplayableCustomFields(PARENT_ITEM_ID)).thenReturn(flowOf(listOf(customField)))
        }

        val uiModel = CustomFieldUiModel(customField)
        val event = viewModel.event.runAndCaptureValues {
            viewModel.onCustomFieldValueClicked(uiModel)
        }.last()

        assertThat(event).isEqualTo(CustomFieldsViewModel.CustomFieldValueClicked(uiModel))
        assertThat(uiModel.contentType).isEqualTo(CustomFieldContentType.URL)
    }

    @Test
    fun `given custom field contains email, when custom field value is clicked, then email is opened`() = testBlocking {
        val customField = CustomField(
            id = 1,
            key = "key",
            value = "email@host.com"
        )
        setup {
            whenever(repository.observeDisplayableCustomFields(PARENT_ITEM_ID)).thenReturn(flowOf(listOf(customField)))
        }

        val uiModel = CustomFieldUiModel(customField)
        val event = viewModel.event.runAndCaptureValues {
            viewModel.onCustomFieldValueClicked(uiModel)
        }.last()

        assertThat(event).isEqualTo(CustomFieldsViewModel.CustomFieldValueClicked(uiModel))
        assertThat(uiModel.contentType).isEqualTo(CustomFieldContentType.EMAIL)
    }

    @Test
    fun `given custom field contains phone, when custom field value is clicked, then phone is opened`() = testBlocking {
        val customField = CustomField(
            id = 1,
            key = "key",
            value = "tel://123456789"
        )
        setup {
            whenever(repository.observeDisplayableCustomFields(PARENT_ITEM_ID)).thenReturn(flowOf(listOf(customField)))
        }

        val uiModel = CustomFieldUiModel(customField)
        val event = viewModel.event.runAndCaptureValues {
            viewModel.onCustomFieldValueClicked(uiModel)
        }.last()

        assertThat(event).isEqualTo(CustomFieldsViewModel.CustomFieldValueClicked(uiModel))
        assertThat(uiModel.contentType).isEqualTo(CustomFieldContentType.PHONE)
    }

    @Test
    fun `when tapping on a custom field, then custom field editor is opened`() = testBlocking {
        val customField = CustomField(
            id = 1,
            key = "key",
            value = "value"
        )
        setup {
            whenever(repository.observeDisplayableCustomFields(PARENT_ITEM_ID)).thenReturn(flowOf(listOf(customField)))
        }

        val uiModel = CustomFieldUiModel(customField)
        val event = viewModel.event.runAndCaptureValues {
            viewModel.onCustomFieldClicked(uiModel)
        }.last()

        assertThat(event).isEqualTo(CustomFieldsViewModel.OpenCustomFieldEditor(uiModel))
    }

    @Test
    fun `when tapping on add custom field, then custom field editor is opened`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onAddCustomFieldClicked()
        }.last()

        assertThat(event).isEqualTo(CustomFieldsViewModel.OpenCustomFieldEditor(null))
    }

    @Test
    fun `when updating a custom field, then custom fields are refreshed`() = testBlocking {
        val customField = CustomFieldUiModel(CUSTOM_FIELDS.first()).copy(value = "new value")
        setup()

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onCustomFieldUpdated(CUSTOM_FIELDS.first().key, customField)
            advanceUntilIdle()
        }.last()

        assertThat(state.customFields.first().value).isEqualTo("new value")
    }

    @Test
    fun `when updating a custom field twice, then make sure the last edit is the one kept`() = testBlocking {
        val customField = CustomFieldUiModel(CUSTOM_FIELDS.first())
        setup()

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onCustomFieldUpdated(CUSTOM_FIELDS.first().key, customField.copy(value = "new value"))
            viewModel.onCustomFieldUpdated(CUSTOM_FIELDS.first().key, customField.copy(value = "new value 2"))
            advanceUntilIdle()
        }.last()

        assertThat(state.customFields.first().value).isEqualTo("new value 2")
    }

    @Test
    fun `when adding a custom field, then custom fields are refreshed`() = testBlocking {
        val customField = CustomFieldUiModel(
            key = "new key",
            value = "new value"
        )
        setup()

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onCustomFieldInserted(customField)
            advanceUntilIdle()
        }.last()

        assertThat(state.customFields).hasSize(CUSTOM_FIELDS.size + 1)
        assertThat(state.customFields.last().key).isEqualTo(customField.key)
        assertThat(state.customFields.last().value).isEqualTo(customField.value)
    }

    @Test
    fun `when adding a custom field then updating it, then confirm the field is not duplicated`() = testBlocking {
        val customField = CustomFieldUiModel(
            key = "new key",
            value = "new value"
        )
        setup()

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onCustomFieldInserted(customField)
            viewModel.onCustomFieldUpdated(customField.key, customField.copy(value = "new value 2"))
            advanceUntilIdle()
        }.last()

        assertThat(state.customFields).hasSize(CUSTOM_FIELDS.size + 1)
        assertThat(state.customFields.last().key).isEqualTo(customField.key)
        assertThat(state.customFields.last().value).isEqualTo("new value 2")
    }

    @Test
    fun `when deleting a custom field, then custom fields are refreshed`() = testBlocking {
        val customField = CustomFieldUiModel(CUSTOM_FIELDS.first())
        setup()

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onCustomFieldDeleted(customField)
            advanceUntilIdle()
        }.last()

        assertThat(state.customFields).hasSize(CUSTOM_FIELDS.size - 1)
        assertThat(state.customFields).doesNotContain(customField)
    }

    @Test
    fun `when deleting a custom field, then show undo action`() = testBlocking {
        val customField = CustomFieldUiModel(CUSTOM_FIELDS.first())
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onCustomFieldDeleted(customField)
            advanceUntilIdle()
        }.last()

        assertThat(event).matches {
            it is MultiLiveEvent.Event.ShowActionSnackbar &&
                it.message == resourceProvider.getString(R.string.custom_fields_list_field_deleted) &&
                it.actionText == resourceProvider.getString(R.string.undo)
        }
    }

    @Test
    fun `when undoing a delete, then custom fields are refreshed`() = testBlocking {
        val customField = CustomFieldUiModel(CUSTOM_FIELDS.first())
        setup()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onCustomFieldDeleted(customField)
            advanceUntilIdle()
        }.last()

        val state = viewModel.state.runAndCaptureValues {
            (event as MultiLiveEvent.Event.ShowActionSnackbar).action.onClick(null)
            advanceUntilIdle()
        }.last()

        assertThat(state.customFields).hasSize(CUSTOM_FIELDS.size)
        assertThat(state.customFields).contains(customField)
    }

    @Test
    fun `given pending changes, when back button is clicked, then discard changes dialog is shown`() = testBlocking {
        setup()

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onCustomFieldInserted(CustomFieldUiModel(key = "new key", value = "new value"))
            viewModel.onBackClick()
        }.last()

        assertThat(state.discardChangesDialogState).isNotNull
    }

    @Test
    fun `given pending changes, when save is clicked, then update fields`() = testBlocking {
        val insertedField = CustomFieldUiModel(key = "new key", value = "new value")
        val updatedField = CustomFieldUiModel(CUSTOM_FIELDS.first()).copy(value = "new value")

        setup()

        viewModel.onCustomFieldUpdated(CUSTOM_FIELDS.first().key, updatedField)
        viewModel.onCustomFieldInserted(insertedField)
        viewModel.onSaveClicked()

        verify(repository).updateCustomFields(
            request = argThat {
                insertedMetadata == listOf(insertedField.toDomainModel()) &&
                    updatedMetadata == listOf(updatedField.toDomainModel())
            }
        )
    }

    @Test
    fun `given saving fails, when save is clicked, then show error message`() = testBlocking {
        setup {
            whenever(repository.updateCustomFields(any())).thenReturn(Result.failure(Exception()))
        }

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onSaveClicked()
        }.last()

        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.custom_fields_list_saving_failed))
    }

    @Test
    fun `given saving succeeds, when save is clicked, then show success message`() = testBlocking {
        setup {
            whenever(repository.updateCustomFields(any())).thenReturn(Result.success(Unit))
        }

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onSaveClicked()
        }.last()

        assertThat(event).isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.custom_fields_list_saving_succeeded))
    }

    @Test
    fun `given custom fields top banner is dismissed, when screen is opened, then banner is not shown`() = testBlocking {
        appPrefs.isCustomFieldsTopBannerDismissed = true
        setup()

        val state = viewModel.state.getOrAwaitValue()

        assertThat(state.topBannerState).isNull()
    }

    @Test
    fun `given custom fields top banner is not dismissed, when screen is opened, then banner is shown`() = testBlocking {
        appPrefs.isCustomFieldsTopBannerDismissed = false
        setup()

        val state = viewModel.state.getOrAwaitValue()

        assertThat(state.topBannerState).isNotNull
    }

    @Test
    fun `given custom fields top banner is shown, when banner is dismissed, then banner is not shown`() = testBlocking {
        appPrefs.isCustomFieldsTopBannerDismissed = false
        setup()

        val initialState = viewModel.state.getOrAwaitValue()
        val state = viewModel.state.runAndCaptureValues {
            initialState.topBannerState!!.onDismiss()
        }.last()

        assertThat(state.topBannerState).isNull()
    }
}
