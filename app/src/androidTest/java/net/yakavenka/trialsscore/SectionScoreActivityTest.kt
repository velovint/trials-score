package net.yakavenka.trialsscore

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.javafaker.Faker
import net.yakavenka.trialsscore.data.RiderScore
import net.yakavenka.trialsscore.model.RiderScoreAdapter
import net.yakavenka.trialsscore.model.SectionScoreAdapter
import net.yakavenka.trialsscore.viewmodel.EditRiderViewModel
import org.hamcrest.Matchers.*
import org.hamcrest.core.StringContains
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
        val entry = giverRegisteredRider()

        scrollToRider(entry)
        onView(withText(containsString(entry.name))).check(matches(isDisplayed()))
    }

    @Test
    fun navigateToScoreEntryPage() {
        val rider = giverRegisteredRider()

        openScoreEntry(rider)

        onView(withText("1")).check(matches(isDisplayed()))
        onView(withText("9")).check(matches(isDisplayed()))
        onView(withId(R.id.lap_score))
            .check(matches(withText(containsString("0 / 0"))))
    }

    @Test
    fun pointsEntryPageUpdatesTotalScore() {
        val rider = giverRegisteredRider()

        openScoreEntry(rider)

        // this click currently lands on 2. need to find how to poke more accurate
        onView(withId(R.id.lap_score_container))
//            .perform(RecyclerViewActions.scrollToPosition<EventScoreAdapter.ViewHolder>(0))
            .perform(RecyclerViewActions.actionOnItemAtPosition<SectionScoreAdapter.ViewHolder>(
                0,
                click()
            ))

        onView(withId(R.id.lap_score))
            .check(matches(withText(StringContains.containsString("2 / 0"))))
    }

    private fun giverRegisteredRider(): RiderScore {
        val riderName = "${faker.name().firstName()} ${faker.name().lastName()}"
        val riderClass = EditRiderViewModel.RIDER_CLASS_OPTIONS.random()
        onView(withId(R.id.floating_action_button)).perform(click())
        onView(withId(R.id.rider_name)).perform(typeText(riderName))
        onView(withId(R.id.rider_class_label)).perform(click())
        EditRiderViewModel.RIDER_CLASS_OPTIONS.random()
        onView(withText(riderClass))
            .inRoot(RootMatchers.isPlatformPopup())
            .perform(click())
        onView(withId(R.id.save_action)).perform(click())
        return RiderScore(name = riderName, riderClass = riderClass )
    }

    private fun openScoreEntry(rider: RiderScore) {
        scrollToRider(rider)
        onView(withText(containsString(rider.name))).perform(click())
    }

    private fun scrollToRider(rider: RiderScore) {
        onView(withId(R.id.recycler_view))
            .perform(RecyclerViewActions.scrollTo<RiderScoreAdapter.ViewHolder>(
                hasDescendant(withText(containsString(rider.name)))
            ))
    }
}
