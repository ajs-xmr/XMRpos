package org.monerokon.xmrpos.data.remote.moneroPay.model

data class MoneroPayHealthResponse(
    val status: Int,
    val services: MoneroPayHealthServices
)