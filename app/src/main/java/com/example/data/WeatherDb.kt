package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "weather_cache")
data class WeatherCache(
    @PrimaryKey val id: Int = 1,
    val latitude: Double,
    val longitude: Double,
    val lastUpdated: Long,
    val currentTemperature: Double,
    val currentWeatherCode: Int,
    val currentWindSpeed: Double,
    val dailyTimes: String, // Comma separated "2026-06-02,2026-06-03"
    val dailyTempMax: String, // Comma separated "25.4,26.1"
    val dailyTempMin: String, // Comma separated "14.2,15.1"
    val dailyWeatherCodes: String // Comma separated "0,1"
)

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_cache WHERE id = 1 LIMIT 1")
    fun getWeatherCache(): Flow<WeatherCache?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: WeatherCache)

    @Query("DELETE FROM weather_cache")
    suspend fun clearCache()
}

@Database(entities = [WeatherCache::class], version = 1, exportSchema = false)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getDatabase(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "weather_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
