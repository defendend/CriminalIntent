package com.defendend.criminalintent

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.defendend.criminalintent.database.CrimeDatabase
import com.defendend.criminalintent.database.migration_1_2
import com.defendend.criminalintent.database.migration_2_3
import java.io.File
import java.util.*
import java.util.concurrent.Executors

private const val DATABASE_NAME = "crime-database"

class CrimeRepository private constructor(context: Context) {

    private val dataBase: CrimeDatabase = Room.databaseBuilder(
        context.applicationContext,
        CrimeDatabase::class.java,
        DATABASE_NAME
    ).addMigrations(migration_1_2, migration_2_3)
        .build()

    private val crimeDao = dataBase.crimeDao()
    private val executor = Executors.newSingleThreadExecutor()
    private val fileDir = context.applicationContext.filesDir

    fun getCrimes(): LiveData<List<Crime>> = crimeDao.getCrimes()

    fun getCrime(id: UUID): LiveData<Crime?> = crimeDao.getCrime(id)

    fun updateCrime(crime: Crime) {
        executor.execute {
            crimeDao.updateCrime(crime)
        }
    }

    fun addCrime(crime: Crime) {
        executor.execute {
            crimeDao.addCrime(crime)
        }
    }

    fun getPhotoFile(crime: Crime): File = File(fileDir, crime.photoFileName)

    companion object {
        private var INSTANCE: CrimeRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = CrimeRepository(context)
            }
        }

        fun get(): CrimeRepository {
            return INSTANCE ?: throw IllegalStateException("Crime repository must be initialized")
        }
    }
}