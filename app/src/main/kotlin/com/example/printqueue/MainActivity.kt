package com.example.printqueue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.printqueue.ui.QueueScreen
import com.example.printqueue.ui.SettingsScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MainApp()
    }
  }
}

@Composable
fun MainApp() {
  val selectedTab = remember { mutableStateOf(0) }

  Column(modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier
      .weight(1f)
      .fillMaxSize()
      .background(Color.White)) {
      when (selectedTab.value) {
        0 -> QueueScreen()
        1 -> SettingsScreen()
      }
    }

    NavigationBar {
      NavigationBarItem(
        label = { Text("Queue") },
        selected = selectedTab.value == 0,
        onClick = { selectedTab.value = 0 },
        icon = { Text("📋") }
      )
      NavigationBarItem(
        label = { Text("Settings") },
        selected = selectedTab.value == 1,
        onClick = { selectedTab.value = 1 },
        icon = { Text("⚙️") }
      )
    }
  }
}
