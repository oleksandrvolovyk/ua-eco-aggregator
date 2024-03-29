package ua.eco.aggregator.base.model

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedData<T: AggregatedRecord>(
    val page: Long,
    val maxPageNumber: Long,
    val itemsPerPage: Int,
    val totalItemsCount: Long,
    val data: List<T>
)