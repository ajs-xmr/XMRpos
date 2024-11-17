package org.monerokon.xmrpos.ui.payment

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.monerokon.xmrpos.data.DataStoreManager
import org.monerokon.xmrpos.data.ExchangeRateManager
import org.monerokon.xmrpos.data.MoneroPayCallbackServer
import org.monerokon.xmrpos.data.MoneroPayManager
import org.monerokon.xmrpos.ui.PaymentEntry
import org.monerokon.xmrpos.ui.PaymentSuccess
import java.net.NetworkInterface
import java.util.Hashtable

class PaymentCheckoutViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

    var paymentValue by mutableStateOf(0.0)
    var primaryFiatCurrency by mutableStateOf("")
    var referenceFiatCurrencies by mutableStateOf(listOf<String>())
    var exchangeRates: Map<String, Double>? by mutableStateOf(null)
    var targetXMRvalue by mutableStateOf(0.0)

    var moneroPayServerAddress by mutableStateOf("")
    var qrCodeUri by mutableStateOf("")
    var address by mutableStateOf("")
    var errorMessage by mutableStateOf("")

    private var navController: NavHostController? = null

    init {
        fetchReferenceFiatCurrencies()
    }

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateBack() {
        navController?.navigate(PaymentEntry)
    }

    fun updatePaymentValue(value: Double) {
        paymentValue = value
    }

    fun updatePrimaryFiatCurrency(value: String) {
        primaryFiatCurrency = value
    }

    private fun fetchReferenceFiatCurrencies() {
        viewModelScope.launch(Dispatchers.IO) {
            val newReferenceFiatCurrencies = dataStoreManager.getStringList(DataStoreManager.REFERENCE_FIAT_CURRENCIES).first()
            newReferenceFiatCurrencies?.let {
                withContext(Dispatchers.Main) {
                    referenceFiatCurrencies = it
                    fetchExchangeRates(it + primaryFiatCurrency)
                }
            }
        }
    }

    private fun fetchExchangeRates(currencies: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val rates = ExchangeRateManager.fetchExchangeRates("XMR", currencies)
            rates?.let {
                withContext(Dispatchers.Main) {
                    exchangeRates = it
                    calculateTargetXMRvalue()
                }
            }
        }
    }

    private fun calculateTargetXMRvalue() {
        exchangeRates?.get(primaryFiatCurrency)?.let {
            targetXMRvalue = paymentValue / it
            startReceive()
        }
    }

    private fun startReceive() {
        viewModelScope.launch(Dispatchers.IO) {
            val newMoneroPayServerAddress = dataStoreManager.getString(DataStoreManager.MONERO_PAY_SERVER_ADDRESS).first()
            newMoneroPayServerAddress?.let {
                withContext(Dispatchers.Main) {
                    moneroPayServerAddress = it
                    getDeviceIpAddress()?.let { ipAddress ->
                        startMoneroPayReceive(ipAddress)
                    } ?: run {
                        errorMessage = "Failed to get IP address"
                    }
                }
            }
        }
    }

    private fun startMoneroPayReceive(ipAddress: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = MoneroPayManager(moneroPayServerAddress).startReceive(
                (targetXMRvalue * 1000000000000).toLong(), "XMRPOS", "http://$ipAddress:8080"
            )
            withContext(Dispatchers.Main) {
                response?.let {
                    address = it.address
                    qrCodeUri = "monero:${it.address}?tx_amount=${it.amount}&tx_description=${it.description}"
                    startReceiveStatusCheck()
                } ?: run {
                    errorMessage = "MoneroPay server is not responding"
                }
            }
        }
    }

    private fun startReceiveStatusCheck() {
        MoneroPayCallbackServer.getInstance(8080) { paymentCallback ->
            if (paymentCallback.amount.expected == paymentCallback.amount.covered.total) {
                println("Payment received!")
                navigateToPaymentSuccess(PaymentSuccess(11.5, "USD", 0.33))
            }
        }
    }

    private fun getDeviceIpAddress(): String? {
        return NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    }

    fun navigateToPaymentSuccess(paymentSuccess: PaymentSuccess) {
        navController?.navigate(paymentSuccess)
    }

    fun generateQRCode(text: String, width: Int, height: Int, margin: Int, color: Int, background: Int): Bitmap {
        val writer = QRCodeWriter()
        val hints = Hashtable<EncodeHintType, Any>().apply {
            this[EncodeHintType.MARGIN] = margin
        }
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints)
        return Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    setPixel(x, y, if (bitMatrix[x, y]) color else background)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        MoneroPayCallbackServer.stopServer()
    }
}