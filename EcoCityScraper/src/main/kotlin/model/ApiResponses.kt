package model

data class Measurement(
    val name: String,
    val value: Double,
    val time: String,
) : ApiResponseRecord

data class Owner(
    val provider: String,
) : ApiResponseRecord

interface ApiResponseRecord

data class ApiResponse(
    val records : List<ApiResponseRecord>
)