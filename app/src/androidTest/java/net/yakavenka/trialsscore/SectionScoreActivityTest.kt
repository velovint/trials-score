package net.yakavenka.trialsscore

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.yakavenka.trialsscore.adapter.EventScoreAdapter
import org.hamcrest.core.StringContains
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SectionScoreActivityTest {
    @get:Rule
    val activity = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun sections_score_landing_page() {
        onView(withText("1")).check(matches(isDisplayed()))
        onView(withText("10")).check(matches(isDisplayed()))
        onView(withId(R.id.lap_score))
            .check(matches(withText(StringContains.containsString("0 / 0"))))
    }


    @Test
    fun total_is_updated_on_click() {
        // this click currently lands on 3. need to find how to poke more accurate
        onView(withId(R.id.lap_score_container))
//            .perform(RecyclerViewActions.scrollToPosition<EventScoreAdapter.ViewHolder>(0))
            .perform(RecyclerViewActions.actionOnItemAtPosition<EventScoreAdapter.ViewHolder>(
                0,
                click()
            ))

        onView(withId(R.id.lap_score))
            .check(matches(withText(StringContains.containsString("score: 0 / 3"))))
    }
}