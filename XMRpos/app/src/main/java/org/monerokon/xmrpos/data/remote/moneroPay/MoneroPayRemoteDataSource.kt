package org.monerokon.xmrpos.data.remote.moneroPay

import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveRequest
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveResponse
import org.monerokon.xmrpos.shared.DataResult
import javax.inject.Inject

class MoneroPayRemoteDataSource @Inject constructor(
    private val api: MoneroPayApi
) {
    suspend fun startReceive(moneroPayReceiveRequest: MoneroPayReceiveRequest): DataResult<MoneroPayReceiveResponse> {
        return try {
            val response = api.startReceive(moneroPayReceiveRequest)
            DataResult.Success(response)
        } catch (e: Exception) {
            DataResult.Failure(message = e.message ?: "Unknown error")
        }
    }
}