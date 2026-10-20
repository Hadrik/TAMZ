package com.example.tamz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aay.compose.donutChart.PieChart
import com.aay.compose.donutChart.model.PieChartData
import com.aay.compose.lineChart.LineChart
import com.aay.compose.lineChart.model.LineParameters
import com.aay.compose.lineChart.model.LineType
import kotlin.math.pow

@Composable
fun Content(
    modifier: Modifier = Modifier,
    chartType: ChartType = ChartType.LINE,
    inputMode: InputMode = InputMode.SLIDERS,
    tax: Double = 15.0
) {
    var investmentInitial by remember { mutableIntStateOf(0) }
    var investmentMonthly by remember { mutableIntStateOf(0) }
    var interest by remember { mutableDoubleStateOf(0.0) }
    var months by remember { mutableIntStateOf(0) }
    val monthly = interest / (100 * 12) * (1 - tax / 100)
    val calc: (Int) -> Double = { month ->
        val growth = (1 + monthly).pow(month)
        if (monthly == 0.0) {
            investmentInitial.toDouble() + investmentMonthly * month
        } else {
            investmentInitial * growth + investmentMonthly * (growth - 1) / monthly
        }
    }

    val totalAmount = calc(months).toInt()
    val investmentTotal = investmentInitial + investmentMonthly * months
    val interestEarned = totalAmount - investmentTotal

    Column(modifier = modifier) {
        Overview(
            totalAmount = totalAmount,
            interestEarned = interestEarned
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (chartType) {
            ChartType.LINE -> ChartLine(
                investment = investmentInitial,
                months = months,
                calc = calc
            )
            ChartType.PIE -> ChartPie(
                investment = investmentTotal,
                interest = interestEarned
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (inputMode) {
            InputMode.SLIDERS -> InputSliders(
                investment = investmentInitial,
                investmentMonthly = investmentMonthly,
                interest = interest,
                months = months,
                onInvestmentChange = { investmentInitial = it },
                onInvestmentMonthlyChange = { investmentMonthly = it },
                onInterestChange = { interest = it },
                onMonthsChange = { months = it }
            )
            InputMode.NUMBER_FIELDS -> InputNumberFields(
                investment = investmentInitial,
                investmentMonthly = investmentMonthly,
                interest = interest,
                months = months,
                onInvestmentChange = { investmentInitial = it },
                onInvestmentMonthlyChange = { investmentMonthly = it },
                onInterestChange = { interest = it },
                onMonthsChange = { months = it }
            )
        }
    }
}

@Composable
fun Overview(
    totalAmount: Int,
    interestEarned: Int,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Naspořená suma: $totalAmount",
            )
            Text(
                text = "Z toho úrok: $interestEarned",
            )
        }
    }
}

@Composable
fun ChartLine(
    investment: Int,
    months: Int,
    calc: (Int) -> Double,
    modifier: Modifier = Modifier
) {
    val totalBalance = List(months + 1) { month ->
        calc(month)
    }
    val params = listOf(
        LineParameters(
            label = "Vklad",
            data = List(months + 1) { investment.toDouble() },
            lineType = LineType.DEFAULT_LINE,
            lineColor = Color.Blue,
            lineShadow = true
        ),
        LineParameters(
            label = "Úrok",
            data = totalBalance,
            lineType = LineType.DEFAULT_LINE,
            lineColor = Color.Green,
            lineShadow = true
        )
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        LineChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp),
            linesParameters = params,
            xAxisData = List(months + 1) { month -> month.toString() },
            yAxisRange = 10,
            animateChart = true
        )
    }
}

@Composable
fun ChartPie(
    investment: Int,
    interest: Int,
    modifier: Modifier = Modifier
) {
    val params = listOf(
        PieChartData(
            partName = "Vklad",
            data = if (investment > 0) investment.toDouble() else 1.0,
            color = Color.Blue
        ),
        PieChartData(
            partName = "Úrok",
            data = if (interest > 0) interest.toDouble() else 1.0,
            color = Color.Green
        )
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        PieChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp),
            pieChartData = params
        )
    }
}

@Composable
fun InputNumberFields(
    investment: Int,
    investmentMonthly: Int,
    interest: Double,
    months: Int,
    onInvestmentChange: (Int) -> Unit,
    onInvestmentMonthlyChange: (Int) -> Unit,
    onInterestChange: (Double) -> Unit,
    onMonthsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            OutlinedTextField(
                value = investment.toString(),
                onValueChange = { onInvestmentChange(it.filter(Char::isDigit).toIntOrNull() ?: 0) },
                label = { Text("Vklad") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = investmentMonthly.toString(),
                onValueChange = { onInvestmentMonthlyChange(it.filter(Char::isDigit).toIntOrNull() ?: 0) },
                label = { Text("Měsíční vklad") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = interest.toString(),
                onValueChange = { onInterestChange(it.filter(Char::isDigit).toDoubleOrNull() ?: 0.0) },
                label = { Text("Úrok") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = months.toString(),
                onValueChange = { onMonthsChange(it.filter(Char::isDigit).toIntOrNull() ?: 0) },
                label = { Text("Období") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
        }
    }
}

@Composable
fun InputSliders(
    investment: Int,
    investmentMonthly: Int,
    interest: Double,
    months: Int,
    onInvestmentChange: (Int) -> Unit,
    onInvestmentMonthlyChange: (Int) -> Unit,
    onInterestChange: (Double) -> Unit,
    onMonthsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Vklad: $investment",
                textAlign = TextAlign.Center
            )
            Slider(
                value = investment.toFloat(),
                onValueChange = { onInvestmentChange(it.toInt()) },
                valueRange = 0f..100000f,
                steps = 49
            )

            Text(
                text = "Měsíční vklad: $investmentMonthly",
                textAlign = TextAlign.Center
            )
            Slider(
                value = investmentMonthly.toFloat(),
                onValueChange = { onInvestmentMonthlyChange(it.toInt()) },
                valueRange = 0f..10000f,
                steps = 49
            )

            Text(
                text = "Úrok: $interest",
                textAlign = TextAlign.Center
            )
            Slider(
                value = interest.toFloat(),
                onValueChange = { onInterestChange(it.toDouble()) },
                valueRange = 0f..10f,
                steps = 9
            )

            Text(
                text = "Období: $months",
                textAlign = TextAlign.Center
            )
            Slider(
                value = months.toFloat(),
                onValueChange = { onMonthsChange(it.toInt()) },
                valueRange = 0f..24f,
                steps = 23
            )
        }
    }
}
