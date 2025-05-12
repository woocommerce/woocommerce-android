package org.wordpress.android.fluxc.network

interface PaginatedResponse<T> {
    val items: Array<T>
    val totalCount: Int?
    val pageCount: Int?
}
