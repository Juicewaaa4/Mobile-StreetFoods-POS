package com.streetfood.pos.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streetfood.pos.data.models.DateFilter
import com.streetfood.pos.ui.components.EmptyStateView
import com.streetfood.pos.ui.components.LoadingIndicator
import com.streetfood.pos.viewmodel.ReportRow
import com.streetfood.pos.viewmodel.ReportState
import com.streetfood.pos.viewmodel.ReportTotals
import com.streetfood.pos.viewmodel.ReportViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OrangeTotal = Color(0xFFE65100)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    reportViewModel: ReportViewModel,
    onBack: () -> Unit
) {
    val state by reportViewModel.reportState.collectAsStateWithLifecycle()
    val filter by reportViewModel.dateFilter.collectAsStateWithLifecycle()
    val customDate by reportViewModel.customDateMillis.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(buildExcelHtml(state.rows, state.totals, state.reportDate, false))
            }
        }.onSuccess {
            Toast.makeText(context, "Report downloaded.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Unable to download report.", Toast.LENGTH_SHORT).show()
        }
    }

    val exportGCashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(buildGCashExcelHtml(state.gcashRows, state.gcashTotals, state.rawGCashTransactions, state.reportDate))
            }
        }.onSuccess {
            Toast.makeText(context, "GCash Report downloaded.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Unable to download GCash report.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Report", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ReportActions(
                selectedLabel = state.reportDate.ifBlank { filter.label },
                hasRows = state.rows.isNotEmpty(),
                gcashRowCount = state.gcashRows.size,
                onPickDate = { showDatePicker = true },
                onDownload = {
                    val stamp = state.reportDate.replace("/", "-").ifBlank {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    }
                    exportLauncher.launch("zoeys-sales-report-$stamp.xls")
                },
                onDownloadGCash = {
                    val stamp = state.reportDate.replace("/", "-").ifBlank {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    }
                    exportGCashLauncher.launch("zoeys-gcash-report-$stamp.xls")
                }
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DateFilter.values().toList()) { f ->
                    FilterChip(
                        selected = customDate == null && filter == f,
                        onClick = { reportViewModel.setFilter(f) },
                        label = { Text(f.label, fontSize = 12.sp) },
                        shape = RoundedCornerShape(50.dp)
                    )
                }
            }

            when {
                state.isLoading -> LoadingIndicator("Loading report...")
                state.error != null -> EmptyStateView("", "Error", state.error ?: "")
                state.rows.isEmpty() -> EmptyStateView(
                    "",
                    "No Sales",
                    "No transactions found for ${state.reportDate.ifBlank { filter.label }}."
                )
                else -> ReportSummaryCard(
                    reportDate = state.reportDate,
                    rows = state.rows,
                    totals = state.totals
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(reportViewModel::setCustomDate)
                        showDatePicker = false
                    }
                ) { Text("Show Report") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ReportActions(
    selectedLabel: String,
    hasRows: Boolean,
    gcashRowCount: Int,
    onPickDate: () -> Unit,
    onDownload: () -> Unit,
    onDownloadGCash: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        val compact = maxWidth < 390.dp
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Showing: $selectedLabel", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onPickDate, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Date")
                    }
                    Button(onClick = onDownload, enabled = hasRows, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("All")
                    }
                    Button(
                        onClick = onDownloadGCash,
                        enabled = gcashRowCount > 0,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005CEE)) // GCash Blue
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (gcashRowCount > 0) "GCash ($gcashRowCount)" else "GCash")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Showing: $selectedLabel",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onPickDate) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Pick Date")
                }
                Button(onClick = onDownload, enabled = hasRows) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("All Excel")
                }
                Button(
                    onClick = onDownloadGCash,
                    enabled = gcashRowCount > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005CEE)) // GCash Blue
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (gcashRowCount > 0) "GCash ($gcashRowCount) Excel" else "GCash Excel")
                }
            }
        }
    }
}

