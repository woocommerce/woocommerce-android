package com.woocommerce.android.ui.customfields.list

import com.woocommerce.android.R
import com.woocommerce.android.ui.customfields.CustomField
import com.woocommerce.android.ui.customfields.CustomFieldContentType
import com.woocommerce.android.ui.customfields.CustomFieldUiModel
import com.woocommerce.android.ui.customfields.CustomFieldsRepository
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
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
    private lateinit var viewModel: CustomFieldsViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = CustomFieldsViewModel(
            savedStateHandle = CustomFieldsFragmentArgs(PARENT_ITEM_ID, PARENT_ITEM_TYPE).toSavedStateHandle(),
            repository = repository
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
        assertThat(states.first().isLoading).isTrue()
        assertThat(states.last().isLoading).isFalse()
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
}
