package org.monerokon.xmrpos.ui.settings.moneropay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.repository.DataStoreRepository
import org.monerokon.xmrpos.ui.Settings
import javax.inject.Inject

@HiltViewModel
class MoneroPayViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) : ViewModel() {

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToMainSettings() {
        navController?.navigate(Settings)
    }

    val confOptions = listOf("0-conf", "1-conf", "10-conf")

    var serverAddress: String by mutableStateOf("")

    var useCallbacks: Boolean by mutableStateOf(false)

    var refreshInterval: String by mutableStateOf("5")

    var conf: String by mutableStateOf("")

    init {
        viewModelScope.launch {
            dataStoreRepository.getMoneroPayServerAddress().collect { storedMoneroPayServerAddress ->
                println("storedMoneroPayServerAddress: $storedMoneroPayServerAddress")
                serverAddress = storedMoneroPayServerAddress
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getMoneroPayUseCallbacks().collect { storedMoneroPayUseCallbacks ->
                println("storedMoneroPayUseCallbacks: $storedMoneroPayUseCallbacks")
                useCallbacks = storedMoneroPayUseCallbacks
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getMoneroPayConfValue().collect { storedConfValue ->
                println("storedConfValue: $storedConfValue")
                conf = storedConfValue
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getMoneroPayRefreshInterval().collect { storedRefreshInterval ->
                println("storedRefreshInterval: $storedRefreshInterval")
                refreshInterval = storedRefreshInterval.toString()
            }
        }
    }

    fun updateServerAddress(newServerAddress: String) {
        serverAddress = newServerAddress
        viewModelScope.launch {
            dataStoreRepository.saveMoneroPayServerAddress(newServerAddress)
        }
    }

    fun updateUseCallbacks(newUseCallbacks: Boolean) {
        useCallbacks = newUseCallbacks
        viewModelScope.launch {
            dataStoreRepository.saveMoneroPayUseCallbacks(newUseCallbacks)
        }
    }

    fun updateRefreshInterval(newRefreshInterval: String) {
        if (newRefreshInterval.isEmpty()) {
            refreshInterval = ""
            viewModelScope.launch {
                dataStoreRepository.saveMoneroPayRefreshInterval(5)
            }
            return
        }
        if (newRefreshInterval.all { it.isDigit() }) {
            refreshInterval = newRefreshInterval
            viewModelScope.launch {
                dataStoreRepository.saveMoneroPayRefreshInterval(newRefreshInterval.toInt())
            }
        }
    }

    fun updateConf(newConf: String) {
        conf = newConf
        viewModelScope.launch {
            dataStoreRepository.saveMoneroPayConfValue(newConf)
        }
    }
}


