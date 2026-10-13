package com.vera_watt.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vera_watt.ui.theme.*
import com.vera_watt.viewmodel.VeraWattViewModel

@Composable
fun AnalyticsScreen(vm: VeraWattViewModel) {
    val price = 0.15f
    val k = if (vm.showCost) price else 1f
    val unit = if (vm.showCost) "€" else "kWh"
    val bars = vm.bars()
    var selected by remember(vm.period) { mutableStateOf(-1) }
    val maxV = bars.maxOf { maxOf(it.consumption, it.solar) } * k
    val selfSuff = bars.sumOf { minOf(it.solar, it.consumption).toDouble() } / bars.sumOf { it.consumption.toDouble() } * 100

    val glassColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val glassBorder = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    val glassShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Semanal", "Mensal", "Anual").forEachIndexed { i, l ->
                FilterChip(selected = vm.period == i, onClick = { vm.period = i }, label = { Text(l) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("kWh"); Switch(vm.showCost, { vm.showCost = it }, Modifier.padding(horizontal = 8.dp)); Text("€")
        }
        Canvas(Modifier.fillMaxWidth().height(220.dp).pointerInput(bars.size) {
            detectTapGestures { o -> selected = (o.x / (size.width / bars.size)).toInt().coerceIn(0, bars.size - 1) }
        }) {
            val gw = size.width / bars.size
            bars.forEachIndexed { i, b ->
                val hc = size.height * (b.consumption * k) / maxV
                val hs = size.height * (b.solar * k) / maxV
                drawRect(CleanBlue, Offset(i * gw + gw * .1f, size.height - hc), Size(gw * .38f, hc))
                drawRect(SolarYellow, Offset(i * gw + gw * .52f, size.height - hs), Size(gw * .38f, hs))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            bars.forEach { Text(it.label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, maxLines = 1) }
        }
        Text("Consumo   -   Producao solar", style = MaterialTheme.typography.labelMedium)
        if (selected >= 0) {
            val b = bars[selected]
            Surface(modifier = Modifier.fillMaxWidth(), color = glassColor, border = glassBorder, shape = glassShape) {
                Text("${b.label}: consumo %.1f $unit - solar %.1f $unit".format(b.consumption * k, b.solar * k),
                    Modifier.padding(16.dp))
            }
        }
        Text("Taxa de autossuficiencia: %.0f%%".format(selfSuff), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(120.dp))
    }
}
