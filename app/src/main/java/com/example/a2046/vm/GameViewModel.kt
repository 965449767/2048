package com.example.a2046.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a2046.data.Direction
import com.example.a2046.data.GameRepo
import com.example.a2046.data.GameState
import com.example.a2046.data.addRandomTile
import com.example.a2046.data.canMove
import com.example.a2046.data.copyBoard
import com.example.a2046.data.has2048
import com.example.a2046.data.MoveDetail
import com.example.a2046.data.moveBoardDetailed
import com.example.a2046.data.newGameState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = GameRepo(app)
    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()
    val gainedEvents = MutableSharedFlow<Long>(extraBufferCapacity = 16)
    private val random = Random(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            val saved = repo.loadOnce()
            if (saved != null && saved.board != null) {
                val board = copyBoard(saved.board)
                val over = !canMove(board)
                _state.value = GameState(
                    board = board,
                    score = saved.score,
                    best = saved.best,
                    won = saved.won,
                    over = over,
                    keepPlaying = saved.keepPlaying
                )
            } else {
                _state.value = newGameState(0L)
            }
            repo.bestFlow.collect { b ->
                _state.update { s -> s.copy(best = maxOf(b, s.best, s.score)) }
            }
        }
    }

    fun newGame() {
        viewModelScope.launch {
            val s = newGameState(_state.value.best)
            _state.value = s
            repo.save(s)
        }
    }

    fun move(dir: Direction) {
        val current = _state.value
        if (current.over && !(current.won && current.keepPlaying)) return
        if (current.won && !current.keepPlaying) return

        val res = moveBoardDetailed(current.board, dir)
        if (!res.moved) return

        val newBoard = copyBoard(res.board)
        val spawn = addRandomTile(newBoard, random)
        val newScore = current.score + res.gained
        val newBest = maxOf(current.best, newScore)
        val nowWon = current.won || has2048(newBoard)
        val nowOver = !canMove(newBoard)

        val next = current.copy(
            board = newBoard,
            score = newScore,
            best = newBest,
            won = nowWon,
            over = nowOver,
            keepPlaying = if (nowWon && !current.won) false else current.keepPlaying,
            lastMove = MoveDetail(res.moves, spawn)
        )
        _state.value = next
        viewModelScope.launch { repo.save(next) }
        if (res.gained > 0) {
            gainedEvents.tryEmit(res.gained)
        }
    }

    fun keepPlaying() {
        _state.update { it.copy(keepPlaying = true) }
        viewModelScope.launch { repo.save(_state.value) }
    }
}
