package com.example.sshclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.example.sshclient.ui.navigation.AppNavGraph
import com.example.sshclient.ui.theme.SshClientTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var walletActivityResultSender: ActivityResultSender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        walletActivityResultSender = ActivityResultSender(this)
        enableEdgeToEdge()
        setContent {
            SshClientTheme {
                AppNavGraph(
                    navController = rememberNavController(),
                    walletActivityResultSender = walletActivityResultSender
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() {
    SshClientTheme {
        AppNavGraph(
            navController = rememberNavController(),
            walletActivityResultSender = null
        )
    }
}
