package org.monerokon.xmrpos.ui.settings.transactionhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.monerokon.xmrpos.data.remote.backend.model.BackendConfirmedTransaction
import org.monerokon.xmrpos.data.remote.backend.model.BackendPendingTransaction
import org.monerokon.xmrpos.data.repository.BackendRepository
import org.monerokon.xmrpos.shared.DataResult
import org.monerokon.xmrpos.ui.Settings
import javax.inject.Inject

data class TransactionHistoryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val confirmedTransactions: List<BackendConfirmedTransaction> = emptyList(),
    val pendingTransactions: List<BackendPendingTransaction> = emptyList(),
)

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val backendRepository: BackendRepository,
) : ViewModel() {

    private var navController: NavHostController? = null

    private val _uiState = MutableStateFlow(TransactionHistoryUiState(isLoading = true))
    val uiState: StateFlow<TransactionHistoryUiState> = _uiState.asStateFlow()

    fun setNavController(navController: NavHostController) {
        this.navController = navController
    }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = backendRepository.fetchTransactionHistory()) {
                is DataResult.Success -> {
                    _uiState.value = TransactionHistoryUiState(
                        confirmedTransactions = result.data.confirmedTransactions,
                        pendingTransactions = result.data.pendingTransactions,
                    )
                }

                is DataResult.Failure -> {
                    _uiState.value = TransactionHistoryUiState(
                        errorMessage = result.message ?: "Unable to load transactions.",
                    )
                }
            }
        }
    }

    fun navigateToMainSettings() {
        navController?.navigate(Settings)
    }
}
