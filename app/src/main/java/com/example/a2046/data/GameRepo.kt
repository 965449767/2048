package com.example.a2046.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.dataStore by preferencesDataStore(name = "game_2046")

object GameKeys {
    val BEST = longPreferencesKey("best")
    val BOARD_JSON = stringPreferencesKey("board_json")
    val SCORE = longPreferencesKey("score")
    val WON = booleanPreferencesKey("won")
    val KEEP_PLAYING = booleanPreferencesKey("keep_playing")
}

class GameRepo(private val context: Context) {
    val bestFlow: Flow<Long> = context.dataStore.data.map { it[GameKeys.BEST] ?: 0L }

    suspend fun save(state: GameState) {
        context.dataStore.edit { prefs ->
            prefs[GameKeys.BEST] = state.best
            prefs[GameKeys.SCORE] = state.score
            prefs[GameKeys.WON] = state.won
            prefs[GameKeys.KEEP_PLAYING] = state.keepPlaying
            prefs[GameKeys.BOARD_JSON] = boardToJson(state.board)
        }
    }

    suspend fun loadOnce(): SavedGame? {
        val prefs = context.dataStore.data.first()
        val boardJson = prefs[GameKeys.BOARD_JSON]
        val board = jsonToBoard(boardJson)
        if (board == null) return null
        val hasTile = board.any { row -> row.any { it != 0 } }
        if (!hasTile) return null
        return SavedGame(
            board = board,
            score = prefs[GameKeys.SCORE] ?: 0L,
            best = prefs[GameKeys.BEST] ?: 0L,
            won = prefs[GameKeys.WON] ?: false,
            keepPlaying = prefs[GameKeys.KEEP_PLAYING] ?: false
        )
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
    private fun boardToJson(b: Array<IntArray>): String {
        val arr = JSONArray()
        for (i in 0..3) {
            val row = JSONArray()
            for (j in 0..3) row.put(b[i][j])
            arr.put(row)
        }
        return arr.toString()
    }

    fun jsonToBoard(s: String?): Array<IntArray>? {
        if (s.isNullOrBlank()) return null
        return try {
            val arr = JSONArray(s)
            Array(4) { i ->
                val row = arr.getJSONArray(i)
                IntArray(4) { j -> row.getInt(j) }
            }
        } catch (e: Exception) {
            null
        }
    }

    data class SavedGame(
        val board: Array<IntArray>?,
        val score: Long,
        val best: Long,
        val won: Boolean,
        val keepPlaying: Boolean
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SavedGame
            if (board != null) {
                if (other.board == null) return false
                if (!board.contentDeepEquals(other.board)) return false
            } else if (other.board != null) return false
            if (score != other.score) return false
            if (best != other.best) return false
            if (won != other.won) return false
            if (keepPlaying != other.keepPlaying) return false
            return true
        }

        override fun hashCode(): Int {
            var result = board?.contentDeepHashCode() ?: 0
            result = 31 * result + score.hashCode()
            result = 31 * result + best.hashCode()
            result = 31 * result + won.hashCode()
            result = 31 * result + keepPlaying.hashCode()
            return result
        }
    }
}
