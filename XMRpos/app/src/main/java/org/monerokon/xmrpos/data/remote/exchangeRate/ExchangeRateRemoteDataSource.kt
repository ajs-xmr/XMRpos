package org.monerokon.xmrpos.data.remote.exchangeRate

import org.monerokon.xmrpos.data.remote.exchangeRate.model.ExchangeRateResponse
import org.monerokon.xmrpos.shared.DataResult
import javax.inject.Inject

class ExchangeRateRemoteDataSource @Inject constructor(
    private val api: ExchangeRateApi
) {
    suspend fun fetchExchangeRates(fromSymbol: String, toSymbols: List<String>): DataResult<ExchangeRateResponse> {
        return try {
            val response = api.fetchExchangeRates(fromSymbol, toSymbols.joinToString(","))
            DataResult.Success(response)
        } catch (e: Exception) {
            DataResult.Failure(message = e.message ?: "Unknown error")
        }
    }
}