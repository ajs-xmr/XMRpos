// PaymentEntryViewModel.kt
package org.monerokon.xmrpos.ui.payment

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.monerokon.xmrpos.data.DataStoreManager
import org.monerokon.xmrpos.data.ExchangeRateManager
import org.monerokon.xmrpos.ui.PaymentCheckout
import org.monerokon.xmrpos.ui.Settings

class PaymentEntryViewModel (application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

    var primaryFiatCurrency by mutableStateOf("");
    var paymentValue by mutableStateOf("0");
    var exchangeRate: Double? by mutableStateOf(null);

    var openSettingsPinCodeDialog by mutableStateOf(false)
    var requirePinCodeOpenSettings by mutableStateOf(true)
    var pinCodeOpenSettings by mutableStateOf("`")

    init {
        // get exchange rate from public api
        fetchPrimaryFiatCurrency()
        CoroutineScope(Dispatchers.IO).launch {
            val storedRequirePinCodeOpenSettings = dataStoreManager.getBoolean(DataStoreManager.REQUIRE_PIN_CODE_OPEN_SETTINGS).first()
            withContext(Dispatchers.Main) {
                requirePinCodeOpenSettings = storedRequirePinCodeOpenSettings ?: false
            }

        }
        CoroutineScope(Dispatchers.IO).launch {
            val storedPinCodeOpenSettings = dataStoreManager.getString(DataStoreManager.PIN_CODE_OPEN_SETTINGS).first()
            withContext(Dispatchers.Main) {
                pinCodeOpenSettings = storedPinCodeOpenSettings ?: ""
            }
        }
    }

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun addDigit(digit: String) {
        // Rules to prevent invalid input
        if (paymentValue.length >= 16) {
            return
        }
        if (paymentValue.isEmpty() && digit == "0") {
            return
        }
        if (paymentValue.isEmpty() && digit == ".") {
            paymentValue += "0."
            return
        }
        if (paymentValue.contains(".") && digit == ".") {
            return
        }
        if (paymentValue == "0" && digit != ".") {
            paymentValue = digit
            return
        }
        paymentValue += digit
    }

    fun removeDigit() {
        if (paymentValue.length == 1) {
            paymentValue = "0"
            return
        }
        if (paymentValue.isNotEmpty()) {
            paymentValue = paymentValue.dropLast(1)
        }
    }

    fun clear() {
        paymentValue = "0"
    }

    fun submit() {
        println("Going to next screen!")
        if (paymentValue.toDouble() == 0.0) {
            return
        }
        navController?.navigate(PaymentCheckout(paymentValue.toDouble(), primaryFiatCurrency))
    }

    fun tryOpenSettings() {
        println("open settings")
        if (requirePinCodeOpenSettings && pinCodeOpenSettings != "") {
            openSettingsPinCodeDialog = true
        } else {
            navController?.navigate(Settings)
        }
    }

    fun openSettings() {
        navController?.navigate(Settings)
    }

    fun updateOpenSettingsPinCodeDialog(newOpenSettingsPinCodeDialog: Boolean) {
        openSettingsPinCodeDialog = newOpenSettingsPinCodeDialog
    }

    fun fetchPrimaryFiatCurrency() {
        CoroutineScope(Dispatchers.IO).launch {
            val newPrimaryFiatCurrency = dataStoreManager.getString(DataStoreManager.PRIMARY_FIAT_CURRENCY).first()
            if (newPrimaryFiatCurrency != null) {
                withContext(Dispatchers.Main) {
                    primaryFiatCurrency = newPrimaryFiatCurrency;
                    if (newPrimaryFiatCurrency != "") {
                        println("primaryFiatCurrency: $newPrimaryFiatCurrency")
                        fetchExchangeRate(newPrimaryFiatCurrency)
                    }
                }
            }
        }
    }

    fun fetchExchangeRate(currency: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val rates = ExchangeRateManager.fetchExchangeRates("XMR", listOf(currency))
            rates?.let {
                println("rate: ${it[currency]}")
                withContext(Dispatchers.Main) {
                    exchangeRate = it[currency]
                }
            }
        }
    }
}