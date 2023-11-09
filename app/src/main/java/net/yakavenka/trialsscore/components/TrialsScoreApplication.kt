package net.yakavenka.trialsscore.components

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun TrialsScoreApplicationComponent() {
    val navController = rememberNavController()
    TrialsScoreNavHost(navController)
}

@Composable
fun TrialsScoreNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "leaderboard") {
        composable("leaderboard") {
            LeaderboardScreen(
                viewModel = hiltViewModel(),
                onRiderSelect = { rider ->
                    navController.navigate("points_entry/${rider.riderId}/1")
                },
                onAdd = {
                    navController.navigate("add_rider")
                },
                onSettings = {
                    navController.navigate("settings")
                },
                onShowFullList = {
                    navController.navigate("screenshot_view")
                }
            )
        }
        composable("screenshot_view") {
            ScreenshotLeaderboardScreen(
                viewModel = hiltViewModel()
            )
        }
        composable(
            "points_entry/{riderId}/{loop}",
            arguments = listOf(
                navArgument("riderId") { type = NavType.IntType },
                navArgument("loop") { type = NavType.IntType })
        ) { backStackEntry ->
            LoopScoreEntryScreen(
                viewModel = hiltViewModel(),
                onLoopSelect = { loopNum ->
                    val riderId = backStackEntry.arguments?.getInt("riderId")
                    navController.navigate(
                        "points_entry/${riderId}/${loopNum}",
                        navOptions = NavOptions.Builder().setPopUpTo("leaderboard", false).build()
                    )
                },
                onEditRider = { riderInfo -> navController.navigate("edit_rider/${riderInfo.id}")},
                onBack = navController::navigateUp
            )

        }
        composable("add_rider") {
            EditRiderScreen(
                viewModel = hiltViewModel(),
                navigateBack = navController::popBackStack
            )
        }
        composable("edit_rider/{riderId}",
            arguments = listOf(navArgument("riderId") { type = NavType.IntType })
        ) { _ ->
            EditRiderScreen(
                viewModel = hiltViewModel(),
                navigateBack = navController::navigateUp
            )
        }
        composable("settings") {
            EventSettingsScreen(navigateBack = navController::navigateUp)
        }
    }

}

