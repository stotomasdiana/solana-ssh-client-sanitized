package com.example.sshclient.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.example.sshclient.ui.screen.serveredit.ServerEditScreen
import com.example.sshclient.ui.screen.serverlist.ServerListScreen
import com.example.sshclient.ui.screen.terminal.TerminalScreen

sealed class Screen(val route: String) {
    data object ServerList : Screen("server_list")
    data object ServerEdit : Screen("server_edit?serverId={serverId}") {
        fun createRoute(serverId: Long? = null): String =
            if (serverId == null) "server_edit" else "server_edit?serverId=$serverId"
    }

    data object Terminal : Screen("terminal/{serverId}") {
        fun createRoute(serverId: Long): String = "terminal/$serverId"
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    walletActivityResultSender: ActivityResultSender?
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ServerList.route
    ) {
        composable(Screen.ServerList.route) {
            ServerListScreen(
                walletActivityResultSender = walletActivityResultSender,
                onAddServer = { navController.navigate(Screen.ServerEdit.createRoute()) },
                onEditServer = { id -> navController.navigate(Screen.ServerEdit.createRoute(id)) },
                onConnectServer = { id -> navController.navigate(Screen.Terminal.createRoute(id)) }
            )
        }

        composable(
            route = Screen.ServerEdit.route,
            arguments = listOf(
                navArgument("serverId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) {
            ServerEditScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Terminal.route,
            arguments = listOf(
                navArgument("serverId") { type = NavType.LongType }
            )
        ) {
            TerminalScreen(onBack = { navController.popBackStack() })
        }
    }
}
