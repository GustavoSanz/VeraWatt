package com.vera_watt.network

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class ConsumoDto(
    val id: Int?,
    @SerializedName("user_id") val userId: Int?,
    val watts: Int?,
    @SerializedName("solar_watts") val solarWatts: Int?,
    val timestamp: String?
)

data class ApiResponse(
    val success: Boolean,
    val message: String?
)

interface VeraWattApiService {
    @GET("2º_Ano/VeraWatt/api/get_consumos.php")
    suspend fun getConsumos(
        @Query("user_id") userId: Int
    ): List<ConsumoDto>

    @POST("2º_Ano/VeraWatt/api/save_consumo.php")
    suspend fun saveConsumo(
        @Body consumo: ConsumoDto
    ): ApiResponse
}

object RetrofitClient {
    // Substitui pelo IP do Tailscale do teu colega (ex: "http://100.101.102.103/")
    var tailscaleIp: String = "http://100.x.x.x/"

    val api: VeraWattApiService by lazy {
        Retrofit.Builder()
            .baseUrl(tailscaleIp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VeraWattApiService::class.java)
    }
}
