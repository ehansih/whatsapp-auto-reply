package com.ehansih.whatsapprules

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RuleDao {

    @Query("SELECT * FROM rules ORDER BY id DESC")
    fun getAllRules(): LiveData<List<Rule>>

    @Query("SELECT * FROM rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<Rule>

    @Insert
    suspend fun insert(rule: Rule)

    @Update
    suspend fun update(rule: Rule)

    @Delete
    suspend fun delete(rule: Rule)
}
