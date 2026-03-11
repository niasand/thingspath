package com.thingspath.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.thingspath.ui.screen.additem.AddItemScreen
import com.thingspath.ui.screen.home.HomeScreen
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
                onItemClick = { itemId ->
                    navController.navigate(Screen.ItemDetail.createRoute(itemId))
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
            com.thingspath.ui.screen.aiadd.AIAddScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onBack = {
                    navController.popBackStack()
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
