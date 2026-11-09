package com.mohallaops

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mohallaops.data.AppDatabase
import com.mohallaops.data.DrillRepository
import com.mohallaops.domain.Actor
import com.mohallaops.domain.DemoActors
import com.mohallaops.domain.PermissionDeniedException
import com.mohallaops.domain.RuleViolationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DrillRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: DrillRepository
    private val leader = DemoActors.leader

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = DrillRepository(db)
        repo.ensureSeeded()
    }

    @After fun tearDown() { db.close() }

    @Test fun run1LeavesThreeFamiliesWaiting() = runBlocking {
        val r = repo.runDemo(leader, 1, transferFirst = false)
        assertEquals(3, r.waiting); assertEquals(2, r.northServed); assertEquals(1, r.southServed)
    }

    @Test fun run2LeavesZeroFamiliesWaiting() = runBlocking {
        val r = repo.runDemo(leader, 2, transferFirst = true)
        assertEquals(0, r.waiting); assertEquals(4, r.southServed); assertEquals(3, r.boxesTransferred)
    }

    @Test fun transferFailsWhenRoadClosed() = runBlocking {
        repo.setRoadOpen(leader, false)
        try { repo.transferBoxes(leader, "NORTH", "SOUTH", 3); fail() } catch (e: RuleViolationException) { }
        assertEquals(5, repo.boxes.first().count { it.ownerStationId == "NORTH" })
    }

    @Test fun southCannotReserveNorthBox() = runBlocking {
        try { repo.reserveBox(DemoActors.southLead, "NORTH-001", "DEL-X"); fail() } catch (e: PermissionDeniedException) { }
        assertTrue(repo.boxes.first().first { it.id == "NORTH-001" }.status == "AVAILABLE")
    }

    @Test fun residentCannotChangeRoadOrStock() = runBlocking {
        try { repo.setRoadOpen(DemoActors.resident, false); fail() } catch (e: PermissionDeniedException) { }
        try { repo.transferBoxes(DemoActors.resident, "NORTH", "SOUTH", 1); fail() } catch (e: PermissionDeniedException) { }
    }

    @Test fun volunteerCannotCloseRoad() = runBlocking {
        try { repo.setRoadOpen(DemoActors.volunteer, false); fail() } catch (e: PermissionDeniedException) { }
    }

    @Test fun assignedDeliveryIsNotCountedUntilReceiptConfirmed() = runBlocking {
        val resident = DemoActors.resident
        val req = repo.submitRequest(resident, "FAM-S1", "SOUTH", 1, "one box south", "en", 1f)
        repo.approveRequest(DemoActors.southLead, req)
        val del = repo.assignDelivery(DemoActors.southLead, req)
        assertEquals(0, repo.families.first().count { it.status == "SERVED" })
        val vol = Actor.volunteer(repo.deliveries.first().first { it.id == del }.volunteerId!!)
        repo.acceptDelivery(vol, del); repo.pickUp(vol, del, listOf("SOUTH-001")); repo.recordHandoff(vol, del)
        assertEquals(0, repo.families.first().count { it.status == "SERVED" })
        repo.confirmReceipt(resident, del)
        assertEquals(1, repo.families.first().count { it.status == "SERVED" })
        try { repo.confirmReceipt(resident, del); fail() } catch (e: RuleViolationException) { } // no duplicate confirmation
    }

    @Test fun duplicateRequestIsRefused() = runBlocking {
        repo.submitRequest(DemoActors.resident, "FAM-S1", "SOUTH", 1, "a", "en", 1f)
        try { repo.submitRequest(DemoActors.resident, "FAM-S1", "SOUTH", 1, "b", "en", 1f); fail() } catch (e: RuleViolationException) { }
    }

    @Test fun importedEventsAreDeduplicated() = runBlocking {
        val json = repo.exportEvents()
        val r = repo.importEvents(json)
        assertEquals(0, r.inserted); assertTrue(r.duplicates > 0)
    }
}
