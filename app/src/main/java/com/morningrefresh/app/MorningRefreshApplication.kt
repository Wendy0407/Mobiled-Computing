package com.morningrefresh.app

import android.app.Application
import com.morningrefresh.app.alarm.AlarmScheduler
import com.morningrefresh.app.data.AppDatabase
import com.morningrefresh.app.data.MorningRepository
import com.morningrefresh.app.data.SettingsStore

class MorningRefreshApplication : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: MorningRepository
        private set
    lateinit var settingsStore: SettingsStore
        private set
    lateinit var alarmScheduler: AlarmScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.create(this)
        repository = MorningRepository(database)
        settingsStore = SettingsStore(this)
        alarmScheduler = AlarmScheduler(this)
    }
}
