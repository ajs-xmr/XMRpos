package org.monerokon.xmrpos.data.remote.backend.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackendTransactionHistoryResponse(
    @SerialName("confirmed_transactions")
    val confirmedTransactions: List<BackendConfirmedTransaction> = emptyList(),
    @SerialName("pending_transactions")
    val pendingTransactions: List<BackendPendingTransaction> = emptyList(),
)

@Serializable
data class BackendConfirmedTransaction(
    @SerialName("transaction_id")
    val transactionId: Int,
    @SerialName("tx_hash")
    val txHash: String,
    val timestamp: String,
    val height: Long,
    val accepted: Boolean,
    val confirmed: Boolean,
)

@Serializable
data class BackendPendingTransaction(
    val id: Int,
    val amount: Long,
    val accepted: Boolean,
    val confirmed: Boolean,
)
