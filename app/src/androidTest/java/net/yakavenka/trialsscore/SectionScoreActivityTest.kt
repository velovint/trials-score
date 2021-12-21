package net.yakavenka.trialsscore

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.core.StringContains
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

    @Test
    fun total_is_updated_on_click() {
        onData(withText("4"))
            .onChildView(withId(R.id.section_score_5))
            .perform(click())
//            .perform(RecyclerViewActions.scrollToPosition<EventScoreAdapter.ViewHolder>(0))

        onView(withId(R.id.lap_score))
            .check(matches(withText(StringContains.containsString("5"))))
    }
}