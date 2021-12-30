package net.yakavenka.trialsscore

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.javafaker.Faker
import net.yakavenka.trialsscore.adapter.EventScoreAdapter
import org.hamcrest.core.StringContains
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SectionScoreActivityTest {
    val faker: Faker = Faker()

    @get:Rule
    val activity = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun addNewRiderCreatesEntry() {
        val riderName = giverRegisteredRider()

        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()))
        onView(withText(riderName)).check(matches(isDisplayed()))
    }

    @Test
    fun navigateToScoreEntryPage() {
        val riderName = giverRegisteredRider()

        onView(withText(riderName)).perform(click())

        onView(withText("1")).check(matches(isDisplayed()))
        onView(withText("9")).check(matches(isDisplayed()))
        onView(withId(R.id.lap_score))
            .check(matches(withText(StringContains.containsString("0 / 0"))))
    }

    @Test
    fun pointsEntryPageUpdatesTotalScore() {
        val riderName = giverRegisteredRider()

        onView(withText(riderName)).perform(click())

        // this click currently lands on 2. need to find how to poke more accurate
        onView(withId(R.id.lap_score_container))
//            .perform(RecyclerViewActions.scrollToPosition<EventScoreAdapter.ViewHolder>(0))
            .perform(RecyclerViewActions.actionOnItemAtPosition<EventScoreAdapter.ViewHolder>(
                0,
                click()
            ))

        onView(withId(R.id.lap_score))
            .check(matches(withText(StringContains.containsString("2 / 0"))))
    }

    private fun giverRegisteredRider(): String {
        val riderName = "${faker.name().firstName()} ${faker.name().lastName()}"
        onView(withId(R.id.floating_action_button)).perform(click())
        onView(withId(R.id.rider_name)).perform(typeText(riderName))
        onView(withId(R.id.rider_class)).perform(typeText("Novice"))
        onView(withId(R.id.save_action)).perform(click())
        return riderName
    }
}