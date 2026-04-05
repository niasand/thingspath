package com.thingspath.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.thingspath.ui.screen.additem.AddItemScreen
import com.thingspath.ui.screen.home.HomeScreen
import com.thingspath.ui.screen.itemdetail.ItemDetailScreen
import com.thingspath.ui.screen.settings.SettingsScreen
import com.thingspath.ui.screen.statistics.StatisticsScreen

@Composable
fun AppNavigation(
    navController: NavHostController = androidx.navigation.compose.rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onItemClick = { itemId ->
                    navController.navigate(Screen.ItemDetail.createRoute(itemId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToStatistics = {
                    navController.navigate(Screen.Statistics.route)
                }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Statistics.route) {
            StatisticsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.AddItem.route) {
            AddItemScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ItemDetail.route,
            arguments = listOf(
                navArgument("itemId") { type = androidx.navigation.NavType.LongType; defaultValue = 0L }
            )
        ) {
            ItemDetailScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
