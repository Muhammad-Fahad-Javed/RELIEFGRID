package com.mohallaops

import android.app.Application
import com.mohallaops.ai.RequestExtractionService
import com.mohallaops.data.AppDatabase
import com.mohallaops.data.DrillRepository

/** Simple manual dependency wiring (see README for why this build does not use Hilt). */
class MohallaApp : Application() {
    lateinit var repo: DrillRepository
        private set
    val extractor: RequestExtractionService by lazy { RequestExtractionService() }

    override fun onCreate() {
        super.onCreate()
        repo = DrillRepository(AppDatabase.create(this))
    }
}
