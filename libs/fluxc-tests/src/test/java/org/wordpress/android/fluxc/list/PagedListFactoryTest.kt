package org.wordpress.android.fluxc.list

import androidx.paging.DataSource.InvalidatedCallback
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.list.PagedListFactory

internal class PagedListFactoryTest {
    @Test
    fun `create factory triggers create data source`() {
        val mockCreateDataSource = mock<() -> TestInternalPagedListDataSource>()
        whenever(mockCreateDataSource.invoke()).thenReturn(mock())
        val pagedListFactory = PagedListFactory(mockCreateDataSource)

        pagedListFactory.create()

        verify(mockCreateDataSource, times(1)).invoke()
    }

    @Test
    fun `invalidate triggers create data source`() {
        val mockCreateDataSource = mock<() -> TestInternalPagedListDataSource>()
        whenever(mockCreateDataSource.invoke()).thenReturn(mock())
        val invalidatedCallback = mock<InvalidatedCallback>()

        val pagedListFactory = PagedListFactory(mockCreateDataSource)
        val currentSource = pagedListFactory.create()
        currentSource.addInvalidatedCallback(invalidatedCallback)

        pagedListFactory.invalidate()

        verify(invalidatedCallback, times(1)).onInvalidated()
    }

    @Test
    fun `given invalidate while a data source is created, when create returns, then the new source is invalid`() {
        lateinit var pagedListFactory: PagedListFactory<TestListDescriptor, TestListIdentifier, TestPagedListResultType>
        val createDataSource = mock<() -> TestInternalPagedListDataSource>()
        whenever(createDataSource.invoke()).thenAnswer {
            pagedListFactory.invalidate()
            mock<TestInternalPagedListDataSource>()
        }
        pagedListFactory = PagedListFactory(createDataSource)

        val source = pagedListFactory.create()

        assertThat(source.isInvalid).isTrue()
    }
}
