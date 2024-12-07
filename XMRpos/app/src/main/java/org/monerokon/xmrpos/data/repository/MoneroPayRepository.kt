package org.monerokon.xmrpos.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.monerokon.xmrpos.data.local.room.model.Transaction
import org.monerokon.xmrpos.data.remote.moneroPay.MoneroPayRemoteDataSource
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveRequest
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveResponse
import org.monerokon.xmrpos.data.remote.moneroPayCallback.MoneroPayCallbackManager
import org.monerokon.xmrpos.data.remote.moneroPayCallback.model.PaymentCallback

class MoneroPayRepository(
    private val moneroPayRemoteDataSource: MoneroPayRemoteDataSource,
    private val callbackManager: MoneroPayCallbackManager,
    private val transactionRepository: TransactionRepository,
) {

    private val _paymentStatus = MutableStateFlow<PaymentCallback?>(null)
    val paymentStatus: StateFlow<PaymentCallback?> = _paymentStatus

    suspend fun startReceive(moneroPayReceiveRequest: MoneroPayReceiveRequest): MoneroPayReceiveResponse? {
        return withContext(Dispatchers.IO) {
            val response = moneroPayRemoteDataSource.startReceive(moneroPayReceiveRequest)
            response?.let {
                callbackManager.startListening { paymentCallback, fiatValue ->
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        handlePaymentCallback(paymentCallback, fiatValue)
                    }
                }
            }
            response
        }
    }

    private suspend fun handlePaymentCallback(paymentCallback: PaymentCallback, fiatValue: Double) {
        if (paymentCallback.amount.expected == paymentCallback.amount.covered.total) {
            _paymentStatus.value = paymentCallback

            transactionRepository.insertTransaction(
                Transaction(
                    1, paymentCallback.transaction.tx_hash, paymentCallback.amount.covered.total, fiatValue, paymentCallback.transaction.timestamp))
        }
    }

    fun stopReceive() {
        callbackManager.stopListening()
    }
}