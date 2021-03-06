package me.proxer.app.chat.sync

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import me.proxer.app.chat.LocalConference
import me.proxer.app.chat.LocalMessage
import me.proxer.app.util.converter.RoomConverters

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
@Database(entities = [(LocalConference::class), (LocalMessage::class)], version = 1, exportSchema = false)
@TypeConverters(RoomConverters::class)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun dao(): ChatDao
}
