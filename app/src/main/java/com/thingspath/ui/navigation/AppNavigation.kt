package com.thingspath.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.thingspath.ui.screen.additem.AddItemScreen
import com.thingspath.ui.screen.home.HomeScreen
import com.thingspath.ui.screen.settings.SettingsScreen
import com.thingspath.ui.screen.itemdetail.ItemDetailScreen

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
                onAddItemClick = {
                    navController.navigate(Screen.AddItem.route)
                },
                onAddAIItemClick = {
                    navController.navigate(Screen.AIAdd.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onItemClick = { itemId ->
                    navController.navigate(Screen.ItemDetail.createRoute(itemId))
                }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
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

        composable(route = Screen.AIAdd.route) {
            val addItemViewModel: com.thingspath.ui.screen.additem.AddItemViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            com.thingspath.ui.screen.aiadd.AIAddScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onBack = {
                    navController.popBackStack()
                },
                onResult = { name, date, location, price ->
                    addItemViewModel.prefillFromAi(name, date, location, price)
                    navController.navigate(Screen.AddItem.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.ItemDetail.route,
            arguments = listOf(
                navArgument("itemId") { type = NavType.LongType; defaultValue = 0L }
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
