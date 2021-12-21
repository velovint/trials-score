package net.yakavenka.trialsscore

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SectionScoreActivityTest {
    @get:Rule
    val activity = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun sections_score_page() {
        // when navigating to sections score
        // then page contains 10 laps
//        onView(withId(R.id.lap_score_container))
//            .check()
        onView(withText("1")).check(matches(isDisplayed()))
        onView(withText("10")).check(matches(isDisplayed()))
    }
}