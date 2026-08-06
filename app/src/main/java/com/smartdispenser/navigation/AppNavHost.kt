package com.smartdispenser.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartdispenser.ui.screens.CategoryDetailsScreen
import com.smartdispenser.ui.screens.HomeScreen
import com.smartdispenser.ui.screens.TimerPresetScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route
    ) {
        composable(NavRoutes.Home.route) {
            HomeScreen(
                onCategoryClick = { categoryId ->
                    navController.navigate(NavRoutes.CategoryDetails.createRoute(categoryId))
                }
            )
        }

        composable(
            route = NavRoutes.CategoryDetails.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: -1L
            CategoryDetailsScreen(
                categoryId = categoryId,
                onBack = { navController.popBackStack() },
                onAddTimer = {
                    navController.navigate(NavRoutes.AddEditTimer.createRoute(categoryId))
                },
                onEditTimer = { presetId ->
                    navController.navigate(NavRoutes.AddEditTimer.createRoute(categoryId, presetId))
                }
            )
        }

        composable(
            route = NavRoutes.AddEditTimer.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType },
                navArgument("presetId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: -1L
            val presetId = backStackEntry.arguments?.getLong("presetId")?.takeIf { it > 0 }
            TimerPresetScreen(
                categoryId = categoryId,
                presetId = presetId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}