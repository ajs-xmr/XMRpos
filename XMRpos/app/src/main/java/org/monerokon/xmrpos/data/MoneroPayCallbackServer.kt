// MoneroPayCallbackServer.kt
package org.monerokon.xmrpos.data

import android.os.Handler
import android.os.Looper
import fi.iki.elonen.NanoHTTPD
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MoneroPayCallbackServer(port: Int, private val onPaymentReceived: (PaymentCallback) -> Unit) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        return if (session.method == Method.POST) {
            val bodyData = mutableMapOf<String, String>()
            session.parseBody(bodyData)
            val callbackData = bodyData["postData"] ?: return newFixedLengthResponse("No callback data")

            processPaymentEvent(callbackData)

            return newFixedLengthResponse("Callback processed successfully")
        } else {
            return newFixedLengthResponse("Invalid request method")
        }
    }

    private fun processPaymentEvent(data: String) {
        try {
            val paymentCallback = Json.decodeFromString<PaymentCallback>(data)
            println("Received payment: ${paymentCallback.transaction.amount}")
            println("Description: ${paymentCallback.description}")
            Handler(Looper.getMainLooper()).post {
                onPaymentReceived(paymentCallback)
            }
        } catch (e: Exception) {
            println("Failed to process callback: ${e.message}")
        }
    }
}

@Serializable
data class PaymentCallback(
    val amount: PaymentCallbackAmount,
    val complete: Boolean,
    val description: String,
    val created_at: String,
    val transaction: PaymentCallbackTransaction
)

@Serializable
data class PaymentCallbackAmount(
    val expected: Long,
    val covered: PaymentCallbackCovered
)

@Serializable
data class PaymentCallbackCovered(
    val total: Long,
    val unlocked: Long
)

@Serializable
data class PaymentCallbackTransaction(
    val amount: Long,
    val confirmations: Int,
    val double_spend_seen: Boolean,
    val fee: Long,
    val height: Int,
    val timestamp: String,
    val tx_hash: String,
    val unlock_time: Int,
    val locked: Boolean
)