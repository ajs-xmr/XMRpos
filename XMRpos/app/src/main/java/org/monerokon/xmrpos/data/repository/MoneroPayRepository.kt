package org.monerokon.xmrpos.data.repository

import org.monerokon.xmrpos.data.local.datastore.DataStoreLocalDataSource
import org.monerokon.xmrpos.data.remote.moneroPay.MoneroPayRemoteDataSource
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveRequest
import org.monerokon.xmrpos.data.remote.moneroPay.model.MoneroPayReceiveResponse
import javax.inject.Inject

class MoneroPayRepository @Inject constructor(
    private val moneroPayRemoteDataSource: MoneroPayRemoteDataSource,
    private val DataStoreLocalDataSource: DataStoreLocalDataSource // Or DataStoreLocalDataSource
) {

    // make receive request to MoneroPay API
    suspend fun startReceive(moneroPayReceiveRequest: MoneroPayReceiveRequest): MoneroPayReceiveResponse {
        return moneroPayRemoteDataSource.startReceive(moneroPayReceiveRequest)
    }

}
