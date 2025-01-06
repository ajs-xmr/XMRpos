package org.monerokon.xmrpos.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.monerokon.xmrpos.data.local.room.model.Transaction
import org.monerokon.xmrpos.data.remote.moneroPay.MoneroPayRemoteDataSource
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveRequest
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveResponse
import org.monerokon.xmrpos.data.remote.moneroPayCallback.MoneroPayCallbackManager
import org.monerokon.xmrpos.data.remote.moneroPayCallback.model.PaymentCallback
import org.monerokon.xmrpos.data.remote.moneroPayCallback.model.PaymentCallbackAmount
import org.monerokon.xmrpos.data.remote.moneroPayCallback.model.PaymentCallbackCovered
import org.monerokon.xmrpos.data.remote.moneroPayCallback.model.PaymentCallbackTransaction
import org.monerokon.xmrpos.shared.DataResult

class MoneroPayRepository(
    private val moneroPayRemoteDataSource: MoneroPayRemoteDataSource,
    private val callbackManager: MoneroPayCallbackManager,
    private val transactionRepository: TransactionRepository,
    private val dataStoreRepository: DataStoreRepository
) {

    private val _paymentStatus = MutableStateFlow<PaymentCallback?>(null)
    val paymentStatus: StateFlow<PaymentCallback?> = _paymentStatus
    var currentCallbackUUID: String? = null
    var currentFiatValue: Double? = null
    var currentAddress: String? = null

    suspend fun startReceive(moneroPayReceiveRequest: MoneroPayReceiveRequest): DataResult<MoneroPayReceiveResponse> {
        return withContext(Dispatchers.IO) {
            val response = moneroPayRemoteDataSource.startReceive(moneroPayReceiveRequest)

            if (response is DataResult.Success) {
                /*callbackManager.startListening { paymentCallback, fiatValue, callbackUUID ->
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        handlePaymentCallback(paymentCallback, fiatValue, callbackUUID)
                    }
                }*/
                currentAddress = response.data.address
                checkPeriodicPaymentStatus()
            }
            response
        }
    }

    private fun checkPeriodicPaymentStatus() {
        val initialCallbackUUID = currentCallbackUUID
        val currentFiatValue = currentFiatValue
        val currentAddress = currentAddress
        if (initialCallbackUUID != null && currentFiatValue != null && currentAddress != null) {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                while (currentCallbackUUID == initialCallbackUUID) {
                    Log.i("MoneroPayRepository", "Checking payment status")
                    val response = moneroPayRemoteDataSource.fetchReceiveStatus(currentAddress)
                    Log.i("MoneroPayRepository", response.toString())
                    if (response is DataResult.Success) {
                        if (response.data.transactions == null || response.data.transactions.isEmpty()) {
                            kotlinx.coroutines.delay(10000)
                            continue
                        }
                        handlePaymentCallback(
                            PaymentCallback(
                                amount = PaymentCallbackAmount(
                                    expected = response.data.amount.expected,
                                    covered = PaymentCallbackCovered(
                                        total = response.data.amount.covered.total,
                                        unlocked = response.data.amount.covered.unlocked
                                    )
                                ),
                                complete = response.data.complete,
                                description = response.data.description,
                                created_at = response.data.created_at,
                                transaction = PaymentCallbackTransaction(
                                    amount = response.data.transactions.last().amount,
                                    confirmations = response.data.transactions.last().confirmations,
                                    double_spend_seen = response.data.transactions.last().double_spend_seen,
                                    fee = response.data.transactions.last().fee,
                                    height = response.data.transactions.last().height,
                                    timestamp = response.data.transactions.last().timestamp,
                                    tx_hash = response.data.transactions.last().tx_hash,
                                    unlock_time = response.data.transactions.last().unlock_time,
                                    locked = response.data.transactions.last().locked
                                )
                            ),
                            currentFiatValue,
                            initialCallbackUUID
                        )
                    }
                    kotlinx.coroutines.delay(10000) // Delay for 10 seconds
                }
            }
        }
    }

    fun updateCurrentCallback(callbackUUID: String?, fiatValue: Double?) {
        currentCallbackUUID = callbackUUID
        currentFiatValue = fiatValue
    }

    private suspend fun handlePaymentCallback(paymentCallback: PaymentCallback, fiatValue: Double, callbackUUID: String) {
        if (currentCallbackUUID != callbackUUID) {
            return
        }

        val confValue = dataStoreRepository.getMoneroPayConfValue().first()
        if (confValue == "0-conf" && paymentCallback.transaction.confirmations < 0) {
            Log.i("MoneroPayRepository", "0-conf: Transaction has negative confirmations")
            return
        } else if (confValue == "1-conf" && paymentCallback.transaction.confirmations < 1) {
            Log.i("MoneroPayRepository", "1-conf: Transaction has less than 1 confirmation")
            return
        } else if (confValue == "10-conf" && paymentCallback.transaction.confirmations < 10) {
            Log.i("MoneroPayRepository", "10-conf: Transaction has less than 10 confirmations")
            return
        }

        if (paymentCallback.amount.covered.total >= paymentCallback.amount.expected) {
            currentCallbackUUID = null
            currentFiatValue = null
            currentAddress = null

            _paymentStatus.value = paymentCallback

            transactionRepository.insertTransaction(
                Transaction(
                    txId = paymentCallback.transaction.tx_hash,
                    xmrAmount = paymentCallback.amount.covered.total,
                    fiatValue = fiatValue,
                    timestamp = paymentCallback.transaction.timestamp
                )
            )
        }
    }


    fun stopReceive() {
        callbackManager.stopListening()
    }
}