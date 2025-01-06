package org.monerokon.xmrpos.ui.settings.moneropay

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.data.repository.MoneroPayRepository
import org.monerokon.xmrpos.shared.DataResult
import org.monerokon.xmrpos.ui.Settings
import javax.inject.Inject

@HiltViewModel
class MoneroPayViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
    private val moneroPayRepository: MoneroPayRepository
) : ViewModel() {

    private val logTag = "MoneroPayViewModel"

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToMainSettings() {
        navController?.navigate(Settings)
    }

    val confOptions = listOf("0-conf", "1-conf", "10-conf")

    var serverAddress: String by mutableStateOf("")

    var requestInterval: String by mutableStateOf("5")

    var conf: String by mutableStateOf("")

    var healthStatus by mutableStateOf("")

    init {
        viewModelScope.launch {
            dataStoreRepository.getMoneroPayServerAddress().collect { storedMoneroPayServerAddress ->
                Log.i(logTag, "storedMoneroPayServerAddress: $storedMoneroPayServerAddress")
                serverAddress = storedMoneroPayServerAddress
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getMoneroPayConfValue().collect { storedConfValue ->
                Log.i(logTag, "storedConfValue: $storedConfValue")
                conf = storedConfValue
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getMoneroPayRequestInterval().collect { storedRequestInterval ->
                Log.i(logTag, "storedRequestInterval: $storedRequestInterval")
                requestInterval = storedRequestInterval.toString()
            }
        }
    }

    fun updateServerAddress(newServerAddress: String) {
        serverAddress = newServerAddress
        viewModelScope.launch {
            dataStoreRepository.saveMoneroPayServerAddress(newServerAddress)
        }
    }

    fun updateRequestInterval(newRequestInterval: String) {
        if (newRequestInterval.isEmpty()) {
            requestInterval = ""
            viewModelScope.launch {
                dataStoreRepository.saveMoneroPayRequestInterval(5)
            }
            return
        }
        if (newRequestInterval.all { it.isDigit() }) {
            requestInterval = newRequestInterval
            viewModelScope.launch {
                dataStoreRepository.saveMoneroPayRequestInterval(newRequestInterval.toInt())
            }
        }
    }

    fun updateConf(newConf: String) {
        conf = newConf
        viewModelScope.launch {
            dataStoreRepository.saveMoneroPayConfValue(newConf)
        }
    }

    fun fetchMoneroPayHealth() {
        viewModelScope.launch {
            val response = moneroPayRepository.fetchMoneroPayHealth()
            if (response is DataResult.Success) {
                Log.i(logTag, "MoneroPay health: ${response.data}")
                healthStatus = response.data.toString()
            } else if (response is DataResult.Failure) {
                Log.e(logTag, "MoneroPay health: ${response.message}")
                healthStatus = response.message
            }
        }
    }

    fun resetHealthStatus() {
        healthStatus = ""
    }
}


