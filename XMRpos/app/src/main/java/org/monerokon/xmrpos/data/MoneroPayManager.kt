// MoneroPayManager.kt
package org.monerokon.xmrpos.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

class MoneroPayManager(private val moneroPayServerAddress: String) {

    private val BASE_URL = moneroPayServerAddress

    private val api: MoneroPayApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MoneroPayApi::class.java)
    }

    // Fetch exchange rates and return as a map
    suspend fun startReceive(amount: Long, description: String, callbackUrl: String?): StartReceiveResponse? {
        return try {
            println("MoneroPay: started")
            api.startReceive(StartReceiveData(amount, description, callbackUrl))
        } catch (e: Exception) {
            println("MoneroPay: failed")
            println("MoneroPay: $e")
            null
        }
    }

    suspend fun fetchReceiveStatus(address: String): FetchReceiveStatusResponse? {
        return try {
            println("MoneroPay: fetching status")
            api.fetchReceiveStatus(address)
        } catch (e: Exception) {
            println("MoneroPay: failed")
            println("MoneroPay: $e")
            null
        }
    }
}

data class StartReceiveData(
    val amount: Long,
    val description: String,
    val callback_url: String?,
)

data class StartReceiveResponse(
    val address: String,
    val amount: Long,
    val description: String,
    val created_at: String,
)


data class FetchReceiveStatusResponse(
    val amount: Amount,
    val complete: Boolean,
    val description: String,
    val createdAt: String,
    val transactions: List<Transaction>
)

data class Amount(
    val expected: Long,
    val covered: Covered
)

data class Covered(
    val total: Long,
    val unlocked: Long
)

data class Transaction(
    val amount: Long,
    val confirmations: Int,
    val doubleSpendSeen: Boolean,
    val fee: Long,
    val height: Int,
    val timestamp: String,
    val txHash: String,
    val unlockTime: Int,
    val locked: Boolean
)


interface MoneroPayApi {
    @POST("receive")
    suspend fun startReceive(
        @Body requestData: StartReceiveData
    ): StartReceiveResponse

    @GET("receive/{address}")
    suspend fun fetchReceiveStatus(
        @Path("address") address: String
    ): FetchReceiveStatusResponse
}