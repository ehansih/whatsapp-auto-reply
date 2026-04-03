package com.ehansih.whatsapprules

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Rule::class], version = 3, exportSchema = false)
abstract class RulesDatabase : RoomDatabase() {

    abstract fun ruleDao(): RuleDao

    companion object {
        @Volatile
        private var INSTANCE: RulesDatabase? = null

        fun getDatabase(context: Context): RulesDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    RulesDatabase::class.java,
                    "rules_database"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
