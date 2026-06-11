package com.golfsupporter

import android.app.Application
import com.golfsupporter.data.repository.GameRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GolfApp : Application() {

    @Inject
    lateinit var repository: GameRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Seed the built-in penalty catalogue on first launch.
        appScope.launch { repository.ensurePenaltiesSeeded() }
    }
}
