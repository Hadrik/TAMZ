package com.example.tamz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

enum class ChartType {
    LINE,
    PIE
}

@Composable
fun ChartSelection(
    selectedChartType: ChartType,
    onChartTypeSelected: (ChartType) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = "Typ grafu")

        ChartTypeOption(
            label = "Sloupcový",
            selected = selectedChartType == ChartType.LINE,
            onClick = { onChartTypeSelected(ChartType.LINE) }
        )
        ChartTypeOption(
            label = "Koláčový",
            selected = selectedChartType == ChartType.PIE,
            onClick = { onChartTypeSelected(ChartType.PIE) }
        )

        Button(onClick = onDone) {
            Text(text = "Hotovo")
        }
    }
}

@Composable
private fun ChartTypeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(text = label)
    }
}
