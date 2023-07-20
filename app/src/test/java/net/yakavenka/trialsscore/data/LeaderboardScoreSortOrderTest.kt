package net.yakavenka.trialsscore.data

import com.github.javafaker.Faker
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger


class LeaderboardScoreSortOrderTest {
    private val idCnt =  AtomicInteger()
    private val faker = Faker()
    private val classes = setOf("Novice", "Intermediate", "Advanced")
    private val numSections = 5
    private val sut = LeaderboardScoreSortOrder(classes, numSections)

    @Test
    fun sortByClass() {
        val list = mutableListOf<RiderScoreSummary>()
        list.add(createScore(riderClass = "Novice"))
        list.add(createScore(riderClass = "Advanced"))
        list.add(createScore(riderClass = "Novice"))

        val actual: List<String> = list.sortedWith(sut).map { it.riderClass }
        assertThat("Num groups", numGroups(actual), equalTo(2))
    }

    @Test
    fun sortUsesPredefinedClassSortOrder() {
        val list = mutableListOf<RiderScoreSummary>()
        classes.shuffled()
            .forEach { list.add(createScore(riderClass = it)) }

        val actual = list.sortedWith(sut).map { it.riderClass }

        Assert.assertEquals(classes.toList(), actual.toList())
    }

    @Test
    fun sortByPointsWithinAClass() {
        val list = mutableListOf<RiderScoreSummary>()
        val rider1 = createScore()
        val rider2 = rider1.copy(points = rider1.points + 2)
        list.add(rider2)
        list.add(rider1.copy(points = rider1.points + 1, riderClass = "Advanced"))
        list.add(rider1)

        val actual: List<RiderScoreSummary> = list.sortedWith(sut)

        val rider1Position = actual.indexOf(rider1)
        assertThat("Next to smaller score", actual.indexOf(rider2), equalTo(rider1Position + 1))
    }

    @Test
    fun sortByFinishedRider() {
        val list = mutableListOf<RiderScoreSummary>()
        val rider1 = createScore(sectionsRidden = numSections)
        val rider2 = rider1.copy(
            riderId = idCnt.incrementAndGet(),
            sectionsRidden = 0,
            points = rider1.points - 1)
        list.add(rider2)
        list.add(rider1.copy(riderId = idCnt.incrementAndGet(), riderClass = "Advanced"))
        list.add(rider1)

        val actual: List<RiderScoreSummary> = list.sortedWith(sut)

        val rider1Position = actual.indexOf(rider1)
        assertThat("Not finished after finished", actual.indexOf(rider2), equalTo(rider1Position + 1))
    }

    @Test
    fun sortByNameWithinNotFinished() {
        val list = mutableListOf<RiderScoreSummary>()
        val rider1 = createScore(sectionsRidden = 0)
        val rider2 = rider1.copy(riderId = idCnt.incrementAndGet(), riderName = rider1.riderName + "z")
        list.add(rider2)
        list.add(rider1.copy(riderId = idCnt.incrementAndGet(), riderClass = "Advanced"))
        list.add(rider1)

        val actual: List<RiderScoreSummary> = list.sortedWith(sut)

        val rider1Position = actual.indexOf(rider1)
        assertThat("Name sort", actual.indexOf(rider2), equalTo(rider1Position + 1))
    }

    @Test
    fun sortByCleansOnSamePoints() {
        val list = mutableListOf<RiderScoreSummary>()
        val rider1 = createScore()
        val rider2 = rider1.copy(numCleans = rider1.numCleans - 1)
        list.add(rider2)
        list.add(rider1.copy(riderId = idCnt.incrementAndGet(), riderClass = "Advanced"))
        list.add(rider1)

        val actual: List<RiderScoreSummary> = list.sortedWith(sut)

        val rider1Position = actual.indexOf(rider1)
        assertThat("More cleans in better", actual.indexOf(rider2), equalTo(rider1Position + 1))
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
        sectionsRidden: Int = SectionScore.Set.TOTAL_SECTIONS,
        totalSections: Int = SectionScore.Set.TOTAL_SECTIONS,
        points: Int = faker.number().numberBetween(1, 50), // exclude min/max so that we can inc/dec both,
        numCleans: Int  = faker.number().numberBetween(1, 10)
    ): RiderScoreSummary {
        return RiderScoreSummary(
            idCnt.getAndIncrement(),
            riderName,
            riderClass,
            sectionsRidden,
            points,
            numCleans,
            totalSections
        )
    }

    private fun numGroups(actual: List<String>): Int {
        var result = 0
        var lastValue: String? = null
        for (entry in actual) {
            if (!entry.equals(lastValue)) result ++
            lastValue = entry
        }
        return result
    }

}
