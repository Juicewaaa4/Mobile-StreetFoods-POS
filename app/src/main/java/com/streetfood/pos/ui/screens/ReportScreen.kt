package com.streetfood.pos.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

private val colProduct = 130.dp
private val colSold = 72.dp
private val colGross = 90.dp
private val colCost = 90.dp
private val colNet = 90.dp
private val colPct = 76.dp
private val colRemarks = 90.dp

private val OrangeTotal = Color(0xFFE65100)
private val HeaderBg = Color(0xFF1B5E20)
private val HeaderText = Color.White
private val AltRowBg = Color(0xFFF1F8E9)
private val BorderColor = Color(0xFFBDBDBD)

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
                writer.write(buildExcelHtml(state))
            }
        }.onSuccess {
            Toast.makeText(context, "Report downloaded.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Unable to download report.", Toast.LENGTH_SHORT).show()
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
                onPickDate = { showDatePicker = true },
                onDownload = {
                    val stamp = state.reportDate.replace("/", "-").ifBlank {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    }
                    exportLauncher.launch("zoeys-sales-report-$stamp.xls")
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
                else -> ReportTable(
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
    onPickDate: () -> Unit,
    onDownload: () -> Unit
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
                        Spacer(Modifier.width(6.dp))
                        Text("Date")
                    }
                    Button(onClick = onDownload, enabled = hasRows, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Excel")
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
                    Text("Download Excel")
                }
            }
        }
    }
}

