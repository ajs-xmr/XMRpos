// PaymentSuccessViewModel.kt
package org.monerokon.xmrpos.ui.payment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import org.monerokon.xmrpos.data.DataStoreManager
import org.monerokon.xmrpos.ui.PaymentEntry

class PaymentSuccessViewModel (application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToEntry() {
        navController?.navigate(PaymentEntry)
    }


}