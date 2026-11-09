package com.mohallaops.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mohallaops.MohallaApp
import com.mohallaops.ai.Extraction
import com.mohallaops.data.ImportResult
import com.mohallaops.domain.Actor
import com.mohallaops.domain.DemoActors
import com.mohallaops.domain.DrillException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val mohalla = app as MohallaApp
    private val repo = mohalla.repo

    private fun <T> kotlinx.coroutines.flow.Flow<List<T>>.state(): StateFlow<List<T>> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stations = repo.stations.state()
    val boxes = repo.boxes.state()
    val families = repo.families.state()
    val requests = repo.requests.state()
    val deliveries = repo.deliveries.state()
    val volunteers = repo.volunteers.state()
    val roads = repo.roads.state()
    val events = repo.events.state()
    val inventoryTx = repo.inventoryTx.state()
    val results = repo.results.state()
    val offline = repo.offline

    private val _actor = MutableStateFlow(DemoActors.leader)
    val actor: StateFlow<Actor> = _actor.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val aiConfigured: Boolean get() = mohalla.extractor.isAiConfigured

    init {
        viewModelScope.launch { repo.ensureSeeded() }
    }

    fun switchActor(actor: Actor) { _actor.value = actor }
    fun consumeMessage() { _message.value = null }
    fun toast(text: String) { _message.value = text }

    /** Runs a repository action; rule/permission violations become a visible message instead of a crash. */
    fun act(success: String? = null, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                if (success != null) _message.value = success
            } catch (e: CancellationException) {
                throw e
            } catch (e: DrillException) {
                _message.value = e.message
            } catch (e: Exception) {
                _message.value = "Something went wrong. Please try again."
            }
        }
    }

    // ---- request flow ----
    fun analyse(text: String, onResult: (Extraction) -> Unit) {
        viewModelScope.launch { onResult(mohalla.extractor.analyse(text)) }
    }

    fun submitRequest(familyId: String, stationId: String, qty: Int, raw: String, language: String, confidence: Float, onDone: () -> Unit) {
        act("Request submitted") {
            repo.submitRequest(actor.value, familyId, stationId, qty, raw, language, confidence)
            onDone()
        }
    }

    // ---- station / delivery actions ----
    fun approve(id: String) = act("Request approved") { repo.approveRequest(actor.value, id) }
    fun reject(id: String) = act("Request rejected") { repo.rejectRequest(actor.value, id) }
    fun assign(id: String) = act("Delivery assigned, boxes reserved") { repo.assignDelivery(actor.value, id) }
    fun accept(id: String) = act("Delivery accepted") { repo.acceptDelivery(actor.value, id) }
    fun decline(id: String) = act("Delivery declined") { repo.declineDelivery(actor.value, id, "Volunteer unavailable") }
    fun pickUp(id: String, boxes: List<String>) = act("Pickup confirmed") { repo.pickUp(actor.value, id, boxes) }
    fun handoff(id: String) = act("Handoff recorded — waiting for recipient confirmation") { repo.recordHandoff(actor.value, id) }
    fun confirmReceipt(id: String) = act("Receipt confirmed") { repo.confirmReceipt(actor.value, id) }
    fun reassign(id: String) = act("Delivery reassigned") { repo.reassignDelivery(actor.value, id) }

    // ---- drill control ----
    fun reset() = act("Drill reset") { repo.resetDrill(actor.value) }
    fun setRoad(open: Boolean) = act(if (open) "Road reopened" else "Road closed") { repo.setRoadOpen(actor.value, open) }
    fun transfer(from: String, to: String, n: Int) = act("Moved $n box(es) $from → $to") { repo.transferBoxes(actor.value, from, to, n) }
    fun setVolunteer(id: String, available: Boolean) = act { repo.setVolunteerAvailable(actor.value, id, available) }
    fun fulfil() = act("Fulfilment finished") { repo.fulfilAll(actor.value) }
    fun record(label: Int) = act("Run $label recorded") { repo.completeRun(actor.value, label) }
    fun runDemo(label: Int, transfer: Boolean) = act("Run $label finished") { repo.runDemo(actor.value, label, transfer) }

    // ---- offline / sync ----
    fun setOffline(value: Boolean) {
        repo.setOffline(value)
        _message.value = if (value) "Offline mode on — new updates will queue" else "Back online"
    }
    fun syncNow() = act("Queued updates marked as synced") { repo.syncNow() }
    suspend fun exportEvents(): String = repo.exportEvents()
    fun importEvents(json: String) {
        viewModelScope.launch {
            val r: ImportResult = repo.importEvents(json)
            _message.value = "Imported ${r.inserted}, duplicates ignored ${r.duplicates}, rejected ${r.rejected}"
        }
    }
}
