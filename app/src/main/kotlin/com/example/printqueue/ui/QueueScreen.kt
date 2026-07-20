package com.example.printqueue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printqueue.db.PrintQueueDatabase

@Composable
fun QueueScreen() {
  val context = LocalContext.current
  val db = PrintQueueDatabase.getInstance(context)
  val jobs = db.printJobDao().getAllJobs().collectAsState(initial = emptyList())

  Column(modifier = Modifier
    .fillMaxSize()
    .padding(16.dp)) {
    Text(
      "Print Queue",
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 16.dp)
    )

    if (jobs.value.isEmpty()) {
      Box(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
        contentAlignment = Alignment.Center) {
        Text("No print jobs", fontSize = 16.sp, color = Color.Gray)
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(jobs.value) { job ->
          JobItem(job)
          Divider(modifier = Modifier.padding(vertical = 8.dp))
        }
      }
    }
  }
}

@Composable
fun JobItem(job: com.example.printqueue.db.PrintJob) {
  Column(modifier = Modifier
    .fillMaxWidth()
    .padding(8.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(job.fileName, fontWeight = FontWeight.Bold)
        Text(job.status, fontSize = 12.sp, color = getStatusColor(job.status))
      }
      Text(job.status, fontWeight = FontWeight.Bold)
    }
    if (!job.errorMessage.isNullOrEmpty()) {
      Spacer(modifier = Modifier.height(4.dp))
      Text("Error: ${job.errorMessage}", fontSize = 12.sp, color = Color.Red)
    }
  }
}

@Composable
private fun getStatusColor(status: String): Color {
  return when (status) {
    "COMPLETED" -> Color.Green
    "FAILED" -> Color.Red
    "PRINTING" -> Color.Blue
    else -> Color.Gray
  }
}
