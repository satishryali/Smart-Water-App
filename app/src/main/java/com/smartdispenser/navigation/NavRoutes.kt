package com.smartdispenser.navigation

sealed class NavRoutes(val route: String) {
    data object Home : NavRoutes("home")
    data object CategoryDetails : NavRoutes("category_details/{categoryId}") {
        fun createRoute(categoryId: Long) = "category_details/$categoryId"
    }
    data object AddEditTimer : NavRoutes("add_edit_timer/{categoryId}?presetId={presetId}") {
        fun createRoute(categoryId: Long, presetId: Long? = null) =
            if (presetId != null) {
                "add_edit_timer/$categoryId?presetId=$presetId"
            } else {
                "add_edit_timer/$categoryId"
            }
    }
}