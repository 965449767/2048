package com.example.a2046.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a2046.data.Direction
import com.example.a2046.data.GameState
import com.example.a2046.data.MoveDetail
import com.example.a2046.data.copyBoard
import com.example.a2046.ui.theme.Bg
import com.example.a2046.ui.theme.BoardBg
import com.example.a2046.ui.theme.BtnBg
import com.example.a2046.ui.theme.EmptyTile
import com.example.a2046.ui.theme.ScoreBg
import com.example.a2046.ui.theme.TextDark
import com.example.a2046.ui.theme.TextLight
import com.example.a2046.vm.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SLIDE_MS = 110
private const val SPAWN_MS = 110
private const val SETTLE_DELAY_MS = 130L
private const val POP_DOWN_MS = 90
private const val POP_SCALE_MAX = 1.05f

fun tileColor(v: Int): Color = when (v) {
    0 -> EmptyTile
    2 -> Color(0xFFEEE4DA)
    4 -> Color(0xFFEDE0C8)
    8 -> Color(0xFFF2B179)
    16 -> Color(0xFFF59563)
    32 -> Color(0xFFF67C5F)
    64 -> Color(0xFFF65E3B)
    128 -> Color(0xFFEDCF72)
    256 -> Color(0xFFEDCC61)
    512 -> Color(0xFFEDC850)
    1024 -> Color(0xFFEDC53F)
    2048 -> Color(0xFFEDC22E)
    else -> Color(0xFF3C3A32)
}

fun tileTextColor(v: Int): Color = if (v <= 4) TextDark else TextLight

@Composable
fun ScoreBox(title: String, value: Long) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ScoreBg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Color(0xFFEEE4DA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value.toString(), color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ThemedTextButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = BtnBg)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GameScreen(vm: GameViewModel) {
    val state by vm.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    var showWin by remember { mutableStateOf(false) }
    var showOver by remember { mutableStateOf(false) }
    var showNewGameConfirm by remember { mutableStateOf(false) }
    var gainedPopup by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.won, state.keepPlaying) {
        if (state.won && !state.keepPlaying) showWin = true
    }
    LaunchedEffect(state.over, state.won, state.keepPlaying) {
        if (state.over && !(state.won && state.keepPlaying)) showOver = true
    }
    LaunchedEffect(Unit) {
        vm.gainedEvents.collect { gained ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            gainedPopup = gained
            delay(600)
            gainedPopup = null
        }
    }

    fun onNewGameClick() {
        if (state.score > 0 && !state.over) showNewGameConfirm = true
        else {
            vm.newGame()
            showWin = false
            showOver = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("2048", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScoreBox("SCORE", state.score)
                    ScoreBox("BEST", state.best)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("滑动合并，合成 2048 方块！", color = TextDark, fontSize = 14.sp)
                Button(
                    onClick = { onNewGameClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = BtnBg, contentColor = TextLight),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("新游戏", fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Board(state = state, onMove = { vm.move(it) })
                    val popupAlpha by animateFloatAsState(
                        targetValue = if (gainedPopup != null) 1f else 0f,
                        animationSpec = tween(200),
                        label = "popupAlpha"
                    )
                    if (popupAlpha > 0.01f) {
                        Text(
                            text = "+${gainedPopup ?: 0}",
                            color = Color(0xFFF67C5F),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .graphicsLayer { alpha = popupAlpha }
                        )
                    }
                }
            }

            Text(
                "提示：向四个方向滑动，相同数字会合并。合成 2048 即获胜。",
                color = TextDark.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }

        if (showNewGameConfirm) {
            AlertDialog(
                onDismissRequest = { showNewGameConfirm = false },
                title = { Text("重开一局？", fontWeight = FontWeight.Bold) },
                text = { Text("当前进度将被清除。") },
                confirmButton = {
                    ThemedTextButton("确定") {
                        showNewGameConfirm = false
                        vm.newGame()
                        showWin = false
                        showOver = false
                    }
                },
                dismissButton = {
                    ThemedTextButton("取消") { showNewGameConfirm = false }
                }
            )
        }
        if (showWin && state.won && !state.keepPlaying) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("恭喜！", fontWeight = FontWeight.Bold) },
                text = { Text("你合成了 2048 方块！\n是否继续挑战更高？") },
                confirmButton = {
                    ThemedTextButton("继续") { vm.keepPlaying(); showWin = false }
                },
                dismissButton = {
                    ThemedTextButton("再来一局") {
                        vm.newGame()
                        showWin = false
                    }
                }
            )
        }
        if (showOver && state.over && !(state.won && state.keepPlaying)) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("游戏结束", fontWeight = FontWeight.Bold) },
                text = { Text("已无可移动，得分 ${state.score}") },
                confirmButton = {
                    ThemedTextButton("再来一局") {
                        vm.newGame()
                        showOver = false
                    }
                }
            )
        }
    }
}

private class AnimTile(
    val id: Long,
    var row: Int,
    var col: Int,
    var value: Int,
    val x: Animatable<Float, AnimationVector1D>,
    val y: Animatable<Float, AnimationVector1D>,
    val scale: Animatable<Float, AnimationVector1D>,
    val alpha: Animatable<Float, AnimationVector1D>
)

private class Pending(
    val finalBoard: Array<IntArray>,
    val mergedCells: List<Pair<Int, Int>>,
    val spawnCell: Pair<Int, Int>?
)

