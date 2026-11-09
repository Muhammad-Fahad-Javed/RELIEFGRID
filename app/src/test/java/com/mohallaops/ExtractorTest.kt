package com.mohallaops

import com.mohallaops.ai.LocalExtractor
import com.mohallaops.ai.LocalIncidentTriage
import com.mohallaops.ai.Planner
import com.mohallaops.data.BoxEntity
import com.mohallaops.data.FamilyEntity
import com.mohallaops.data.StationEntity
import com.mohallaops.domain.S
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractorTest {
    // -------------------------------------------------------------
    // Supply Request Extraction Tests
    // -------------------------------------------------------------
    @Test fun urduExampleFromSpec() {
        val r = LocalExtractor.extract("السلام علیکم، ہمارے گھر میں چار افراد ہیں اور ہمیں ایک فوڈ باکس چاہیے۔ ہم ساؤتھ اسٹیشن کے قریب رہتے ہیں۔")
        assertEquals("ur", r.language)
        assertEquals("SOUTH", r.stationId)
        assertEquals(1, r.quantity)
        assertEquals(4, r.householdSize)
        assertTrue(r.missing.isEmpty())
    }

    @Test fun englishWithDigits() {
        val r = LocalExtractor.extract("We are 4 people and need 2 boxes near north station")
        assertEquals("en", r.language)
        assertEquals("NORTH", r.stationId)
        assertEquals(2, r.quantity)
        assertEquals(4, r.householdSize)
    }

    @Test fun missingInformationIsReported() {
        val r = LocalExtractor.extract("please help us")
        assertNull(r.quantity)
        assertNull(r.stationId)
        assertEquals(2, r.missing.size)
        assertTrue(r.confidence < 0.7f)
    }

    @Test fun bothStationsIsUnclear() {
        val r = LocalExtractor.extract("one box between north and south")
        assertNull(r.stationId)
    }

    // -------------------------------------------------------------
    // Planner Tests
    // -------------------------------------------------------------
    private val stations = listOf(
        StationEntity("NORTH", "North Station", "North Lead", 0f, 0f),
        StationEntity("SOUTH", "South Station", "South Lead", 0f, 0f)
    )
    private fun fams() = listOf("N1", "N2").map { FamilyEntity("FAM-$it", it, "NORTH", 1, S.WAITING, 0f, 0f) } +
        listOf("S1", "S2", "S3", "S4").map { FamilyEntity("FAM-$it", it, "SOUTH", 1, S.WAITING, 0f, 0f) }
    private fun boxes() = (1..5).map { BoxEntity("NORTH-00$it", "NORTH", "NORTH", S.AVAILABLE) } +
        BoxEntity("SOUTH-001", "SOUTH", "SOUTH", S.AVAILABLE)

    @Test fun plannerSuggestsMovingThreeBoxes() {
        val s = Planner.analyse(stations, boxes(), fams(), roadOpen = true)
        assertNotNull(s)
        assertEquals("NORTH", s!!.from)
        assertEquals("SOUTH", s.to)
        assertEquals(3, s.count)
        assertTrue(s.executable)
    }

    @Test fun plannerCannotTransferWhenRoadClosed() {
        val s = Planner.analyse(stations, boxes(), fams(), roadOpen = false)
        assertNotNull(s)
        assertTrue(!s!!.executable)
    }

    @Test fun plannerQuietWhenBalanced() {
        val balanced = boxes().map {
            if (it.id in listOf("NORTH-003", "NORTH-004", "NORTH-005")) it.copy(ownerStationId = "SOUTH") else it
        }
        assertNull(Planner.analyse(stations, balanced, fams(), roadOpen = true))
    }

    // -------------------------------------------------------------
    // Emergency & Disaster Incident AI Triage Tests
    // -------------------------------------------------------------
    @Test fun urduCriticalTrappedTriage() {
        val report = "چھت پر پانی چڑھ رہا ہے اور ہمارے ساتھ ایک بچہ اور بوڑھے پھنسے ہوئے ہیں، ساؤتھ اسٹیشن کے قریب فوری مدد بھیجیں۔"
        val t = LocalIncidentTriage.triage(report)
        assertEquals(S.CAT_TRAPPED, t.category)
        assertEquals(S.URGENCY_CRITICAL, t.urgency)
        assertEquals("SOUTH", t.stationId)
        assertTrue(t.requiresHumanReview)
        assertTrue(t.confidence >= 0.7f)
    }

    @Test fun englishRoadBlockageTriage() {
        val report = "The culvert road near north station is completely washed out and flooded with water."
        val t = LocalIncidentTriage.triage(report)
        assertEquals(S.CAT_ROAD, t.category)
        assertEquals("NORTH", t.stationId)
        assertTrue(t.urgency in listOf(S.URGENCY_HIGH, S.URGENCY_MEDIUM))
        assertTrue(t.requiresHumanReview)
    }

    @Test fun englishMedicalEmergencyTriage() {
        val report = "Severe medical emergency! Diabetic patient with bleeding injury near south sector."
        val t = LocalIncidentTriage.triage(report)
        assertEquals(S.CAT_MEDICAL, t.category)
        assertEquals(S.URGENCY_CRITICAL, t.urgency)
        assertEquals("SOUTH", t.stationId)
    }

    @Test fun vagueReportHighlightsMissingInformation() {
        val report = "Help there is an issue"
        val t = LocalIncidentTriage.triage(report)
        assertTrue(t.missingInfo.isNotEmpty())
        assertTrue(t.requiresHumanReview)
        assertTrue(t.confidence < 0.6f)
    }
}
