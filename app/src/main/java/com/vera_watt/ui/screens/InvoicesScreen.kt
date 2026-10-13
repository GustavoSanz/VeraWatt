package com.vera_watt.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vera_watt.model.Invoice
import com.vera_watt.model.InvoiceStatus
import com.vera_watt.ui.theme.*
import com.vera_watt.viewmodel.VeraWattViewModel

@Composable
fun InvoicesScreen(vm: VeraWattViewModel) {
    var open by remember { mutableStateOf<Invoice?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "As Minhas Faturas",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 32.dp, start = 24.dp, bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(vm.invoices) { inv ->
                InvoiceCard(inv = inv, onClick = { open = inv })
            }
        }
    }

    open?.let { shown ->
        InvoicePaymentDialog(
            inv = vm.invoices.first { it.id == shown.id },
            vm = vm,
            onDismiss = { open = null }
        )
    }
}

@Composable
fun InvoiceCard(inv: Invoice, onClick: () -> Unit) {
    val statusColor = when (inv.status) {
        InvoiceStatus.PAID -> com.vera_watt.ui.theme.PrimaryGreen
        InvoiceStatus.PENDING -> com.vera_watt.ui.theme.TertiaryWarm
        else -> com.vera_watt.ui.theme.DangerRed
    }
    val statusIcon = when (inv.status) {
        InvoiceStatus.PAID -> Icons.Default.CheckCircle
        InvoiceStatus.PENDING -> Icons.Default.Info
        else -> Icons.Default.Warning
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 400f)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Fatura de ${inv.month}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = "Status",
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = inv.status.label,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
            Text(
                text = "%.2f €".format(inv.total),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = com.vera_watt.ui.theme.PrimaryGreen
            )
        }
    }
}

@Composable
fun InvoicePaymentDialog(inv: Invoice, vm: VeraWattViewModel, onDismiss: () -> Unit) {
    var method by remember { mutableStateOf(0) }
    var phone by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    val ref = "123 456 789"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Detalhes da Fatura",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color(0xFF1F2937)
            ) 
        },
        text = {
            Column(
                modifier = Modifier.animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Breakdown of costs
                Surface(
                    color = Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Energia ativa", color = Color.Gray)
                            Text("%.2f €".format(inv.energy), fontWeight = FontWeight.Medium)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Termo fixo", color = Color.Gray)
                            Text("%.2f €".format(inv.power), fontWeight = FontWeight.Medium)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Taxas/IVA", color = Color.Gray)
                            Text("%.2f €".format(inv.taxes), fontWeight = FontWeight.Medium)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("%.2f €".format(inv.total), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = com.vera_watt.ui.theme.PrimaryGreen)
                        }
                    }
                }

                if (inv.status != InvoiceStatus.PAID) {
                    Text("Metodo de Pagamento", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) { 
                        listOf("MB WAY", "Multibanco", "Cartao").forEachIndexed { i, label ->
                            FilterChip(
                                selected = method == i,
                                onClick = { method = i },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = com.vera_watt.ui.theme.PrimaryGreen.copy(alpha = 0.1f),
                                    selectedLabelColor = com.vera_watt.ui.theme.PrimaryGreen
                                )
                            ) 
                        } 
                    }
                    
                    when (method) {
                        0 -> OutlinedTextField(
                                value = phone, 
                                onValueChange = { phone = it }, 
                                label = { Text("Telemovel") }, 
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        1 -> Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                            Text("Entidade: 21000", fontWeight = FontWeight.Medium)
                            Text("Referencia: $ref", fontWeight = FontWeight.Medium)
                            Text("Valor: %.2f €".format(inv.total), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { clip.setText(AnnotatedString(ref)) }) { 
                                Text("Copiar Referencia") 
                            }
                        }
                        else -> Text("Pagamento por cartao (Em breve)", color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            if (inv.status != InvoiceStatus.PAID) {
                var showProcessing by remember { mutableStateOf(false) }
                
                if (showProcessing) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = null,
                        text = {
                            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                CircularProgressIndicator(color = com.vera_watt.ui.theme.PrimaryGreen)
                                Text("A processar pagamento...")
                            }
                        },
                        confirmButton = {}
                    )
                    
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        vm.pay(inv.id)
                        Toast.makeText(ctx, "Pagamento confirmado com sucesso!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }

                Button(
                    onClick = {
                        if (method == 0 && phone.length < 9) {
                            Toast.makeText(ctx, "Numero de telemovel invalido", Toast.LENGTH_SHORT).show()
                        } else { 
                            showProcessing = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.vera_watt.ui.theme.PrimaryGreen)
                ) { 
                    Text(if (method == 1) "Ja paguei" else "Pagar Agora") 
                }
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { Text("Fechar") } 
        }
    )
}

