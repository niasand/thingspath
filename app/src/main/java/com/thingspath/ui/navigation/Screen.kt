package com.thingspath.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddItem : Screen("add_item")
    object AIAdd : Screen("ai_add")
    object Settings : Screen("settings")
    object ItemDetail : Screen("item_detail/{itemId}") {
        fun createRoute(itemId: Long) = "item_detail/$itemId"
    }
}
