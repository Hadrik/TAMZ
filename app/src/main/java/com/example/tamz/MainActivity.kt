package com.example.tamz

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.tamz.ui.theme.TAMZTheme
import androidx.core.content.edit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TAMZTheme {
                SokobanApp()
            }
        }
    }
}

@Composable
private fun SokobanApp() {
    val context = LocalContext.current
    val scorePreferences = remember {
        context.getSharedPreferences("level_top_scores", Context.MODE_PRIVATE)
    }
    val statePreferences = remember {
        context.getSharedPreferences("level_state", Context.MODE_PRIVATE)
    }
    val levels = remember {
        loadLevels(
            path = "levels/minicosmos.txt",
            context = context,
            savedState = { name, baseLevel ->
                val savedTiles = statePreferences.getString("state_$name", null)
                val restoredLevel = savedTiles
                    ?.takeIf { it.length == baseLevel.size }
                    ?.mapNotNull { savedTile ->
                        TileType.entries.firstOrNull { it.tile == savedTile }
                    }
                    ?.takeIf { it.size == baseLevel.size }
                    ?.toTypedArray()

                SavedLevelState(
                    moves = statePreferences.getInt("moves_$name", 0),
                    level = restoredLevel
                )
            }
        ).onEach { level ->
            level.onStateChanged = {
                statePreferences.edit {
                    putInt("moves_${level.name}", level.moves)
                    putString(
                        "state_${level.name}",
                        level.currentLevel.joinToString("") { tile -> tile.tile.toString() }
                    )
                }
            }
        }
    }
    val topScores = remember {
        mutableStateMapOf<String, Int>().apply {
            levels.forEach { level ->
                val score = scorePreferences.getInt(level.name, -1)
                if (score >= 0) put(level.name, score)
            }
        }
    }
    val tileBitmap = remember { TileBitmap(context) }
    var selectedLevel by remember { mutableStateOf<LevelData?>(null) }

    if (selectedLevel == null) {
        LevelList(
            levels = levels,
            topScores = topScores,
            tileBitmap = tileBitmap,
            onLevelSelected = { selectedLevel = it }
        )
    } else {
        LevelDetail(
            level = selectedLevel!!,
            tileBitmap = tileBitmap,
            onBack = { selectedLevel = null },
            onGameFinished = { moves ->
                val levelName = selectedLevel!!.name
                val previousBest = topScores[levelName]
                if (previousBest == null || moves < previousBest) {
                    topScores[levelName] = moves
                    scorePreferences.edit { putInt(levelName, moves) }
                }
            }
        )
    }
}

@Composable
private fun LevelList(
    levels: List<LevelData>,
    topScores: Map<String, Int>,
    tileBitmap: TileBitmap,
    onLevelSelected: (LevelData) -> Unit
) {
    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(levels) { level ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLevelSelected(level) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        GameScreen(
                            levelData = level,
                            tileBitmap = tileBitmap,
                            preview = true,
                            onGameFinished = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(level.width.toFloat() / level.height)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(level.name, style = MaterialTheme.typography.titleMedium)
                            topScores[level.name]?.let { score ->
                                Text("Nejlepší skóre: $score")
                            }
                            Text("Tahů: ${level.moves}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelDetail(
    level: LevelData,
    tileBitmap: TileBitmap,
    onBack: () -> Unit,
    onGameFinished: (Int) -> Unit
) {
    var showWinDialog: Boolean by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("Zpět")
                }
                Text(
                    text = level.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text("Moves: ${level.moves}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            GameScreen(
                levelData = level,
                tileBitmap = tileBitmap,
                preview = false,
                onGameFinished = {
                    showWinDialog = true
                    onGameFinished(level.moves)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(level.width.toFloat() / level.height)
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = level::reset) {
                Text("Reset")
            }

            if (showWinDialog) {
                AlertDialog(
                    onDismissRequest = { showWinDialog = false },
                    title = { Text("Výhra!") },
                    text = { Text("Dobrá práce! Vyhrál jsi v ${level.moves} krocích.") },
                    confirmButton = {
                        TextButton(onClick = { showWinDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}
