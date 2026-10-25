package com.example.a2046.data

import kotlin.random.Random

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

data class TileMove(
    val fromRow: Int,
    val fromCol: Int,
    val toRow: Int,
    val toCol: Int,
    val value: Int,
    val merging: Boolean
)

data class MoveDetail(
    val moves: List<TileMove>,
    val spawn: Pair<Int, Int>? = null
)

data class DetailedMoveResult(
    val board: Array<IntArray>,
    val moved: Boolean,
    val gained: Long,
    val moves: List<TileMove>
)

data class GameState(
    val board: Array<IntArray> = emptyBoard(),
    val score: Long = 0L,
    val best: Long = 0L,
    val won: Boolean = false,
    val over: Boolean = false,
    val keepPlaying: Boolean = false,
    val lastMove: MoveDetail? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GameState
        if (!board.contentDeepEquals(other.board)) return false
        if (score != other.score) return false
        if (best != other.best) return false
        if (won != other.won) return false
        if (over != other.over) return false
        if (keepPlaying != other.keepPlaying) return false
        if (lastMove != other.lastMove) return false
        return true
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + score.hashCode()
        result = 31 * result + best.hashCode()
        result = 31 * result + won.hashCode()
        result = 31 * result + over.hashCode()
        result = 31 * result + keepPlaying.hashCode()
        result = 31 * result + (lastMove?.hashCode() ?: 0)
        return result
    }
}

fun emptyBoard(): Array<IntArray> = Array(4) { IntArray(4) { 0 } }

fun copyBoard(b: Array<IntArray>): Array<IntArray> = Array(4) { i -> b[i].copyOf() }

fun has2048(b: Array<IntArray>): Boolean {
    for (i in 0..3) for (j in 0..3) if (b[i][j] == 2048) return true
    return false
}

fun isFull(b: Array<IntArray>): Boolean {
    for (i in 0..3) for (j in 0..3) if (b[i][j] == 0) return false
    return true
}

fun canMove(b: Array<IntArray>): Boolean {
    if (!isFull(b)) return true
    for (i in 0..3) {
        for (j in 0..2) {
            if (b[i][j] == b[i][j + 1]) return true
        }
    }
    for (j in 0..3) {
        for (i in 0..2) {
            if (b[i][j] == b[i + 1][j]) return true
        }
    }
    return false
}

fun addRandomTile(b: Array<IntArray>, random: Random = Random): Pair<Int, Int>? {
    val empty = mutableListOf<Pair<Int, Int>>()
    for (i in 0..3) for (j in 0..3) if (b[i][j] == 0) empty.add(i to j)
    if (empty.isEmpty()) return null
    val (i, j) = empty[random.nextInt(empty.size)]
    b[i][j] = if (random.nextFloat() < 0.9f) 2 else 4
    return i to j
}

fun newGameState(best: Long = 0L): GameState {
    val board = emptyBoard()
    addRandomTile(board)
    addRandomTile(board)
    return GameState(board = board, score = 0L, best = best)
}

private data class LineMove(val from: Int, val to: Int, val value: Int, val merging: Boolean)

private data class LineResult(val values: IntArray, val moves: List<LineMove>, val gained: Long)

private fun compactLine(vals: IntArray): LineResult {
    val src = mutableListOf<Int>()
    for (i in 0..3) if (vals[i] != 0) src.add(i)
    val out = IntArray(4)
    val moves = mutableListOf<LineMove>()
    var gained = 0L
    var target = 0
    var i = 0
    while (i < src.size) {
        val a = src[i]
        if (i + 1 < src.size && vals[src[i + 1]] == vals[a]) {
            val v = vals[a] * 2
            out[target] = v
            gained += v
            moves.add(LineMove(a, target, vals[a], true))
            moves.add(LineMove(src[i + 1], target, vals[src[i + 1]], true))
            i += 2
        } else {
            out[target] = vals[a]
            moves.add(LineMove(a, target, vals[a], false))
            i += 1
        }
        target++
    }
    return LineResult(out, moves, gained)
}

fun moveBoardDetailed(b: Array<IntArray>, direction: Direction): DetailedMoveResult {
    val board = emptyBoard()
    val moves = mutableListOf<TileMove>()
    var gained = 0L

    for (line in 0..3) {
        val reversedSpace = direction == Direction.RIGHT || direction == Direction.DOWN
        val vals = when (direction) {
            Direction.LEFT, Direction.RIGHT -> IntArray(4) { b[line][it] }
            Direction.UP, Direction.DOWN -> IntArray(4) { b[it][line] }
        }
        if (reversedSpace) {
            vals.reverse()
        }
        val r = compactLine(vals)
        for (m in r.moves) {
            val realFrom: Int
            val realTo: Int
            if (reversedSpace) {
                realFrom = 3 - m.from
                realTo = 3 - m.to
            } else {
                realFrom = m.from
                realTo = m.to
            }
            val tm = when (direction) {
                Direction.LEFT -> TileMove(line, realFrom, line, realTo, m.value, m.merging)
                Direction.RIGHT -> TileMove(line, realFrom, line, realTo, m.value, m.merging)
                Direction.UP -> TileMove(realFrom, line, realTo, line, m.value, m.merging)
                Direction.DOWN -> TileMove(realFrom, line, realTo, line, m.value, m.merging)
            }
            moves.add(tm)
        }
        for (t in 0..3) {
            val v = r.values[t]
            when (direction) {
                Direction.LEFT -> board[line][t] = v
                Direction.RIGHT -> board[line][3 - t] = v
                Direction.UP -> board[t][line] = v
                Direction.DOWN -> board[3 - t][line] = v
            }
        }
    }

    var moved = false
    for (m in moves) {
        if (m.fromRow != m.toRow || m.fromCol != m.toCol) moved = true
    }
    return DetailedMoveResult(board, moved, gained, moves)
}
