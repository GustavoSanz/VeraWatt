package com.vera_watt.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vera_watt.ui.theme.*
import com.vera_watt.viewmodel.VeraWattViewModel

@Composable
fun RealtimeScreen(vm: VeraWattViewModel) {
    val frac by animateFloatAsState((vm.watts / 3500f).coerceIn(0f, 1f), label = "gauge")
    val color = if (frac < .5f) lerp(Emerald, SolarYellow, frac * 2) else lerp(SolarYellow, Danger, (frac - .5f) * 2)
    val net = vm.watts - vm.solarWatts

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val glassModifier = Modifier.fillMaxWidth()
        val glassColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        val glassBorder = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        val glassShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

        Surface(modifier = glassModifier, color = glassColor, border = glassBorder, shape = glassShape) {
            Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 28f, cap = StrokeCap.Round)
                        val tl = Offset(14f, 14f)
                        val sz = Size(size.width - 28f, size.height - 28f)
                        drawArc(color = Color.Gray.copy(alpha = .25f), startAngle = 135f, sweepAngle = 270f,
                            useCenter = false, topLeft = tl, size = sz, style = stroke)
                        drawArc(color = color, startAngle = 135f, sweepAngle = 270f * frac,
                            useCenter = false, topLeft = tl, size = sz, style = stroke)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${vm.watts} W", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("%.2f kW".format(vm.watts / 1000.0))
                    }
                }
            }
        }
        
        Surface(modifier = glassModifier, color = glassColor, border = glassBorder, shape = glassShape) {
            Column(Modifier.padding(16.dp)) {
                Text("Balanco energetico", fontWeight = FontWeight.Bold)
                Text(if (net < 0) "A injetar ${-net} W na rede (credito)" else "A importar $net W da rede")
            }
        }
        Surface(modifier = glassModifier, color = glassColor, border = glassBorder, shape = glassShape) {
            Column(Modifier.padding(16.dp)) {
                Text("Tarifa em vigor", fontWeight = FontWeight.Bold)
                Text("${vm.tariff.label} - %.2f €/kWh".format(vm.tariff.price))
            }
        }
        Surface(modifier = glassModifier, color = glassColor, border = glassBorder, shape = glassShape) {
            Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Solar: ${vm.solarWatts} W")
                Text("Consumo: ${vm.watts} W")
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}
