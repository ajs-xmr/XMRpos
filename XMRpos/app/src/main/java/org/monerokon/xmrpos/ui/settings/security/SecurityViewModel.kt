package org.monerokon.xmrpos.ui.settings.moneropay

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.DataStoreManager
import org.monerokon.xmrpos.ui.Settings

class SecurityViewModel (application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToMainSettings() {
        navController?.navigate(Settings)
    }

    var requirePinCodeOnAppStart by mutableStateOf(false)
    var requirePinCodeOpenSettings by mutableStateOf(false)
    var pinCodeOnAppStart by mutableStateOf("")
    var pinCodeOpenSettings by mutableStateOf("")

    init {
        viewModelScope.launch {
            dataStoreManager.getBoolean(DataStoreManager.REQUIRE_PIN_CODE_ON_APP_START).collect { storedRequirePinCodeOnAppStart  ->
                println("storedRequirePinCodeOnAppStart: $storedRequirePinCodeOnAppStart")
                requirePinCodeOnAppStart = storedRequirePinCodeOnAppStart?: false
            }
        }
        viewModelScope.launch {
            dataStoreManager.getBoolean(DataStoreManager.REQUIRE_PIN_CODE_OPEN_SETTINGS).collect { storedRequirePinCodeOpenSettings  ->
                println("storedRequirePinCodeOpenSettings: $storedRequirePinCodeOpenSettings")
                requirePinCodeOpenSettings = storedRequirePinCodeOpenSettings?: false
            }
        }
        viewModelScope.launch {
            dataStoreManager.getString(DataStoreManager.PIN_CODE_ON_APP_START).collect { storedPinCodeOnAppStart  ->
                println("storedPinCodeOnAppStart: $storedPinCodeOnAppStart")
                pinCodeOnAppStart = storedPinCodeOnAppStart?: ""
            }
        }
        viewModelScope.launch {
            dataStoreManager.getString(DataStoreManager.PIN_CODE_OPEN_SETTINGS).collect { storedPinCodeOpenSettings  ->
                println("storedPinCodeOpenSettings: $storedPinCodeOpenSettings")
                pinCodeOpenSettings = storedPinCodeOpenSettings?: ""
            }
        }
    }

    fun updateRequirePinCodeOnAppStart(newRequirePinCodeOnAppStart: Boolean) {
        requirePinCodeOnAppStart = newRequirePinCodeOnAppStart
        viewModelScope.launch {
            dataStoreManager.saveBoolean(DataStoreManager.REQUIRE_PIN_CODE_ON_APP_START, newRequirePinCodeOnAppStart)
        }
    }

    fun updateRequirePinCodeOpenSettings(newRequirePinCodeOpenSettings: Boolean) {
        requirePinCodeOpenSettings = newRequirePinCodeOpenSettings
        viewModelScope.launch {
            dataStoreManager.saveBoolean(DataStoreManager.REQUIRE_PIN_CODE_OPEN_SETTINGS, newRequirePinCodeOpenSettings)
        }
    }

    fun updatePinCodeOnAppStart(newPinCodeOnAppStart: String) {
        if (newPinCodeOnAppStart.length > 16) {
            return
        }
        if (newPinCodeOnAppStart.isNotEmpty() && !newPinCodeOnAppStart.matches(Regex("^[0-9]*\$"))) {
            return
        }
        pinCodeOnAppStart = newPinCodeOnAppStart
        viewModelScope.launch {
            dataStoreManager.saveString(DataStoreManager.PIN_CODE_ON_APP_START, newPinCodeOnAppStart)
        }
    }

    fun updatePinCodeOpenSettings(newPinCodeOpenSettings: String) {
        if (newPinCodeOpenSettings.length > 16) {
            return
        }
        if (newPinCodeOpenSettings.isNotEmpty() && !newPinCodeOpenSettings.matches(Regex("^[0-9]*\$"))) {
            return
        }
        pinCodeOpenSettings = newPinCodeOpenSettings
        viewModelScope.launch {
            dataStoreManager.saveString(DataStoreManager.PIN_CODE_OPEN_SETTINGS, newPinCodeOpenSettings)
        }
    }
}


