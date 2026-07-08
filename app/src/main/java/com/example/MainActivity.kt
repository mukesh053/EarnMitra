package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.MainLayout
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: AppViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    // Register Global Uncaught Exception Handler
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      try {
        val sharedPrefs = getSharedPreferences("earnmitra_crash_report", Context.MODE_PRIVATE)
        val stackTraceString = android.util.Log.getStackTraceString(throwable)
        sharedPrefs.edit().putString("last_crash", stackTraceString).commit()
      } catch (e: Exception) {
        e.printStackTrace()
      }
      defaultHandler?.uncaughtException(thread, throwable)
    }

    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val sharedPrefs = getSharedPreferences("earnmitra_crash_report", Context.MODE_PRIVATE)
    val lastCrash = sharedPrefs.getString("last_crash", null)

    if (lastCrash != null) {
      setContent {
        MyApplicationTheme {
          Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            CrashDiagnosticsScreen(
              crashLog = lastCrash,
              onClear = {
                sharedPrefs.edit().remove("last_crash").commit()
                recreate()
              },
              modifier = Modifier.padding(innerPadding)
            )
          }
        }
      }
    } else {
      setContent {
        MyApplicationTheme {
          Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            MainLayout(
              viewModel = viewModel,
              modifier = Modifier.padding(innerPadding)
            )
          }
        }
      }
    }
  }
}

@Composable
fun CrashDiagnosticsScreen(
  crashLog: String,
  onClear: () -> Unit,
  modifier: Modifier = Modifier
) {
  val clipboardManager = LocalClipboardManager.current
  
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0F172A)) // Dark slate background
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "EarnMitra - નિદાન અહેવાલ (Diagnostics)",
      color = Color(0xFFF97316), // Orange accent
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold
    )
    
    Text(
      text = "એપ્લિકેશન છેલ્લી વખતે બંધ થઈ ગઈ હતી. કૃપા કરીને આ ભૂલનો સ્ક્રીનશોટ અથવા ટેક્સ્ટ કોપી કરીને સપોર્ટ ટીમને મોકલો જેથી અમે તેને તરત જ સુધારી શકીએ:",
      color = Color(0xFF94A3B8),
      fontSize = 12.sp,
      lineHeight = 18.sp
    )

    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .background(Color(0xFF1E293B))
        .padding(12.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = crashLog,
          color = Color(0xFFEF4444), // Red text for error
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(
        onClick = {
          clipboardManager.setText(AnnotatedString(crashLog))
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
        modifier = Modifier.weight(1f)
      ) {
        Text("નકલ કરો / Copy")
      }

      Button(
        onClick = onClear,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
        modifier = Modifier.weight(1f)
      ) {
        Text("ચાલુ કરો / Reset & Retry")
      }
    }
  }
}
