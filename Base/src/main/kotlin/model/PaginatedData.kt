package model

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedData<T>(
    val page: Long,
    val maxPageNumber: Long,
    val itemsPerPage: Int,
    val totalItemsCount: Long,
    val data: List<T>
)