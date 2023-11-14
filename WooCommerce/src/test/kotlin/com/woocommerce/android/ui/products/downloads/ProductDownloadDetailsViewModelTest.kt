package com.woocommerce.android.ui.products.downloads

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.ProductFile
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsViewModel.ProductDownloadDetailsEvent.*
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsViewModel.ProductDownloadDetailsViewState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ProductDownloadDetailsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ProductDownloadDetailsViewModel
    private val file = ProductFile(id = "id", name = "file", url = "url")
    private val savedStateForEditing = ProductDownloadDetailsFragmentArgs(isEditing = true, productFile = file)
        .initSavedStateHandle()
    private val savedStateForAdding = ProductDownloadDetailsFragmentArgs(
        isEditing = false,
        productFile = file.copy(id = null)
    )
        .initSavedStateHandle()

    private val resourceProvider: ResourceProvider = mock {
        on { getString(R.string.product_downloadable_files_edit_title) } doReturn "file"
    }

    @Test
    fun `test has the correct init state`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
            resourceProvider
        )

        var state: ProductDownloadDetailsViewState? = null
        viewModel.productDownloadDetailsViewStateData.observeForever { _, new -> state = new }

        assertThat(state!!.fileDraft).isEqualTo(file)
        assertThat(state!!.showDoneButton).isEqualTo(false)
    }

    @Test
    fun `test display the correct title if name is empty`() {
        val file = file.copy(name = "")
        val savedState = ProductDownloadDetailsFragmentArgs(isEditing = true, productFile = file)
            .initSavedStateHandle()
        viewModel = ProductDownloadDetailsViewModel(
            savedState,
            resourceProvider
        )

        val title = viewModel.screenTitle

        assertThat(title).isEqualTo("file")
    }

    @Test
    fun `test file name edit`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
            resourceProvider
        )

        val newName = "new name"
        viewModel.onFileNameChanged(newName)

        var state: ProductDownloadDetailsViewState? = null
        viewModel.productDownloadDetailsViewStateData.observeForever { _, new -> state = new }

        assertThat(state!!.fileDraft.name).isEqualTo(newName)
        assertThat(state!!.showDoneButton).isEqualTo(true)
    }

    @Test
    fun `test file url edit`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
            resourceProvider
        )

        val newUrl = "new url"
        viewModel.onFileUrlChanged(newUrl)

        var state: ProductDownloadDetailsViewState? = null
        viewModel.productDownloadDetailsViewStateData.observeForever { _, new -> state = new }

        assertThat(state!!.fileDraft.url).isEqualTo(newUrl)
        assertThat(state!!.showDoneButton).isEqualTo(true)
    }

    @Test
    fun `test dispatch update event when editing`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
            resourceProvider
        )

        val newUrl = "http://url.com"
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
    fun `test dispatch add event when adding file`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForAdding,
            resourceProvider
        )

        val newUrl = "http://url.com"
        val newName = "new name"
        viewModel.onFileNameChanged(newName)
        viewModel.onFileUrlChanged(newUrl)

        var event: Event? = null
        viewModel.event.observeForever { new -> event = new }
        viewModel.onDoneOrUpdateClicked()

        assertThat(event).isInstanceOf(AddFileAndExitEvent::class.java)
        assertEquals(newName, (event as AddFileAndExitEvent).file.name)
        assertEquals(newUrl, (event as AddFileAndExitEvent).file.url)
    }

    @Test
    fun `test delete file`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
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

    @Test
    fun `test field validation when url empty`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
            resourceProvider
        )

        viewModel.onFileUrlChanged("")

        var state: ProductDownloadDetailsViewState? = null
        viewModel.productDownloadDetailsViewStateData.observeForever { _, new -> state = new }

        assertThat(state!!.urlErrorMessage).isEqualTo(R.string.product_downloadable_files_url_invalid)
        assertThat(state!!.nameErrorMessage).isNull()
    }

    @Test
    fun `test field validation when url invalid`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
            resourceProvider
        )

        viewModel.onFileUrlChanged("invalid_url")

        var state: ProductDownloadDetailsViewState? = null
        viewModel.productDownloadDetailsViewStateData.observeForever { _, new -> state = new }

        assertThat(state!!.urlErrorMessage).isEqualTo(R.string.product_downloadable_files_url_invalid)
        assertThat(state!!.nameErrorMessage).isNull()
    }

    @Test
    fun `test field validation when url without path and name empty`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
            resourceProvider
        )

        viewModel.onFileUrlChanged("http://testurl.com/")
        viewModel.onFileNameChanged("")

        var state: ProductDownloadDetailsViewState? = null
        viewModel.productDownloadDetailsViewStateData.observeForever { _, new -> state = new }

        assertThat(state!!.urlErrorMessage).isNull()
        assertThat(state!!.nameErrorMessage).isEqualTo(R.string.product_downloadable_files_name_invalid)
    }

    @Test
    fun `test field validation when url with path and name empty`() {
        viewModel = ProductDownloadDetailsViewModel(
            savedStateForEditing,
            resourceProvider
        )

        viewModel.onFileUrlChanged("http://testurl.com/path/file.jpg")
        viewModel.onFileNameChanged("")

        var state: ProductDownloadDetailsViewState? = null
        viewModel.productDownloadDetailsViewStateData.observeForever { _, new -> state = new }

        assertThat(state!!.urlErrorMessage).isNull()
        assertThat(state!!.nameErrorMessage).isNull()
    }
}
