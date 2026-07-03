package com.vibeup.android.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vibeup.android.data.local.dao.ArtistPlayCount
import com.vibeup.android.data.local.dao.DayPlayCount
import com.vibeup.android.data.local.dao.PlayHistoryDao
import com.vibeup.android.data.local.dao.SongPlayCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ListeningStats(
    val totalPlays: Int = 0,
    val totalHoursListened: Double = 0.0,
    val topSongs: List<SongPlayCount> = emptyList(),
    val topArtists: List<ArtistPlayCount> = emptyList(),
    val topAlbum: String = "",
    val peakHour: Int = -1,
    val streak: Int = 0,
    val weeklyPlays: List<DayPlayCount> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val playHistoryDao: PlayHistoryDao,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val uid get() = auth.currentUser?.uid ?: "guest_user"

    private val _stats = MutableStateFlow(ListeningStats())
    val stats: StateFlow<ListeningStats> = _stats.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _stats.value = _stats.value.copy(isLoading = true)
            try {
                val totalPlays = playHistoryDao.getTotalPlays(uid)
                val totalSeconds = playHistoryDao.getTotalListenSeconds(uid) ?: 0L
                val topSongs = playHistoryDao.getTopSongs(uid, 5)
                val topArtists = playHistoryDao.getTopArtists(uid, 5)
                val topAlbum = playHistoryDao.getTopAlbum(uid)?.artist ?: ""
                val peakHour = playHistoryDao.getPeakHour(uid)?.hour ?: -1
                val weeklyPlays = playHistoryDao.getWeeklyPlays(uid)
                val streak = calculateStreak()

                _stats.value = ListeningStats(
                    totalPlays = totalPlays,
                    totalHoursListened = totalSeconds / 3600.0,
                    topSongs = topSongs,
                    topArtists = topArtists,
                    topAlbum = topAlbum,
                    peakHour = peakHour,
                    streak = streak,
                    weeklyPlays = weeklyPlays,
                    isLoading = false
                )
            } catch (e: Exception) {
                _stats.value = _stats.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun calculateStreak(): Int {
        val days = playHistoryDao.getDistinctPlayDays(uid)
        if (days.isEmpty()) return 0

        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val todayYear = cal.get(Calendar.YEAR)

        var streak = 0
        var expectedDay = today
        var expectedYear = todayYear

        for (day in days) {
            if (day.dayOfYear == expectedDay && day.year == expectedYear) {
                streak++
                expectedDay--
                if (expectedDay == 0) {
                    expectedYear--
                    expectedDay = if (isLeapYear(expectedYear)) 366 else 365
                }
            } else break
        }
        return streak
    }

    private fun isLeapYear(year: Int) =
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

    fun formatHour(hour: Int): String {
        return when {
            hour == 0 -> "12 AM"
            hour < 12 -> "$hour AM"
            hour == 12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
    }

    fun getDayLabel(dayOfYear: Int, year: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.DAY_OF_YEAR, dayOfYear)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "Sun"
        }
    }
}