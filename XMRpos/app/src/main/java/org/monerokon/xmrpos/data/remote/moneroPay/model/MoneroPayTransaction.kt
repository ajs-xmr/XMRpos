package org.monerokon.xmrpos.data.remote.moneroPay.model

data class MoneroPayTransaction(
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
