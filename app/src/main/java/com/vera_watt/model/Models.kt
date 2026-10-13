package com.vera_watt.model

// Ajusta os preços com os do teu comercializador (€/kWh)
enum class Tariff(val label: String, val price: Double) {
    VAZIO("Vazio", 0.10), CHEIAS("Cheias", 0.17), PONTA("Ponta", 0.24)
}

enum class InvoiceStatus(val label: String) { PAID("Paga"), PENDING("Pendente"), OVERDUE("Em atraso") }

data class Invoice(
    val id: Int, val month: String,
    val energy: Double, val power: Double, val taxes: Double,
    val status: InvoiceStatus
) { val total: Double get() = energy + power + taxes }

data class EcoAction(val id: Int, val title: String, val points: Int, val monthlySaving: Double, val done: Boolean = false)

data class Bar(val label: String, val consumption: Float, val solar: Float)