@Composable
fun Board(state: GameState, onMove: (Direction) -> Unit) {
    var totalDragX by remember { mutableStateOf(0f) }
    var totalDragY by remember { mutableStateOf(0f) }
    val threshold = with(LocalDensity.current) { 60.dp.toPx() }
    val gap = 8.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(BoardBg)
            .padding(8.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { totalDragX = 0f; totalDragY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    },
                    onDragEnd = {
                        if (abs(totalDragX) > abs(totalDragY)) {
                            if (abs(totalDragX) > threshold) {
                                if (totalDragX > 0) onMove(Direction.RIGHT) else onMove(Direction.LEFT)
                            }
                        } else {
                            if (abs(totalDragY) > threshold) {
                                if (totalDragY > 0) onMove(Direction.DOWN) else onMove(Direction.UP)
                            }
                        }
                        totalDragX = 0f
                        totalDragY = 0f
                    }
                )
            }
    ) {
        val density = LocalDensity.current
        val gapPx = with(density) { gap.toPx() }
        val innerPx = with(density) { maxWidth.toPx() }
        val cell = with(density) { ((innerPx - 3 * gapPx) / 4f).toDp() }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            for (i in 0..3) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    for (j in 0..3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmptyTile)
                        )
                    }
                }
            }
        }

        TileLayer(
            state = state,
            cell = cell,
            gap = gap,
            modifier = Modifier.matchParentSize()
        )
    }
}

@Composable
fun TileLayer(
    state: GameState,
    cell: Dp,
    gap: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cellPx = density.run { cell.toPx() }
    val gapPx = density.run { gap.toPx() }

    val tiles = remember { mutableStateListOf<AnimTile>() }
    var nextId by remember { mutableStateOf(0L) }
    var pending by remember { mutableStateOf<Pending?>(null) }

    val scope = rememberCoroutineScope()

    fun rebuild(board: Array<IntArray>, mergedCells: List<Pair<Int, Int>>, spawnCell: Pair<Int, Int>?, animate: Boolean) {
        tiles.clear()
        val cellList = mutableListOf<Triple<Int, Int, Int>>()
        for (i in 0..3) for (j in 0..3) {
            val v = board[i][j]
            if (v != 0) cellList.add(Triple(i, j, v))
        }
        cellList.sortBy { (i, j, _) ->
            val isSpawn = spawnCell != null && spawnCell.first == i && spawnCell.second == j
            val isMerged = mergedCells.any { it.first == i && it.second == j }
            when {
                isMerged -> 1
                isSpawn -> 2
                else -> 0
            }
        }
        for ((i, j, v) in cellList) {
            val id = nextId++
            val isSpawn = spawnCell != null && spawnCell.first == i && spawnCell.second == j
            val hideSpawn = isSpawn && animate
            val t = AnimTile(
                id = id,
                row = i,
                col = j,
                value = v,
                x = Animatable(j * (cellPx + gapPx)),
                y = Animatable(i * (cellPx + gapPx)),
                scale = Animatable(if (hideSpawn) 0.85f else 1f),
                alpha = Animatable(if (hideSpawn) 0f else 1f)
            )
            tiles.add(t)
            if (hideSpawn) {
                scope.launch {
                    t.alpha.animateTo(1f, tween(SPAWN_MS))
                    t.scale.animateTo(1f, tween(SPAWN_MS))
                }
            }
        }
        if (animate) {
            for (cellPos in mergedCells) {
                val nt = tiles.firstOrNull { it.row == cellPos.first && it.col == cellPos.second }
                if (nt != null) {
                    scope.launch {
                        nt.scale.snapTo(POP_SCALE_MAX)
                        nt.scale.animateTo(1f, tween(POP_DOWN_MS))
                    }
                }
            }
        }
    }

    LaunchedEffect(state.board) {
        val boardSnapshot = copyBoard(state.board)
        val detail = state.lastMove
        if (detail == null) {
            pending = null
            rebuild(boardSnapshot, emptyList(), null, animate = false)
            return@LaunchedEffect
        }

        pending?.let { rebuild(it.finalBoard, it.mergedCells, it.spawnCell, animate = false) }
        pending = null

        val posMap = tiles.associateBy { it.row to it.col }
        var defensive = false
        val mergedTargets = mutableListOf<Pair<Int, Int>>()
        for (m in detail.moves) {
            val t = posMap[m.fromRow to m.fromCol]
            if (t == null) {
                defensive = true
                break
            }
            t.row = m.toRow
            t.col = m.toCol
            launch {
                t.x.animateTo(m.toCol * (cellPx + gapPx), tween(SLIDE_MS))
            }
            launch {
                t.y.animateTo(m.toRow * (cellPx + gapPx), tween(SLIDE_MS))
            }
            if (m.merging) {
                val key = m.toRow to m.toCol
                if (!mergedTargets.contains(key)) mergedTargets.add(key)
            }
        }

        if (defensive) {
            rebuild(boardSnapshot, emptyList(), null, animate = false)
            return@LaunchedEffect
        }

        val p = Pending(boardSnapshot, mergedTargets, detail.spawn)
        pending = p

        delay(SETTLE_DELAY_MS)
        rebuild(boardSnapshot, mergedTargets, detail.spawn, animate = true)
        pending = null
    }

    Box(modifier = modifier) {
        for (t in tiles) {
            AnimTileView(t = t, cell = cell)
        }
    }
}

@Composable
private fun AnimTileView(t: AnimTile, cell: Dp) {
    Box(
        modifier = Modifier
            .offset { IntOffset(t.x.value.roundToInt(), t.y.value.roundToInt()) }
            .size(cell)
            .graphicsLayer {
                alpha = t.alpha.value
                scaleX = t.scale.value
                scaleY = t.scale.value
            }
            .clip(RoundedCornerShape(6.dp))
            .background(tileColor(t.value)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = t.value.toString(),
            fontSize = if (t.value >= 8192) 20.sp else if (t.value >= 1024) 24.sp else 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = tileTextColor(t.value)
        )
    }
}
