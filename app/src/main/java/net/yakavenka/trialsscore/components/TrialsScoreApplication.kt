package net.yakavenka.trialsscore.components

import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
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
                    navController.navigate("points_entry/${rider.riderId}/${rider.riderName}/1")
                },
                onAdd = {
                    navController.navigate("add_rider")
                })
        }
        composable(
            "points_entry/{riderId}/{riderName}/{loop}",
            arguments = listOf(
                navArgument("riderId") { type = NavType.IntType },
                navArgument("riderName") { type = NavType.StringType },
                navArgument("loop") { type = NavType.IntType })
        ) { backStackEntry ->
            LoopScoreEntryScreen(
                viewModel(factory = ScoreCardViewModel.Factory),
                onNavigate = { loopNum ->
                    val riderId = backStackEntry.arguments?.getInt("riderId")
                    val riderName = backStackEntry.arguments?.getString("riderName")
                    navController.navigate(
                        "points_entry/${riderId}/${riderName}/${loopNum}",
                        navOptions = NavOptions.Builder().setPopUpTo("leaderboard", false).build()
                    )
                })
        }
        composable("add_rider") {
            EditRiderScreen(
                viewModel = viewModel(factory = EditRiderViewModel.Factory)
            )
        }
    }

}