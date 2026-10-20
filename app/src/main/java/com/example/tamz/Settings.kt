package com.example.tamz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

enum class InputMode {
    SLIDERS,
    NUMBER_FIELDS
}

@Composable
fun Settings(
    selectedInputMode: InputMode,
    onInputModeSelected: (InputMode) -> Unit,
    tax: Double,
    onTaxChange: (Double) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = "Způsob zadávání")

        InputModeOption(
            label = "Posuvníky",
            selected = selectedInputMode == InputMode.SLIDERS,
            onClick = { onInputModeSelected(InputMode.SLIDERS) }
        )
        InputModeOption(
            label = "Číselná pole",
            selected = selectedInputMode == InputMode.NUMBER_FIELDS,
            onClick = { onInputModeSelected(InputMode.NUMBER_FIELDS) }
        )

        OutlinedTextField(
            value = tax.toString(),
            onValueChange = { value ->
                value
                    .replace(',', '.')
                    .toDoubleOrNull()
                    ?.coerceIn(0.0, 100.0)
                    ?.let(onTaxChange)
            },
            label = { Text("Daň (%)") },
            singleLine = true
        )

        Button(onClick = onDone) {
            Text(text = "Hotovo")
        }
    }
}

@Composable
private fun InputModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(text = label)
    }
}
