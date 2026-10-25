package com.example.a2046

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.a2046.ui.GameScreen
import com.example.a2046.ui.theme._2048Theme
import com.example.a2046.vm.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _2048Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: GameViewModel = viewModel()
                    GameScreen(vm = vm)
                }
            }
        }
    }
}
