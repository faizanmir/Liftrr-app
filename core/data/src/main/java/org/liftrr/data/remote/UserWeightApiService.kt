package org.liftrr.data.remote

import org.liftrr.data.remote.dto.weight.BulkWeightUpsertRequest
import org.liftrr.data.remote.dto.weight.WeightEntryResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface UserWeightApiService {
    companion object {
        private const val API_PREFIX = "/api/v1"
    }

    @GET("${API_PREFIX}/weights")
    suspend fun listWeights(): List<WeightEntryResponse>

    @POST("${API_PREFIX}/weights/bulk")
    suspend fun bulkUpsert(@Body request: BulkWeightUpsertRequest): List<WeightEntryResponse>
}
