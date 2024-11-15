package org.monerokon.xmrpos.ui.settings.fiatcurrencies

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

class FiatCurrenciesViewModel (application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    val fiatOptions = listOf("USD", "EUR", "CZK", "MXN")

    private val dataStoreManager = DataStoreManager(application)

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToMainSettings() {
        navController?.navigate(Settings)
    }

    var primaryFiatCurrency: String by mutableStateOf("")

    var referenceFiatCurrencies: List<String> by mutableStateOf(emptyList())

    init {
        viewModelScope.launch {
            dataStoreManager.getString(DataStoreManager.PRIMARY_FIAT_CURRENCY).collect { storedPrimaryFiatCurrency ->
                println("primaryFiatCurrency: $storedPrimaryFiatCurrency")
                primaryFiatCurrency = storedPrimaryFiatCurrency?: ""
            }
        }
        viewModelScope.launch {
            dataStoreManager.getStringList(DataStoreManager.REFERENCE_FIAT_CURRENCIES).collect { storedReferenceFiatCurrencies ->
                println("storedContactInformation: $storedReferenceFiatCurrencies")
                referenceFiatCurrencies = storedReferenceFiatCurrencies?: emptyList()
            }
        }
    }

    fun updatePrimaryFiatCurrency(newPrimaryFiatCurrency: String) {
        primaryFiatCurrency = newPrimaryFiatCurrency
        viewModelScope.launch {
            dataStoreManager.saveString(DataStoreManager.PRIMARY_FIAT_CURRENCY, newPrimaryFiatCurrency)
        }
    }

    fun addReferenceFiatCurrency(newReferenceFiatCurrency: String) {
        referenceFiatCurrencies = referenceFiatCurrencies + newReferenceFiatCurrency
        viewModelScope.launch {
            dataStoreManager.saveStringList(DataStoreManager.REFERENCE_FIAT_CURRENCIES, referenceFiatCurrencies)
        }
    }

    fun removeReferenceFiatCurrency(index: Int) {
        referenceFiatCurrencies = referenceFiatCurrencies.toMutableList().apply { removeAt(index) }
        viewModelScope.launch {
            dataStoreManager.saveStringList(DataStoreManager.REFERENCE_FIAT_CURRENCIES, referenceFiatCurrencies)
        }
    }

    fun moveReferenceFiatCurrencyUp(index: Int) {
        if (index > 0) {
            referenceFiatCurrencies = referenceFiatCurrencies.toMutableList().apply {
                val temp = this[index]
                this[index] = this[index - 1]
                this[index - 1] = temp
            }
            viewModelScope.launch {
                dataStoreManager.saveStringList(DataStoreManager.REFERENCE_FIAT_CURRENCIES, referenceFiatCurrencies)
            }
        }
    }

    fun moveReferenceFiatCurrencyDown(index: Int) {
        if (index < referenceFiatCurrencies.size - 1) {
            referenceFiatCurrencies = referenceFiatCurrencies.toMutableList().apply {
                val temp = this[index]
                this[index] = this[index + 1]
                this[index + 1] = temp
            }
            viewModelScope.launch {
                dataStoreManager.saveStringList(DataStoreManager.REFERENCE_FIAT_CURRENCIES, referenceFiatCurrencies)
            }
        }
    }


}


