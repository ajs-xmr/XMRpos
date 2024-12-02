package org.monerokon.xmrpos.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.monerokon.xmrpos.data.remote.moneroPay.MoneroPayRemoteDataSource
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveRequest
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveResponse
import org.monerokon.xmrpos.data.remote.moneroPayCallback.MoneroPayCallbackManager
import org.monerokon.xmrpos.data.remote.moneroPayCallback.model.PaymentCallback

class MoneroPayRepository(
    private val moneroPayRemoteDataSource: MoneroPayRemoteDataSource,
    private val callbackManager: MoneroPayCallbackManager
) {

    private val _paymentStatus = MutableStateFlow<PaymentCallback?>(null)
    val paymentStatus: StateFlow<PaymentCallback?> = _paymentStatus

    suspend fun startReceive(moneroPayReceiveRequest: MoneroPayReceiveRequest): MoneroPayReceiveResponse? {
        return withContext(Dispatchers.IO) {
            val response = moneroPayRemoteDataSource.startReceive(moneroPayReceiveRequest)
            response?.let {
                callbackManager.startListening { paymentCallback ->
                    handlePaymentCallback(paymentCallback)
                }
            }
            response
        }
    }

    private fun handlePaymentCallback(paymentCallback: PaymentCallback) {
        if (paymentCallback.amount.expected == paymentCallback.amount.covered.total) {
            _paymentStatus.value = paymentCallback
        }
    }

    fun stopReceive() {
        callbackManager.stopListening()
    }
}