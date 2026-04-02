package com.example.sshclient.wallet

import android.net.Uri
import com.funkatronics.encoders.Base58
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.RpcCluster
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WalletTransferService @Inject constructor() {

    suspend fun signAndSendMainnetTransfer(sender: ActivityResultSender): WalletTransferResult {
        return runCatching {
            val walletAdapter = MobileWalletAdapter(
                connectionIdentity = ConnectionIdentity(
                    Uri.parse(APP_IDENTITY_URI),
                    Uri.parse(APP_ICON_RELATIVE_URI),
                    APP_IDENTITY_NAME
                )
            ).apply {
                rpcCluster = RpcCluster.MainnetBeta
            }

            when (
                val result = walletAdapter.transact(sender) { authResult ->
                    val fromPublicKeyBytes = authResult.accounts.firstOrNull()?.publicKey
                        ?: error("Wallet did not return a public key")
                    val fromPublicKey = SolanaPublicKey(fromPublicKeyBytes)
                    val recipientPublicKey = SolanaPublicKey.Companion.from(RECIPIENT_ADDRESS)
                    val latestBlockhash = fetchLatestBlockhash()

                    val transaction = Transaction(
                        Message.Builder()
                            .addInstruction(
                                TransactionInstruction(
                                    programId = SolanaPublicKey.Companion.from(SYSTEM_PROGRAM_ID),
                                    accounts = listOf(
                                        AccountMeta(fromPublicKey, true, true),
                                        AccountMeta(recipientPublicKey, false, true)
                                    ),
                                    data = createSystemTransferData(TRANSFER_LAMPORTS)
                                )
                            )
                            .setRecentBlockhash(latestBlockhash)
                            .build()
                    )

                    signAndSendTransactions(arrayOf(transaction.serialize()))
                }
            ) {
                is TransactionResult.Success -> {
                    val addressBytes = result.authResult.accounts.firstOrNull()?.publicKey
                    val signatureBytes = result.payload.signatures.firstOrNull()

                    WalletTransferResult.Success(
                        walletAddress = addressBytes?.let { SolanaPublicKey(it).base58() }.orEmpty(),
                        signature = signatureBytes?.let(Base58::encodeToString).orEmpty()
                    )
                }

                is TransactionResult.NoWalletFound -> WalletTransferResult.Error(
                    "No compatible Solana Mobile Wallet Adapter wallet was found on this device."
                )

                is TransactionResult.Failure -> WalletTransferResult.Error(
                    result.e.message ?: result.message ?: "Mainnet transfer failed"
                )
            }
        }.getOrElse { throwable ->
            WalletTransferResult.Error(
                throwable.message ?: "An unknown error occurred during wallet signing."
            )
        }
    }

    private fun createSystemTransferData(lamports: Long): ByteArray =
        ByteBuffer.allocate(12)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(SYSTEM_TRANSFER_INSTRUCTION_INDEX)
            .putLong(lamports)
            .array()

    private suspend fun fetchLatestBlockhash(): String = withContext(Dispatchers.IO) {
        val connection = (URL(MAINNET_RPC_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        try {
            connection.outputStream.bufferedWriter().use { writer ->
                writer.write(GET_LATEST_BLOCKHASH_REQUEST)
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)
            json
                .optJSONObject("result")
                ?.optJSONObject("value")
                ?.optString("blockhash")
                ?.takeIf { it.isNotBlank() }
                ?: error("Mainnet RPC did not return a blockhash")
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val APP_IDENTITY_NAME = "SSH Client"
        const val APP_IDENTITY_URI = "https://example.com"
        const val APP_ICON_RELATIVE_URI = "favicon.ico"
        const val MAINNET_RPC_URL = "https://api.mainnet-beta.solana.com"
        const val RECIPIENT_ADDRESS = "REPLACE_WITH_RECIPIENT_ADDRESS"
        const val SYSTEM_PROGRAM_ID = "11111111111111111111111111111111"
        const val SYSTEM_TRANSFER_INSTRUCTION_INDEX = 2
        const val TRANSFER_SOL = "REPLACE_WITH_TRANSFER_AMOUNT"
        const val TRANSFER_LAMPORTS = 10_000_000L
        const val GET_LATEST_BLOCKHASH_REQUEST =
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"getLatestBlockhash\",\"params\":[{\"commitment\":\"finalized\"}]}"
    }
}

sealed interface WalletTransferResult {
    data class Success(
        val walletAddress: String,
        val signature: String
    ) : WalletTransferResult

    data class Error(val message: String) : WalletTransferResult
}
