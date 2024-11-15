package org.monerokon.xmrpos.ui.settings.moneropay

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.DataStoreManager
import org.monerokon.xmrpos.ui.Settings

class MoneroPayViewModel (application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

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
            dataStoreManager.getString(DataStoreManager.MONERO_PAY_SERVER_ADDRESS).collect { storedMoneroPayServerAddress ->
                println("storedMoneroPayServerAddress: $storedMoneroPayServerAddress")
                serverAddress = storedMoneroPayServerAddress?: ""
            }
        }
        viewModelScope.launch {
            dataStoreManager.getBoolean(DataStoreManager.MONERO_PAY_USE_CALLBACKS).collect { storedMoneroPayUseCallbacks ->
                println("storedMoneroPayUseCallbacks: $storedMoneroPayUseCallbacks")
                useCallbacks = storedMoneroPayUseCallbacks?: false
            }
        }
        viewModelScope.launch {
            dataStoreManager.getString(DataStoreManager.MONERO_PAY_CONF_VALUE).collect { storedConfValue ->
                println("storedConfValue: $storedConfValue")
                conf = storedConfValue?: ""
            }
        }
        viewModelScope.launch {
            dataStoreManager.getInt(DataStoreManager.MONERO_PAY_REFRESH_INTERVAL)
                .collect { storedRefreshInterval ->
                    println("storedRefreshInterval: $storedRefreshInterval")
                    refreshInterval = storedRefreshInterval.toString()
                }
        }
    }

    fun updateServerAddress(newServerAddress: String) {
        serverAddress = newServerAddress
        viewModelScope.launch {
            dataStoreManager.saveString(DataStoreManager.MONERO_PAY_SERVER_ADDRESS, newServerAddress)
        }
    }

    fun updateUseCallbacks(newUseCallbacks: Boolean) {
        useCallbacks = newUseCallbacks
        viewModelScope.launch {
            dataStoreManager.saveBoolean(DataStoreManager.MONERO_PAY_USE_CALLBACKS, newUseCallbacks)
        }
    }

    fun updateRefreshInterval(newRefreshInterval: String) {
        if (newRefreshInterval.isEmpty()) {
            refreshInterval = ""
            viewModelScope.launch {
                dataStoreManager.saveInt(DataStoreManager.MONERO_PAY_REFRESH_INTERVAL, 5)
            }
            return
        }
        if (newRefreshInterval.all { it.isDigit() }) {
            refreshInterval = newRefreshInterval
            viewModelScope.launch {
                dataStoreManager.saveInt(DataStoreManager.MONERO_PAY_REFRESH_INTERVAL, newRefreshInterval.toInt())
            }
        }
    }

    fun updateConf(newConf: String) {
        conf = newConf
        viewModelScope.launch {
            dataStoreManager.saveString(DataStoreManager.MONERO_PAY_CONF_VALUE, newConf)
        }
    }
}


