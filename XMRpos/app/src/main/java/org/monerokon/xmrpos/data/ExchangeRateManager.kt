// DataStoreManager.kt
package org.monerokon.xmrpos.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.text.get

// on first run, populate the DataStore with some default values
typealias ExchangeRateResponse = Map<String, Double>

object ExchangeRateManager {
    private val BASE_URL = "https://min-api.cryptocompare.com/data/"

    private val api: ExchangeRateApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExchangeRateApi::class.java)
    }

    // Fetch exchange rates and return as a map
    suspend fun fetchExchangeRates(fromSymbol: String, toSymbols: List<String>): Map<String, Double>? {
        val symbols = toSymbols.joinToString(",")  // Convert list to comma-separated string
        return try {
            api.getExchangeRates(fromSymbol, symbols)
        } catch (e: Exception) {
            null
        }
    }
}

interface ExchangeRateApi {
    @GET("price")
    suspend fun getExchangeRates(
        @Query("fsym") fromSymbol: String,
        @Query("tsyms") toSymbols: String
    ): ExchangeRateResponse
}