@Composable
private fun ReportSummaryCard(
    reportDate: String,
    rows: List<ReportRow>,
    totals: ReportTotals
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Summary totals cards
        item {
            Text(
                "Date: $reportDate",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SummaryStatCard("Gross Sales", "₱ %,.2f".format(totals.totalGross), Color(0xFF1B5E20), Modifier.weight(1f))
                SummaryStatCard("Net Amount", "₱ %,.2f".format(totals.totalNet), OrangeTotal, Modifier.weight(1f))
                SummaryStatCard("Qty Sold", totals.totalQty.toString(), Color(0xFF1565C0), Modifier.weight(1f))
            }
        }

        // Per-product rows
        items(rows) { row ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.productName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${row.qtySold} pc(s) sold", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₱ %,.2f".format(row.grossSales), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))
                        Text("Net: ₱ %,.2f".format(row.netAmount), fontSize = 11.sp, color = OrangeTotal)
                    }
                }
            }
        }

        // Totals footer card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                border = BorderStroke(1.5.dp, OrangeTotal)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = OrangeTotal)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₱ %,.2f".format(totals.totalGross), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = OrangeTotal)
                        Text("Net ₱ %,.2f  •  %.0f%%".format(totals.totalNet, totals.avgPercentage), fontSize = 11.sp, color = OrangeTotal)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun buildExcelHtml(rows: List<ReportRow>, totals: ReportTotals, reportDate: String, isGCash: Boolean): String {
    fun esc(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    val rowHtml = rows.joinToString("") { row ->
        """
        <tr>
          <td>${esc(row.productName)}</td>
          <td style="text-align:center;">${row.qtySold}</td>
          <td style="text-align:right;">${"%,.2f".format(row.grossSales)}</td>
          <td style="text-align:right;">&#8369; ${"%,.2f".format(row.costOfSales)}</td>
          <td style="text-align:right;">&#8369; ${"%,.2f".format(row.netAmount)}</td>
          <td style="text-align:center;">${"%.0f%%".format(row.percentage)}</td>
          <td></td>
        </tr>
        """.trimIndent()
    }

    return """
        <html>
        <head>
          <meta charset="utf-8" />
          <style>
            body { font-family: Calibri, Arial, sans-serif; margin: 0; }
            table { border-collapse: collapse; width: 100%; }
            td, th { border: 1px solid #555; padding: 5px 7px; font-size: 12px; }
            .no-border { border: none !important; }
            .logo-cell { border: none !important; text-align: center; padding-bottom: 4px; }
            .brand-name {
              font-size: 26px;
              font-weight: bold;
              color: #1B5E20;
              font-family: 'Georgia', serif;
            }
            .brand-name-large {
              font-size: 32px;
              font-weight: bold;
              color: #1B5E20;
              font-family: 'Georgia', serif;
            }
            .date-cell { border: none !important; font-weight: bold; font-size: 13px; color: #000; }
            th {
              background-color: #fff;
              font-weight: bold;
              text-align: center;
              font-size: 12px;
            }
            .total-row td { color: #E65100; font-weight: bold; }
            .signatory-label { border: none !important; text-align: left; font-size: 11px; }
            .signatory-name { border: none !important; text-align: left; font-size: 11px; font-weight: bold; }
          </style>
        </head>
        <body>
          <table>
            <!-- Logo / header rows -->
            <tr>
              <td class="no-border" colspan="3"></td>
              <td class="logo-cell" colspan="4">
                <div class="brand-name">ZOEY'S</div>
                <div class="brand-name-large">STREET FOODS</div>
              </td>
            </tr>
            <tr>
              <td class="date-cell" colspan="2">Date: ${esc(reportDate)}</td>
              <td class="no-border" colspan="5"></td>
            </tr>
            <tr><td colspan="7" class="no-border" style="height:6px;"></td></tr>
            <!-- Column headers -->
            <tr>
              <th>PRODUCT</th>
              <th>SOLD(PC'S)</th>
              <th>GROSS SALES</th>
              <th>COST OF SALES</th>
              <th>NET AMOUNT</th>
              <th>PERCENTAGE</th>
              <th>REMARKS</th>
            </tr>
            $rowHtml
            <!-- Totals -->
            <tr class="total-row">
              <td>Total</td>
              <td style="text-align:center;">${totals.totalQty}</td>
              <td style="text-align:right;">${"%,.2f".format(totals.totalGross)}</td>
              <td style="text-align:right;">&#8369; ${"%,.2f".format(totals.totalCost)}</td>
              <td style="text-align:right;">&#8369; ${"%,.2f".format(totals.totalNet)}</td>
              <td style="text-align:center;">${"%.0f%%".format(totals.avgPercentage)}</td>
              <td></td>
            </tr>
            <!-- Spacer -->
            <tr><td colspan="7" class="no-border" style="height:12px;"></td></tr>
            <tr><td colspan="7" class="no-border" style="height:12px;"></td></tr>
            <!-- Signatories -->
            <tr>
              <td class="no-border" colspan="4"></td>
              <td class="signatory-label">Prepared By:</td>
              <td class="signatory-name" colspan="2">Kenneth Francisco</td>
            </tr>
            <tr>
              <td class="no-border" colspan="4"></td>
              <td class="signatory-label">Reviewed By:</td>
              <td class="signatory-name" colspan="2">Judy Peralta</td>
            </tr>
            <tr>
              <td class="no-border" colspan="4"></td>
              <td class="signatory-label">Checked By:</td>
              <td class="signatory-name" colspan="2">Trecia E. De Jesus</td>
            </tr>
            <tr>
              <td class="no-border" colspan="4"></td>
              <td class="signatory-label">Noted By:</td>
              <td class="signatory-name" colspan="2">Enrique DM Martinez</td>
            </tr>
          </table>
        </body>
        </html>
    """.trimIndent()
}

private fun buildGCashExcelHtml(
    rows: List<ReportRow>,
    totals: ReportTotals,
    transactions: List<com.streetfood.pos.data.models.Transaction>,
    reportDate: String
): String {
    fun esc(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    // Collect all reference numbers from transactions
    val refNumbers = transactions.mapNotNull { tx ->
        tx.referenceNumber?.trim()?.takeIf { it.isNotBlank() }
    }.distinct()

    // Build product rows — using exact same data as All Sales Report + Reference No.
    val rowHtml = rows.mapIndexed { idx, row ->
        val ref = refNumbers.getOrNull(idx) ?: ""
        """
        <tr>
          <td>${esc(row.productName)}</td>
          <td style="text-align:center;">${row.qtySold}</td>
          <td style="text-align:right;">${"%,.2f".format(row.grossSales)}</td>
          <td style="text-align:right;">&#8369; ${"%,.2f".format(row.costOfSales)}</td>
          <td style="text-align:right;">&#8369; ${"%,.2f".format(row.netAmount)}</td>
          <td style="text-align:center;">${"%.0f%%".format(row.percentage)}</td>
          <td></td>
          <td style="text-align:center; font-weight:bold; color:#0D47A1;">$ref</td>
        </tr>
        """.trimIndent()
    }.joinToString("")

    // Extra ref rows if there are more reference numbers than products
    val extraRefRows = if (refNumbers.size > rows.size) {
        refNumbers.drop(rows.size).joinToString("") { ref ->
            """
            <tr>
              <td></td>
              <td style="text-align:center;"></td>
              <td style="text-align:right;"></td>
              <td style="text-align:right;"></td>
              <td style="text-align:right;"></td>
              <td style="text-align:center;"></td>
              <td></td>
              <td style="text-align:center; font-weight:bold; color:#0D47A1;">$ref</td>
            </tr>
            """.trimIndent()
        }
    } else ""

    return """
        <html>
        <head>
          <meta charset="utf-8" />
          <style>
            body { font-family: Calibri, Arial, sans-serif; margin: 0; }
            table { border-collapse: collapse; width: 100%; }
            td, th { border: 1px solid #555; padding: 5px 7px; font-size: 12px; }
            .no-border { border: none !important; }
            .logo-cell { border: none !important; text-align: center; padding-bottom: 4px; }
            .brand-name {
              font-size: 26px;
              font-weight: bold;
              color: #1B5E20;
              font-family: 'Georgia', serif;
            }
            .brand-name-large {
              font-size: 32px;
              font-weight: bold;
              color: #1B5E20;
              font-family: 'Georgia', serif;
            }
            .gcash-title {
              font-size: 11px;
              color: #005CEE;
              font-weight: bold;
              letter-spacing: 2px;
              margin-top: 2px;
            }
            .date-cell { border: none !important; font-weight: bold; font-size: 13px; color: #000; }
            th {
              background-color: #fff;
              font-weight: bold;
              text-align: center;
              font-size: 12px;
            }
            .total-row td { color: #E65100; font-weight: bold; }
            .signatory-label { border: none !important; text-align: left; font-size: 11px; }
            .signatory-name  { border: none !important; text-align: left; font-size: 11px; font-weight: bold; }
          </style>
        </head>
        <body>
          <table>
            <!-- Logo / header rows -->
            <tr>
              <td class="no-border" colspan="3"></td>
              <td class="logo-cell" colspan="5">
                <div class="brand-name">ZOEY'S</div>
                <div class="brand-name-large">STREET FOODS</div>
                <div class="gcash-title">GCash Transactions</div>
              </td>
            </tr>
            <tr>
              <td class="date-cell" colspan="2">Date: ${esc(reportDate)}</td>
              <td class="no-border" colspan="6"></td>
            </tr>
            <tr><td colspan="8" class="no-border" style="height:6px;"></td></tr>
            <!-- Column headers -->
            <tr>
              <th>PRODUCT</th>
              <th>SOLD(PC'S)</th>
              <th>GROSS SALES</th>
              <th>COST OF SALES</th>
              <th>NET AMOUNT</th>
              <th>PERCENTAGE</th>
              <th>REMARKS</th>
              <th style="background-color:#E3F2FD; color:#0D47A1;">REFERENCE NO.</th>
            </tr>
            $rowHtml$extraRefRows
            <!-- Totals -->
            <tr class="total-row">
              <td>Total</td>
              <td style="text-align:center;">${totals.totalQty}</td>
              <td style="text-align:right;">${"%,.2f".format(totals.totalGross)}</td>
              <td style="text-align:right;">&#8369; ${"%,.2f".format(totals.totalCost)}</td>
              <td style="text-align:right;">&#8369; ${"%,.2f".format(totals.totalNet)}</td>
              <td style="text-align:center;">${"%.0f%%".format(totals.avgPercentage)}</td>
              <td></td>
              <td></td>
            </tr>
            <!-- Spacer -->
            <tr><td colspan="8" class="no-border" style="height:12px;"></td></tr>
            <tr><td colspan="8" class="no-border" style="height:12px;"></td></tr>
            <!-- Signatories -->
            <tr>
              <td class="no-border" colspan="5"></td>
              <td class="signatory-label">Prepared By:</td>
              <td class="signatory-name" colspan="2">Kenneth Francisco</td>
            </tr>
            <tr>
              <td class="no-border" colspan="5"></td>
              <td class="signatory-label">Reviewed By:</td>
              <td class="signatory-name" colspan="2">Judy Peralta</td>
            </tr>
            <tr>
              <td class="no-border" colspan="5"></td>
              <td class="signatory-label">Checked By:</td>
              <td class="signatory-name" colspan="2">Trecia E. De Jesus</td>
            </tr>
            <tr>
              <td class="no-border" colspan="5"></td>
              <td class="signatory-label">Noted By:</td>
              <td class="signatory-name" colspan="2">Enrique DM Martinez</td>
            </tr>
          </table>
        </body>
        </html>
    """.trimIndent()
}

