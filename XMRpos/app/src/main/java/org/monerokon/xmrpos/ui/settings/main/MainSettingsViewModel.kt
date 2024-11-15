// MainSettingsViewModel.kt
package org.monerokon.xmrpos.ui.settings.main

import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import org.monerokon.xmrpos.ui.CompanyInformation
import org.monerokon.xmrpos.ui.FiatCurrencies
import org.monerokon.xmrpos.ui.MoneroPay
import org.monerokon.xmrpos.ui.PaymentEntry

class MainSettingsViewModel (private val savedStateHandle: SavedStateHandle): ViewModel() {

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToPayment() {
        navController?.navigate(PaymentEntry)
    }

    fun navigateToCompanyInformation() {
        navController?.navigate(CompanyInformation)
    }

    fun navigateToFiatCurrencies() {
        navController?.navigate(FiatCurrencies)
    }

    fun navigateToMoneroPay() {
        navController?.navigate(MoneroPay)
    }
}


