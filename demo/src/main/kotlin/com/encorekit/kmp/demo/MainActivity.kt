package com.encorekit.kmp.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.encorekit.kmp.Encore
import com.encorekit.kmp.models.PresentationResult
import com.encorekit.kmp.setActivity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Encore.setActivity(this)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DemoScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        Encore.setActivity(null)
        super.onDestroy()
    }
}

@Composable
private fun DemoScreen() {
    val scope = rememberCoroutineScope()
    var resultText by remember { mutableStateOf("No result yet") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Encore KMP Demo",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "User: demo_user_kmp_001",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        val result = Encore.placement("demo").show()
                        resultText = when (result) {
                            is PresentationResult.Granted -> "Granted (offer: ${result.offerId})"
                            is PresentationResult.NotGranted -> "Not granted (${result.reason.value})"
                        }
                    } catch (e: Exception) {
                        resultText = "Error: ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Show Placement")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = resultText,
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                Encore.reset()
                resultText = "SDK reset"
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Reset SDK")
        }
    }
}
