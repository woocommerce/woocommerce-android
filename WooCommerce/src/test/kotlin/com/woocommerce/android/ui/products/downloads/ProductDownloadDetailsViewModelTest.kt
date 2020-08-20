package com.woocommerce.android.ui.products.downloads

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.woocommerce.android.R
import com.woocommerce.android.model.ProductFile
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsViewModel.ProductDownloadDetailsEvent.DeleteFileEvent
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsViewModel.ProductDownloadDetailsEvent.UpdateFileAndExitEvent
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsViewModel.ProductDownloadDetailsViewState
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ProductDownloadDetailsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ProductDownloadDetailsViewModel
    private val file = ProductFile(id = "id", name = "file", url = "url")
    private val savedStateForEditing = SavedStateWithArgs(
        SavedStateHandle(),
        null,
        ProductDownloadDetailsFragmentArgs(file)
    )

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val resourceProvider: ResourceProvider = mock() {
        on { getString(R.string.product_downloadable_files_edit_title) } doReturn "file"
    }

    @Test
    fun `test has the correct init state`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
            coroutinesTestRule.testDispatchers,
            resourceProvider
        )

        var state: ProductDownloadDetailsViewState? = null
        viewModel.productDownloadDetailsViewStateData.observeForever { _, new -> state = new }

        assertThat(state!!.fileDraft).isEqualTo(file)
        assertThat(state!!.hasChanges).isEqualTo(false)
    }

    @Test
    fun `test display the correct title if name is empty`() {
        val file = file.copy(name = "")
        val savedStateWithArgs = SavedStateWithArgs(
            SavedStateHandle(),
            null,
            ProductDownloadDetailsFragmentArgs(file)
        )
        viewModel = ProductDownloadDetailsViewModel(
            savedStateWithArgs,
            coroutinesTestRule.testDispatchers,
            resourceProvider
        )

        val title = viewModel.screenTitle

        assertThat(title).isEqualTo("file")
    }

    @Test
    fun `test file name edit`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
            coroutinesTestRule.testDispatchers,
            resourceProvider
        )

        val newName = "new name"
        viewModel.onFileNameChanged(newName)

        var state: ProductDownloadDetailsViewState? = null
        viewModel.productDownloadDetailsViewStateData.observeForever { _, new -> state = new }

        assertThat(state!!.fileDraft.name).isEqualTo(newName)
        assertThat(state!!.hasChanges).isEqualTo(true)
    }

    @Test
    fun `test file url edit`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
            coroutinesTestRule.testDispatchers,
            resourceProvider
        )

        val newUrl = "new url"
        viewModel.onFileUrlChanged(newUrl)

        var state: ProductDownloadDetailsViewState? = null
        viewModel.productDownloadDetailsViewStateData.observeForever { _, new -> state = new }

        assertThat(state!!.fileDraft.url).isEqualTo(newUrl)
        assertThat(state!!.hasChanges).isEqualTo(true)
    }

    @Test
    fun `test dispatch update event`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
            coroutinesTestRule.testDispatchers,
            resourceProvider
        )

        val newUrl = "new url"
        val newName = "new name"
        viewModel.onFileNameChanged(newName)
        viewModel.onFileUrlChanged(newUrl)

        var event: Event? = null
        viewModel.event.observeForever { new -> event = new }
        viewModel.onDoneOrUpdateClicked()

        assertThat(event).isInstanceOf(UpdateFileAndExitEvent::class.java)
        assertEquals(newName, (event as UpdateFileAndExitEvent).updatedFile.name)
        assertEquals(newUrl, (event as UpdateFileAndExitEvent).updatedFile.url)
    }

    @Test
    fun `test delete file`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
            coroutinesTestRule.testDispatchers,
            resourceProvider
        )

        val events = mutableListOf<Event>()
        viewModel.event.observeForever { new -> events.add(new) }
        viewModel.onDeleteButtonClicked()
        viewModel.triggerFileDeletion()

        assertThat(events[0]).isInstanceOf(ShowDialog::class.java)
        assertThat(events[1]).isInstanceOf(DeleteFileEvent::class.java)
        assertThat((events[1] as DeleteFileEvent).file).isEqualTo(file)
    }
}
