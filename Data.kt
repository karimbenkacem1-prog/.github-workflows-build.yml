package com.karimbenkacem.yourtime.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// ---------- DataStore (user preferences) ----------
val Context.ds by preferencesDataStore("settings")

data class Prefs(
    val lang: String = "ar",
    val theme: Int = 0,            // 0 system, 1 light, 2 dark
    val bg: Int = 0,               // Islamic background style 0..2
    val totalOn: Boolean = true, val totalMin: Int = 240,
    val contOn: Boolean = true, val contMin: Int = 120,
    val breakMin: Int = 5,         // screen-off gap that counts as a real break
    val sound: Boolean = true, val vibrate: Boolean = true,
    val quietOn: Boolean = true, val quietStart: Int = 22 * 60, val quietEnd: Int = 5 * 60,
    val goalMin: Int = 180,
    val onboarded: Boolean = false,
    val acts: Set<String> = setOf("quran", "dhikr"),
)

object K {
    val lang = stringPreferencesKey("lang"); val theme = intPreferencesKey("theme"); val bg = intPreferencesKey("bg")
    val totalOn = booleanPreferencesKey("totalOn"); val totalMin = intPreferencesKey("totalMin")
    val contOn = booleanPreferencesKey("contOn"); val contMin = intPreferencesKey("contMin")
    val breakMin = intPreferencesKey("breakMin")
    val sound = booleanPreferencesKey("sound"); val vibrate = booleanPreferencesKey("vibrate")
    val quietOn = booleanPreferencesKey("quietOn"); val quietStart = intPreferencesKey("quietStart"); val quietEnd = intPreferencesKey("quietEnd")
    val goalMin = intPreferencesKey("goalMin"); val onboarded = booleanPreferencesKey("onboarded")
    val acts = stringSetPreferencesKey("acts")
}

class SettingsRepo(private val ctx: Context) {
    private val d = Prefs()
    val flow: Flow<Prefs> = ctx.ds.data.map { p ->
        Prefs(
            p[K.lang] ?: d.lang, p[K.theme] ?: d.theme, p[K.bg] ?: d.bg,
            p[K.totalOn] ?: d.totalOn, p[K.totalMin] ?: d.totalMin,
            p[K.contOn] ?: d.contOn, p[K.contMin] ?: d.contMin, p[K.breakMin] ?: d.breakMin,
            p[K.sound] ?: d.sound, p[K.vibrate] ?: d.vibrate,
            p[K.quietOn] ?: d.quietOn, p[K.quietStart] ?: d.quietStart, p[K.quietEnd] ?: d.quietEnd,
            p[K.goalMin] ?: d.goalMin, p[K.onboarded] ?: d.onboarded, p[K.acts] ?: d.acts,
        )
    }
    suspend fun get(): Prefs = flow.first()
    suspend fun edit(block: (MutablePreferences) -> Unit) { ctx.ds.edit { block(it) } }
}

// ---------- Room (reminder history) ----------
@Entity(tableName = "day_stat")
data class DayStat(@PrimaryKey val date: String, val reminders: Int = 0)

@Dao
interface DayDao {
    @Query("SELECT reminders FROM day_stat WHERE date = :d") suspend fun reminders(d: String): Int?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(s: DayStat)
    @Query("SELECT * FROM day_stat ORDER BY date DESC LIMIT 14") suspend fun recent(): List<DayStat>
}

@Database(entities = [DayStat::class], version = 1, exportSchema = false)
abstract class Db : RoomDatabase() {
    abstract fun dao(): DayDao
    companion object {
        @Volatile private var inst: Db? = null
        fun get(c: Context): Db = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, Db::class.java, "yt.db").build().also { inst = it }
        }
    }
}

// ---------- Static content ----------
data class Activity(val id: String, val emoji: String, val ar: String, val en: String)
val ACTIVITIES = listOf(
    Activity("quran", "📖", "اقرأ القرآن", "Read the Quran"),
    Activity("memorize", "🧠", "احفظ آية", "Memorize a verse"),
    Activity("learn", "🎓", "تعلّم شيئاً جديداً", "Learn something new"),
    Activity("book", "📚", "اقرأ كتاباً نافعاً", "Read a beneficial book"),
    Activity("lecture", "🎧", "استمع إلى محاضرة إسلامية", "Listen to an Islamic lecture"),
    Activity("walk", "🚶", "تمرّن أو امشِ قليلاً", "Exercise or take a walk"),
    Activity("family", "👨‍👩‍👧", "اجلس مع أهلك", "Spend quality time with family"),
    Activity("dhikr", "📿", "اذكر الله", "Perform dhikr"),
)

data class Daily(val ar: String, val en: String, val srcAr: String, val srcEn: String)
val DAILY = listOf(
    Daily("«نعمتان مغبون فيهما كثير من الناس: الصحة والفراغ»", "“Two blessings many people lose out on: health and free time.”", "رواه البخاري", "Narrated by al-Bukhari"),
    Daily("﴿أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ﴾", "“Verily, in the remembrance of Allah do hearts find rest.”", "الرعد ١٣:٢٨", "Ar-Ra'd 13:28"),
    Daily("«اغتنم … فراغك قبل شغلك»", "“Value … your free time before you become busy.”", "رواه الحاكم", "Narrated by al-Hakim"),
    Daily("﴿وَرَتِّلِ الْقُرْآنَ تَرْتِيلًا﴾", "“And recite the Quran with measured recitation.”", "المزمل ٧٣:٤", "Al-Muzzammil 73:4"),
)
