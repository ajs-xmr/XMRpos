// CompanyInformationViewModel.kt
package org.monerokon.xmrpos.ui.settings.companyinformation

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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

class CompanyInformationViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

    private var navController: NavHostController? = null

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    fun navigateToMainSettings() {
        navController?.navigate(Settings)
    }

    var companyName: String by mutableStateOf("")

    var contactInformation: String by mutableStateOf("")

    // Load data from DataStore when ViewModel is initialized
    init {
        viewModelScope.launch {
            dataStoreManager.getString(DataStoreManager.COMPANY_NAME).collect { storedCompanyName ->
                println("storedCompanyName: $storedCompanyName")
                companyName = storedCompanyName?: ""
            }
        }
        viewModelScope.launch {
            dataStoreManager.getString(DataStoreManager.CONTACT_INFORMATION).collect { storedContactInformation ->
                println("storedContactInformation: $storedContactInformation")
                contactInformation = storedContactInformation?: ""
            }
        }
    }

    fun updateCompanyName(newCompanyName: String) {
        companyName = newCompanyName
        viewModelScope.launch {
            dataStoreManager.saveString(DataStoreManager.COMPANY_NAME, newCompanyName)
        }
    }

    fun updateContactInformation(newContactInformation: String) {
        contactInformation = newContactInformation
        viewModelScope.launch {
            dataStoreManager.saveString(DataStoreManager.CONTACT_INFORMATION, contactInformation)
        }
    }

    // TODO: get image from uri and save it to internal storage
    fun saveLogo(uri: Uri) {
        println("TODO")
    }

}


