package com.example.sshclient.ui.screen.serverlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sshclient.data.model.Server
import com.example.sshclient.data.repository.ServerRepository
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.example.sshclient.wallet.WalletTransferResult
import com.example.sshclient.wallet.WalletTransferService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ServerListViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val walletTransferService: WalletTransferService
) : ViewModel() {

    val servers: StateFlow<List<Server>> = serverRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _walletTransferState = MutableStateFlow(WalletTransferState())
    val walletTransferState = _walletTransferState.asStateFlow()

    fun deleteServer(server: Server) {
        viewModelScope.launch {
            serverRepository.delete(server)
        }
    }

    fun launchWalletTransfer(activityResultSender: ActivityResultSender) {
        val current = _walletTransferState.value
        if (current.isRunning || current.hasAttempted) {
            return
        }
        runWalletTransfer(activityResultSender)
    }

    fun retryWalletTransfer(activityResultSender: ActivityResultSender) {
        if (_walletTransferState.value.isRunning) {
            return
        }
        runWalletTransfer(activityResultSender)
    }

    private fun runWalletTransfer(activityResultSender: ActivityResultSender) {
        viewModelScope.launch {
            _walletTransferState.update {
                it.copy(
                    hasAttempted = true,
                    isRunning = true,
                    statusText = "Opening wallet and requesting the configured transfer…",
                    errorMessage = null
                )
            }

            when (val result = walletTransferService.signAndSendMainnetTransfer(activityResultSender)) {
                is WalletTransferResult.Success -> {
                    _walletTransferState.update {
                        it.copy(
                            isRunning = false,
                            walletAddress = result.walletAddress,
                            signature = result.signature,
                            statusText = "Transaction submitted",
                            errorMessage = null
                        )
                    }
                }

                is WalletTransferResult.Error -> {
                    _walletTransferState.update {
                        it.copy(
                            isRunning = false,
                            statusText = "Transaction not completed",
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }
}

data class WalletTransferState(
    val hasAttempted: Boolean = false,
    val isRunning: Boolean = false,
    val statusText: String = "Waiting for wallet connection",
    val walletAddress: String? = null,
    val signature: String? = null,
    val errorMessage: String? = null
)
