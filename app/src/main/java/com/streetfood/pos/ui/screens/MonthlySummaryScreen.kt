package com.streetfood.pos.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streetfood.pos.data.models.MonthlyExpense
import com.streetfood.pos.viewmodel.MonthlySummaryViewModel
import java.text.DateFormatSymbols

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlySummaryScreen(
    viewModel: MonthlySummaryViewModel,
    onBack: () -> Unit
) {
    val monthYear by viewModel.selectedMonthYear.collectAsStateWithLifecycle()
    val monthlyExpense by viewModel.monthlyExpense.collectAsStateWithLifecycle()
    val dailyBookTotals by viewModel.dailyBookTotals.collectAsStateWithLifecycle()
    val monthlyPOSSales by viewModel.monthlyPOSSales.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }

    val monthName = DateFormatSymbols().months[monthYear.month - 1]

    val exp1Total = dailyBookTotals.first
    val exp2Total = dailyBookTotals.second
    val fixedTotal = monthlyExpense?.totalFixed ?: 0.0
    val grandTotalExpenses = exp1Total + exp2Total + fixedTotal
    val totalSales = monthlyPOSSales
    val netProfit = totalSales - grandTotalExpenses

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(buildMonthlySummaryExcelHtml(
                    monthName, monthYear.year,
                    exp1Total, exp2Total, fixedTotal,
                    grandTotalExpenses, totalSales, netProfit,
                    monthlyExpense
                ))
            }
        }.onSuccess {
            Toast.makeText(context, "Monthly Summary exported!", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💰 Monthly Summary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        exportLauncher.launch("MonthlySummary_${monthName}_${monthYear.year}.xls")
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Export Excel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month Navigation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Default.ChevronLeft, "Previous")
                    }
                    Text(
                        "$monthName ${monthYear.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Default.ChevronRight, "Next")
                    }
                }
            }

            // ===== NET PROFIT CARD =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (netProfit >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (netProfit >= 0) "NET PROFIT" else "NET LOSS",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        "₱%,.2f".format(netProfit),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            // ===== DAILY BOOK EXPENSES (Auto) =====
            Text("From Daily Book (Auto)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseRow("📦 Expenses 1 (Ingredients)", exp1Total)
                    Divider()
                    ExpenseRow("📋 Expenses 2 (Other Daily)", exp2Total)
                    Divider()
                    ExpenseRow("📊 Total POS Sales", totalSales, isPositive = true)
                }
            }

            // ===== FIXED MONTHLY EXPENSES =====
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Fixed Monthly Expenses", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val me = monthlyExpense
                    ExpenseRow("💼 Salary", me?.salary ?: 0.0)
                    Divider()
                    ExpenseRow("🏠 Rental (Store)", me?.rentalStore ?: 0.0)
                    Divider()
                    ExpenseRow("🏠 Rental (BH)", me?.rentalBH ?: 0.0)
                    Divider()
                    ExpenseRow("⚡ Meralco (Store)", me?.meralcoStore ?: 0.0)
                    Divider()
                    ExpenseRow("⚡ Meralco (BH)", me?.meralcoBH ?: 0.0)
                    Divider()
                    ExpenseRow("💧 Maynilad (Store)", me?.mayniladStore ?: 0.0)
                    Divider()
                    ExpenseRow("💧 Maynilad (BH)", me?.mayniladBH ?: 0.0)
                    if ((me?.otherExpenses ?: 0.0) > 0.0) {
                        Divider()
                        ExpenseRow("📌 ${me?.otherLabel?.ifBlank { "Other" } ?: "Other"}", me?.otherExpenses ?: 0.0)
                    }
                }
            }

            // ===== GRAND SUMMARY =====
            Text("Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow("Total Daily Expenses", exp1Total + exp2Total, Color(0xFFD32F2F))
                    SummaryRow("Total Fixed Expenses", fixedTotal, Color(0xFFD32F2F))
                    Divider(thickness = 2.dp)
                    SummaryRow("Grand Total Expenses", grandTotalExpenses, Color(0xFFD32F2F))
                    SummaryRow("Total Sales", totalSales, Color(0xFF2E7D32))
                    Divider(thickness = 2.dp)
                    SummaryRow(
                        if (netProfit >= 0) "🟢 NET PROFIT" else "🔴 NET LOSS",
                        netProfit,
                        if (netProfit >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                        isBold = true
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Edit Fixed Expenses Dialog
    if (showEditDialog) {
        EditMonthlyExpenseDialog(
            existing = monthlyExpense,
            month = monthYear.month,
            year = monthYear.year,
            onDismiss = { showEditDialog = false },
            onSave = { expense ->
                viewModel.saveMonthlyExpense(expense) { success ->
                    if (success) Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(context, "Failed to save.", Toast.LENGTH_SHORT).show()
                }
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun ExpenseRow(label: String, amount: Double, isPositive: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            "₱%,.2f".format(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isPositive) Color(0xFF2E7D32) else Color.Unspecified
        )
    }
}

@Composable
private fun SummaryRow(label: String, amount: Double, color: Color, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Normal
        )
        Text(
            "₱%,.2f".format(amount),
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun EditMonthlyExpenseDialog(
    existing: MonthlyExpense?,
    month: Int,
    year: Int,
    onDismiss: () -> Unit,
    onSave: (MonthlyExpense) -> Unit
) {
    var salary by remember { mutableStateOf(existing?.salary?.let { if (it > 0) it.toLong().toString() else "" } ?: "") }
    var rentalStore by remember { mutableStateOf(existing?.rentalStore?.let { if (it > 0) it.toLong().toString() else "" } ?: "") }
    var rentalBH by remember { mutableStateOf(existing?.rentalBH?.let { if (it > 0) it.toLong().toString() else "" } ?: "") }
    var meralcoStore by remember { mutableStateOf(existing?.meralcoStore?.let { if (it > 0) it.toString() else "" } ?: "") }
    var meralcoBH by remember { mutableStateOf(existing?.meralcoBH?.let { if (it > 0) it.toString() else "" } ?: "") }
    var mayniladStore by remember { mutableStateOf(existing?.mayniladStore?.let { if (it > 0) it.toString() else "" } ?: "") }
    var mayniladBH by remember { mutableStateOf(existing?.mayniladBH?.let { if (it > 0) it.toString() else "" } ?: "") }
    var otherExp by remember { mutableStateOf(existing?.otherExpenses?.let { if (it > 0) it.toString() else "" } ?: "") }
    var otherLabel by remember { mutableStateOf(existing?.otherLabel ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Fixed Expenses", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExpenseField("💼 Salary", salary) { salary = it }
                ExpenseField("🏠 Rental (Store)", rentalStore) { rentalStore = it }
                ExpenseField("🏠 Rental (BH)", rentalBH) { rentalBH = it }
                ExpenseField("⚡ Meralco (Store)", meralcoStore) { meralcoStore = it }
                ExpenseField("⚡ Meralco (BH)", meralcoBH) { meralcoBH = it }
                ExpenseField("💧 Maynilad (Store)", mayniladStore) { mayniladStore = it }
                ExpenseField("💧 Maynilad (BH)", mayniladBH) { mayniladBH = it }
                ExpenseField("📌 Other Expense", otherExp) { otherExp = it }
                if (otherExp.isNotBlank()) {
                    OutlinedTextField(
                        value = otherLabel,
                        onValueChange = { otherLabel = it },
                        label = { Text("Other Label") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    MonthlyExpense(
                        month = month,
                        year = year,
                        salary = salary.toDoubleOrNull() ?: 0.0,
                        rentalStore = rentalStore.toDoubleOrNull() ?: 0.0,
                        rentalBH = rentalBH.toDoubleOrNull() ?: 0.0,
                        meralcoStore = meralcoStore.toDoubleOrNull() ?: 0.0,
                        meralcoBH = meralcoBH.toDoubleOrNull() ?: 0.0,
                        mayniladStore = mayniladStore.toDoubleOrNull() ?: 0.0,
                        mayniladBH = mayniladBH.toDoubleOrNull() ?: 0.0,
                        otherExpenses = otherExp.toDoubleOrNull() ?: 0.0,
                        otherLabel = otherLabel
                    )
                )
            }) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ExpenseField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) }
    )
}

private fun buildMonthlySummaryExcelHtml(
    monthName: String,
    year: Int,
    exp1Total: Double,
    exp2Total: Double,
    fixedTotal: Double,
    grandTotalExpenses: Double,
    totalSales: Double,
    netProfit: Double,
    me: MonthlyExpense?
): String {
    fun fmt(v: Double) = "%,.2f".format(v)
    val profitColor = if (netProfit >= 0) "#1B5E20" else "#B71C1C"
    val profitLabel = if (netProfit >= 0) "NET PROFIT" else "NET LOSS"

    return """
        <html>
        <head>
          <meta charset="utf-8" />
          <style>
            body { font-family: Calibri, Arial, sans-serif; margin: 0; }
            table { border-collapse: collapse; width: 100%; }
            td, th { border: 1px solid #999; padding: 6px 10px; font-size: 12px; }
            .no-border { border: none !important; }
            .brand-name { font-size: 26px; font-weight: bold; color: #E65100; font-family: 'Georgia', serif; }
            .brand-name-large { font-size: 32px; font-weight: bold; color: #E65100; font-family: 'Georgia', serif; }
            .sub-title { font-size: 12px; color: #555; margin-top: 2px; }
            .section-header td { background-color: #FFF3E0; font-weight: bold; color: #E65100; font-size: 13px; }
            .total-row td { font-weight: bold; border-top: 2px solid #E65100; }
            .profit-row td { font-weight: bold; font-size: 16px; color: $profitColor; border-top: 3px double #333; }
          </style>
        </head>
        <body>
          <table>
            <tr>
              <td class="no-border" colspan="3" style="text-align:center; padding-bottom:8px;">
                <div class="brand-name">ARCEO'S</div>
                <div class="brand-name-large">LUGAW HOUSE</div>
                <div class="sub-title">Monthly Summary &mdash; $monthName $year</div>
              </td>
            </tr>
            <tr><td colspan="3" class="no-border" style="height:6px;"></td></tr>

            <tr class="section-header"><td colspan="3">Daily Book Expenses (Auto)</td></tr>
            <tr><td>Expenses 1 (Ingredients)</td><td></td><td style="text-align:right;">&#8369; ${fmt(exp1Total)}</td></tr>
            <tr><td>Expenses 2 (Other Daily)</td><td></td><td style="text-align:right;">&#8369; ${fmt(exp2Total)}</td></tr>
            <tr class="total-row"><td>Subtotal Daily Expenses</td><td></td><td style="text-align:right; color:#D32F2F;">&#8369; ${fmt(exp1Total + exp2Total)}</td></tr>

            <tr><td colspan="3" class="no-border" style="height:6px;"></td></tr>
            <tr class="section-header"><td colspan="3">Fixed Monthly Expenses</td></tr>
            <tr><td>Salary</td><td></td><td style="text-align:right;">&#8369; ${fmt(me?.salary ?: 0.0)}</td></tr>
            <tr><td>Rental (Store)</td><td></td><td style="text-align:right;">&#8369; ${fmt(me?.rentalStore ?: 0.0)}</td></tr>
            <tr><td>Rental (BH)</td><td></td><td style="text-align:right;">&#8369; ${fmt(me?.rentalBH ?: 0.0)}</td></tr>
            <tr><td>Meralco (Store)</td><td></td><td style="text-align:right;">&#8369; ${fmt(me?.meralcoStore ?: 0.0)}</td></tr>
            <tr><td>Meralco (BH)</td><td></td><td style="text-align:right;">&#8369; ${fmt(me?.meralcoBH ?: 0.0)}</td></tr>
            <tr><td>Maynilad (Store)</td><td></td><td style="text-align:right;">&#8369; ${fmt(me?.mayniladStore ?: 0.0)}</td></tr>
            <tr><td>Maynilad (BH)</td><td></td><td style="text-align:right;">&#8369; ${fmt(me?.mayniladBH ?: 0.0)}</td></tr>
            ${if ((me?.otherExpenses ?: 0.0) > 0.0) "<tr><td>${me?.otherLabel?.ifBlank { "Other" } ?: "Other"}</td><td></td><td style=\"text-align:right;\">&#8369; ${fmt(me?.otherExpenses ?: 0.0)}</td></tr>" else ""}
            <tr class="total-row"><td>Subtotal Fixed Expenses</td><td></td><td style="text-align:right; color:#D32F2F;">&#8369; ${fmt(fixedTotal)}</td></tr>

            <tr><td colspan="3" class="no-border" style="height:10px;"></td></tr>
            <tr class="section-header"><td colspan="3">Summary</td></tr>
            <tr><td>Grand Total Expenses</td><td></td><td style="text-align:right; color:#D32F2F; font-weight:bold;">&#8369; ${fmt(grandTotalExpenses)}</td></tr>
            <tr><td>Total Sales (POS)</td><td></td><td style="text-align:right; color:#2E7D32; font-weight:bold;">&#8369; ${fmt(totalSales)}</td></tr>
            <tr class="profit-row"><td>$profitLabel</td><td></td><td style="text-align:right;">&#8369; ${fmt(netProfit)}</td></tr>
          </table>
        </body>
        </html>
    """.trimIndent()
}
