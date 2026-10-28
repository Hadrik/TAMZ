package com.example.tamz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tamz.ui.theme.TAMZTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TAMZTheme {
                val navController = rememberNavController()
                var chartType by remember { mutableStateOf(ChartType.LINE) }
                var inputMode by remember { mutableStateOf(InputMode.NUMBER_FIELDS) }
                var tax by remember { mutableDoubleStateOf(15.0) }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    topBar = {
                        TopBar(
                            onChartTypeClick = { navController.navigate("chart") },
                            onSettingsClick = { navController.navigate("settings") },
                            onBarcodeClick = { navController.navigate("barcode") }
                        )
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("main") {
                            Content(
                                chartType = chartType,
                                inputMode = inputMode,
                                tax = tax
                            )
                        }

                        composable("settings") {
                            Settings(
                                selectedInputMode = inputMode,
                                onInputModeSelected = { inputMode = it },
                                tax = tax,
                                onTaxChange = { tax = it },
                                onDone = { navController.popBackStack() }
                            )
                        }

                        composable("chart") {
                            ChartSelection(
                                selectedChartType = chartType,
                                onChartTypeSelected = { chartType = it },
                                onDone = { navController.popBackStack() }
                            )
                        }

                        composable("barcode") {
                            BarcodeScreen(
                                onDone = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onChartTypeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBarcodeClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Spoření",
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Otevřít nabídku"
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Typ grafu") },
                    onClick = {
                        expanded = false
                        onChartTypeClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Nastavení") },
                    onClick = {
                        expanded = false
                        onSettingsClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Vytvořit čárový kód") },
                    onClick = {
                        expanded = false
                        onBarcodeClick()
                    }
                )
            }
        }
    }
}