@Composable
private fun ReportTable(
    reportDate: String,
    rows: List<ReportRow>,
    totals: ReportTotals
) {
    val hScroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "ZOEY'S STREET FOODS",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = HeaderBg
            )
            Text(
                "SALES REPORT",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF388E3C)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Date: $reportDate",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(modifier = Modifier.fillMaxSize().horizontalScroll(hScroll)) {
            Column {
                TableHeaderRow()
                LazyColumn(
                    modifier = Modifier.widthIn(min = colProduct + colSold + colGross + colCost + colNet + colPct + colRemarks),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(rows.mapIndexed { i, r -> i to r }) { (idx, row) ->
                        TableDataRow(row = row, isAlt = idx % 2 == 1)
                    }
                    item { TableTotalsRow(totals) }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderRow() {
    Row(modifier = Modifier.background(HeaderBg).border(1.dp, BorderColor)) {
        HeaderCell("PRODUCT", colProduct, TextAlign.Left)
        HeaderCell("SOLD\n(PC'S)", colSold, TextAlign.Center)
        HeaderCell("GROSS\nSALES", colGross, TextAlign.Center)
        HeaderCell("COST OF\nSALES", colCost, TextAlign.Center)
        HeaderCell("NET\nAMOUNT", colNet, TextAlign.Center)
        HeaderCell("PERCENT\nAGE", colPct, TextAlign.Center)
        HeaderCell("REMARKS", colRemarks, TextAlign.Center)
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp, align: TextAlign) {
    Box(
        modifier = Modifier.width(width).height(48.dp).border(0.5.dp, BorderColor.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = HeaderText,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = align,
            lineHeight = 13.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun TableDataRow(row: ReportRow, isAlt: Boolean) {
    val bg = if (isAlt) AltRowBg else Color.White
    Row(modifier = Modifier.background(bg).border(0.5.dp, BorderColor), verticalAlignment = Alignment.CenterVertically) {
        DataCell(row.productName, colProduct, TextAlign.Left, isName = true)
        DataCell(row.qtySold.toString(), colSold, TextAlign.Center)
        DataCell("₱ %,.2f".format(row.grossSales), colGross, TextAlign.Right)
        DataCell("₱ %,.2f".format(row.costOfSales), colCost, TextAlign.Right)
        DataCell("₱ %,.2f".format(row.netAmount), colNet, TextAlign.Right)
        DataCell("%.0f%%".format(row.percentage), colPct, TextAlign.Center)
        DataCell("", colRemarks, TextAlign.Center)
    }
}

@Composable
private fun DataCell(
    text: String,
    width: Dp,
    align: TextAlign,
    isName: Boolean = false
) {
    Box(
        modifier = Modifier.width(width).height(38.dp).border(0.3.dp, BorderColor.copy(alpha = 0.3f)),
        contentAlignment = when (align) {
            TextAlign.Right -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        }
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isName) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = align,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

@Composable
private fun TableTotalsRow(totals: ReportTotals) {
    Row(modifier = Modifier.background(Color(0xFFFFF3E0)).border(1.dp, OrangeTotal), verticalAlignment = Alignment.CenterVertically) {
        TotalCell("Total", colProduct, TextAlign.Left, bold = true, color = OrangeTotal)
        TotalCell(totals.totalQty.toString(), colSold, TextAlign.Center, bold = true, color = OrangeTotal)
        TotalCell("₱ %,.2f".format(totals.totalGross), colGross, TextAlign.Right, bold = true, color = OrangeTotal)
        TotalCell("₱ %,.2f".format(totals.totalCost), colCost, TextAlign.Right, bold = true, color = OrangeTotal)
        TotalCell("₱ %,.2f".format(totals.totalNet), colNet, TextAlign.Right, bold = true, color = OrangeTotal)
        TotalCell("%.0f%%".format(totals.avgPercentage), colPct, TextAlign.Center, bold = true, color = OrangeTotal)
        TotalCell("", colRemarks, TextAlign.Center, bold = false)
    }
}

@Composable
private fun TotalCell(
    text: String,
    width: Dp,
    align: TextAlign,
    bold: Boolean,
    color: Color = OrangeTotal
) {
    Box(
        modifier = Modifier.width(width).height(42.dp).border(0.3.dp, OrangeTotal.copy(alpha = 0.4f)),
        contentAlignment = when (align) {
            TextAlign.Right -> Alignment.CenterEnd
            TextAlign.Center -> Alignment.Center
            else -> Alignment.CenterStart
        }
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal,
            color = color,
            textAlign = align,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

private fun buildExcelHtml(state: ReportState): String {
    fun esc(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    val rows = state.rows.joinToString("") { row ->
        """
        <tr>
          <td>${esc(row.productName)}</td>
          <td class="center">${row.qtySold}</td>
          <td class="money">${row.grossSales}</td>
          <td class="money">${row.costOfSales}</td>
          <td class="money">${row.netAmount}</td>
          <td class="center">${"%.0f%%".format(row.percentage)}</td>
          <td></td>
        </tr>
        """.trimIndent()
    }

    return """
        <html>
        <head>
          <meta charset="utf-8" />
          <style>
            table { border-collapse: collapse; font-family: Arial, sans-serif; }
            th { background: #1B5E20; color: white; font-weight: bold; text-align: center; }
            th, td { border: 1px solid #9E9E9E; padding: 6px 8px; }
            .title { color: #1B5E20; font-size: 20px; font-weight: bold; text-align: center; }
            .subtitle { color: #388E3C; font-weight: bold; text-align: center; }
            .date { text-align: center; font-weight: bold; }
            .money { mso-number-format:"₱ #,##0.00"; text-align: right; }
            .center { text-align: center; }
            .total td { background: #FFF3E0; color: #E65100; font-weight: bold; }
          </style>
        </head>
        <body>
          <table>
            <tr><td class="title" colspan="7">ZOEY'S STREET FOODS</td></tr>
            <tr><td class="subtitle" colspan="7">SALES REPORT</td></tr>
            <tr><td class="date" colspan="7">Date: ${esc(state.reportDate)}</td></tr>
            <tr>
              <th>PRODUCT</th><th>SOLD (PC'S)</th><th>GROSS SALES</th><th>COST OF SALES</th><th>NET AMOUNT</th><th>PERCENTAGE</th><th>REMARKS</th>
            </tr>
            $rows
            <tr class="total">
              <td>Total</td>
              <td class="center">${state.totals.totalQty}</td>
              <td class="money">${state.totals.totalGross}</td>
              <td class="money">${state.totals.totalCost}</td>
              <td class="money">${state.totals.totalNet}</td>
              <td class="center">${"%.0f%%".format(state.totals.avgPercentage)}</td>
              <td></td>
            </tr>
          </table>
        </body>
        </html>
    """.trimIndent()
}
