package net.yakavenka.trialsscore.components

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import net.yakavenka.trialsscore.viewmodel.EditRiderViewModel
import net.yakavenka.trialsscore.viewmodel.EventScoreViewModel
import net.yakavenka.trialsscore.viewmodel.ScoreCardViewModel

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
                viewModel = viewModel(factory = EventScoreViewModel.Factory),
                onRiderSelect = { rider ->
                    navController.navigate("points_entry/${rider.riderId}/1")
                },
                onAdd = {
                    navController.navigate("add_rider")
                },
                onSettings = {
                    navController.navigate("settings")
                })
        }
        composable(
            "points_entry/{riderId}/{loop}",
            arguments = listOf(
                navArgument("riderId") { type = NavType.IntType },
                navArgument("loop") { type = NavType.IntType })
        ) { backStackEntry ->
            LoopScoreEntryScreen(
                viewModel(factory = ScoreCardViewModel.Factory),
                onLoopSelect = { loopNum ->
                    val riderId = backStackEntry.arguments?.getInt("riderId")
                    navController.navigate(
                        "points_entry/${riderId}/${loopNum}",
                        navOptions = NavOptions.Builder().setPopUpTo("leaderboard", false).build()
                    )
                },
                onEditRider = { riderId -> navController.navigate("edit_rider/${riderId}")},
                onBack = navController::navigateUp
            )

        }
        composable("add_rider") {
            EditRiderScreen(
                viewModel = viewModel(factory = EditRiderViewModel.Factory),
                navigateBack = navController::navigateUp
            )
        }
        composable("edit_rider/{riderId}",
            arguments = listOf(navArgument("riderId") { type = NavType.IntType })
        ) { _ ->
            EditRiderScreen(
                viewModel = viewModel(factory = EditRiderViewModel.Factory),
                navigateBack = navController::navigateUp
            )
        }
        composable("settings") {
            EventSettingsScreen()
        }
    }

}