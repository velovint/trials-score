package net.yakavenka.trialsscore.viewmodel

import com.github.javafaker.Faker
import net.yakavenka.trialsscore.data.RiderScoreSummary
import net.yakavenka.trialsscore.data.UserPreferences
import net.yakavenka.trialsscore.data.UserPreferencesRepository
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger


class RiderStandingTransformationTest {
    private val idCnt =  AtomicInteger()
    private val faker = Faker()
    private val classes = setOf("Advanced", "Intermediate", "Novice")
    private val numSections = 5
    private val sut = RiderStandingTransformation()

    @Test
    fun transformationSortsResultByClass() {
        val summary = listOf(
            createScore(riderClass = "Novice"),
            createScore(riderClass = "Advanced"),
            createScore(riderClass = "Novice"))

        val actual = sut.invoke(summary, UserPreferences(numSections, 3, classes)).map { it.riderClass }

        assertThat("Rider classes", actual, equalTo(listOf("Advanced", "Novice", "Novice")))
    }

    @Test
    fun transformationSetsStandingsWithinAClass() {
        val summary = listOf(
            RiderScoreSummary(1, "Novice Second", "Novice", numSections, 5, 8),
            RiderScoreSummary(2, "Rider2", "Advanced", numSections, 5, 8),
            RiderScoreSummary(3, "Novice Winner", "Novice", numSections, 2, 8)
        )

        val actual = sut.invoke(summary, UserPreferences(numSections, 3, classes)).filter { it.riderClass == "Novice" }

        assertThat("Top novice", actual[0].riderName, equalTo("Novice Winner"))
        assertThat("Top place", actual[0].standing, equalTo(1))
        assertThat("Second novice", actual[1].standing, equalTo(2))
    }

    @Test
    fun transformationSortsByPointsWithinAClass() {
        val classWinner = createScore(riderName = "Class Winner", riderClass = "Novice")
        val classSecond = createScore(riderName = "Class Second", points = classWinner.points + 2, riderClass = classWinner.riderClass)
        val differentClass = createScore(riderName = "Different Class", points = classWinner.points + 1, riderClass = "Advanced")
        val summary = listOf(classSecond, differentClass, classWinner)

        val actual = sut.invoke(summary, UserPreferences(numSections, 3, classes)).map(RiderStanding::riderName)

        assertThat("Order", actual, equalTo(listOf("Different Class", "Class Winner", "Class Second")))
    }

    @Test
    fun transformationSortsFinishedRiderFirst() {
        val finishedRider = createScore(riderName = "Finished Rider", sectionsRidden = numSections, points = 1000)
        val notFinishedRider = createScore(riderName = "1Not Finished", sectionsRidden = 0, points = 0)
        val summary = listOf(notFinishedRider, finishedRider)

        val actual = sut.invoke(summary, UserPreferences(numSections, 3, classes))

        assertThat("Finished rider first", actual[0].riderName, equalTo(finishedRider.riderName))
        assertThat("Not finished at the end", actual[1].riderName, equalTo(notFinishedRider.riderName))
    }

    @Test
    fun transformationSortsByNameWithinNotFinished() {
        val rider1 = createScore(riderName = "A", sectionsRidden = 0)
        val rider2 = createScore(riderName = "B",  sectionsRidden = 0)
        val finishedRider = createScore(riderName="A finished", sectionsRidden = numSections)
        val summary = listOf(rider2, rider1, finishedRider)

        val actual = sut.invoke(summary, UserPreferences(numSections, 3, classes)).map { it.riderName }

        assertThat("Name sort", actual, equalTo(listOf("A finished", "A", "B")))
    }

    @Test
    fun transformationSortsByCleansOnSamePoints() {
        val first = createScore(riderName = "More cleans")
        val second = createScore(riderName = "Less cleans",
            numCleans = first.numCleans - 1,
            points = first.points,
            riderClass = first.riderClass)
        val summary = listOf(second, first)

        val actual = sut.invoke(summary, UserPreferences(numSections, 3, classes)).map { it.riderName }

        assertThat("More cleans in better", actual, equalTo(listOf("More cleans", "Less cleans")))
    }

    @Test
    fun generateTestData() {
        val classes = UserPreferencesRepository.DEFAULT_RIDER_CLASSES.toMutableList()
        classes.addAll(classes.takeLast(3))
        classes.addAll(classes.takeLast(3))
        repeat(100) {
            println(faker.name().firstName() + " " + faker.name().lastName() + ","
                    + classes.random())
        }
    }

    private fun createScore(
        riderName: String = faker.name().firstName(),
        riderClass: String = "Novice",
        sectionsRidden: Int = numSections,
        points: Int = faker.number().numberBetween(1, 50), // exclude min/max so that we can inc/dec both,
        numCleans: Int  = faker.number().numberBetween(1, 10)
    ): RiderScoreSummary {
        return RiderScoreSummary(
            idCnt.getAndIncrement(),
            riderName,
            riderClass,
            sectionsRidden,
            points,
            numCleans
        )
    }

}