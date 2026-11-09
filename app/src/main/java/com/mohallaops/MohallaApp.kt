package com.mohallaops

import android.app.Application
import com.mohallaops.ai.IncidentTriageService
import com.mohallaops.ai.RequestExtractionService
import com.mohallaops.data.AppDatabase
import com.mohallaops.data.DrillRepository

/**
 * RELIEFGRID Application.
 * Configures database, repository and AI services (on-device + optional Gemini proxy).
 */
class MohallaApp : Application() {
    lateinit var repo: DrillRepository
        private set

    val extractor: RequestExtractionService by lazy { RequestExtractionService() }
    val triageService: IncidentTriageService by lazy { IncidentTriageService() }

    override fun onCreate() {
        super.onCreate()
        repo = DrillRepository(AppDatabase.create(this))
    }
}
