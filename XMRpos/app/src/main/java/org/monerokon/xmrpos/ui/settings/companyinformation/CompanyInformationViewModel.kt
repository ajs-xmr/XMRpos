// CompanyInformationViewModel.kt
package org.monerokon.xmrpos.ui.settings.companyinformation

import android.net.Uri
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
class CompanyInformationViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
) : ViewModel() {

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
            dataStoreRepository.getCompanyName().collect { storedCompanyName ->
                companyName = storedCompanyName
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getContactInformation().collect { storedContactInformation ->
                contactInformation = storedContactInformation
            }
        }
    }

    fun updateCompanyName(newCompanyName: String) {
        companyName = newCompanyName
        viewModelScope.launch {
            dataStoreRepository.saveCompanyName(newCompanyName)
        }
    }

    fun updateContactInformation(newContactInformation: String) {
        contactInformation = newContactInformation
        viewModelScope.launch {
            dataStoreRepository.saveContactInformation(newContactInformation)
        }
    }

    // TODO: get image from uri and save it to internal storage
    fun saveLogo(uri: Uri) {
        println("TODO")
    }

}


