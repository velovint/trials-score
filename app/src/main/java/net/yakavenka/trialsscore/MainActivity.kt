package net.yakavenka.trialsscore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.yakavenka.trialsscore.components.TrialsScoreApplicationComponent


class MainActivity : ComponentActivity() {

//    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val navHostFragment =
//            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//        navController = navHostFragment.navController
//        setupActionBarWithNavController(navController)
        setContent {
            TrialsScoreApplicationComponent()
        }


    }

//    override fun onSupportNavigateUp(): Boolean {
//        return navController.navigateUp() || super.onSupportNavigateUp()
//    }
}