package com.woocommerce.android.helpers

import androidx.paging.PagedList
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

/**
 * Converts a list to a [PagedList] for testing
 */
fun <T> mockPagedList(list: List<T>): PagedList<T> {
    val pagedList = Mockito.mock(PagedList::class.java) as PagedList<T>
    Mockito.`when`(pagedList.get(ArgumentMatchers.anyInt())).then { invocation ->
        val index = invocation.arguments.first() as Int
        list[index]
    }
    Mockito.`when`(pagedList.size).thenReturn(list.size)
    return pagedList
}
