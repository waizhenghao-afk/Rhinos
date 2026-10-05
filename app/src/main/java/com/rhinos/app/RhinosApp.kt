package com.rhinos.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.rhinos.app.data.db.RhinosDatabase
import com.rhinos.app.data.repository.DailyPnlRepository
import com.rhinos.app.data.repository.NoteRepository
import com.rhinos.app.data.repository.TradeRepository
import com.rhinos.app.notifications.ExpiryScheduler
import com.rhinos.app.settings.SettingsRepository

class RhinosApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ExpiryScheduler.createChannel(this)
    }
}

/**
 * Manual dependency container. Small app, no DI framework needed.
 */
class AppContainer(context: Context) {

    private val database: RhinosDatabase = Room.databaseBuilder(
        context.applicationContext,
        RhinosDatabase::class.java,
        "rhinos.db"
    ).fallbackToDestructiveMigration().build()

    val tradeRepository = TradeRepository(database.tradeDao())
    val dailyPnlRepository = DailyPnlRepository(database.dailyPnlDao())
    val noteRepository = NoteRepository(database.noteDao())
    val settingsRepository = SettingsRepository(context.applicationContext)
}
