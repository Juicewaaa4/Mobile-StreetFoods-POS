package com.streetfood.pos.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streetfood.pos.data.models.DailyBookEntry
import com.streetfood.pos.viewmodel.DailyBookViewModel
import java.text.DateFormatSymbols
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyBookScreen(
    viewModel: DailyBookViewModel,
    onBack: () -> Unit
) {
    val monthYear by viewModel.selectedMonthYear.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showEntryDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableIntStateOf(1) }

    val monthName = DateFormatSymbols().months[monthYear.month - 1]
    val daysInMonth = remember(monthYear) {
        val cal = Calendar.getInstance()
        cal.set(monthYear.year, monthYear.month - 1, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Calculate totals
    val totalExp1 = entries.sumOf { it.expenses1 }
    val totalExp2 = entries.sumOf { it.expenses2 }
    val totalSales = entries.sumOf { it.sales }
    val totalOverShort = entries.sumOf { it.overShort }
    val totalTotal = entries.sumOf { it.totalSales }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(buildDailyBookExcelHtml(entries, monthName, monthYear.year, daysInMonth))
            }
        }.onSuccess {
            Toast.makeText(context, "Daily Book exported!", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📒 Daily Book", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        exportLauncher.launch("DailyBook_${monthName}_${monthYear.year}.xls")
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
        ) {
            // Month Navigation Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Default.ChevronLeft, "Previous Month")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$monthName ${monthYear.year}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$daysInMonth days • ${entries.size} entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Default.ChevronRight, "Next Month")
                    }
                }
            }

            // Totals Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Monthly Totals", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryItem("Expenses 1", totalExp1, Color(0xFFD32F2F))
                        SummaryItem("Expenses 2", totalExp2, Color(0xFFD32F2F))
                        SummaryItem("Sales", totalSales, Color(0xFF2E7D32))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryItem("Over/Short", totalOverShort, if (totalOverShort >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F))
                        SummaryItem("Total Sales", totalTotal, MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TableHeader("Day", 40.dp)
                TableHeader("Exp. 1", 80.dp)
                TableHeader("Exp. 2", 80.dp)
                TableHeader("Sales", 80.dp)
                TableHeader("Over", 70.dp)
                TableHeader("Total", 80.dp)
            }

            // Table Rows
            LazyColumn(modifier = Modifier.weight(1f)) {
                val daysList = (1..daysInMonth).toList()
                itemsIndexed(daysList) { index, day ->
                    val entry = entries.find { it.day == day }
                    val isEven = index % 2 == 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(if (isEven) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable {
                                selectedDay = day
                                showEntryDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Day number
                        Text(
                            day.toString(),
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        // Expenses 1
                        TableCell(entry?.expenses1, 80.dp)
                        // Expenses 2
                        TableCell(entry?.expenses2, 80.dp)
                        // Sales
                        TableCell(entry?.sales, 80.dp, color = Color(0xFF2E7D32))
                        // Over/Short
                        TableCell(
                            entry?.overShort, 70.dp,
                            color = if ((entry?.overShort ?: 0.0) >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                        // Total
                        TableCell(entry?.totalSales, 80.dp, fontWeight = FontWeight.Bold)
                    }

                    if (index < daysInMonth - 1) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Entry Dialog
    if (showEntryDialog) {
        val existingEntry = entries.find { it.day == selectedDay }
        DailyEntryDialog(
            day = selectedDay,
            monthName = monthName,
            year = monthYear.year,
            existing = existingEntry,
            viewModel = viewModel,
            onDismiss = { showEntryDialog = false },
            onSave = { entry ->
                viewModel.saveEntry(entry) { success ->
                    if (success) {
                        Toast.makeText(context, "Day $selectedDay saved!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to save.", Toast.LENGTH_SHORT).show()
                    }
                }
                showEntryDialog = false
            }
        )
    }
}

@Composable
private fun TableHeader(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun TableCell(
    value: Double?,
    width: androidx.compose.ui.unit.Dp,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = if (value != null && value != 0.0) "%,.0f".format(value) else "—",
        modifier = Modifier.width(width),
        textAlign = TextAlign.End,
        fontSize = 12.sp,
        color = if (value != null && value != 0.0) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        fontWeight = fontWeight
    )
}

@Composable
private fun SummaryItem(label: String, value: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(
            "₱%,.0f".format(value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun DailyEntryDialog(
    day: Int,
    monthName: String,
    year: Int,
    existing: DailyBookEntry?,
    viewModel: DailyBookViewModel,
    onDismiss: () -> Unit,
    onSave: (DailyBookEntry) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Day Shift States
    var exp1DayText by remember { mutableStateOf(if (existing?.expenses1Day != 0.0) existing?.expenses1Day?.toLong()?.toString() ?: "" else "") }
    var exp2DayText by remember { mutableStateOf(if (existing?.expenses2Day != 0.0) existing?.expenses2Day?.toLong()?.toString() ?: "" else "") }
    var overDayText by remember { mutableStateOf(if (existing?.overShortDay != 0.0) existing?.overShortDay?.toLong()?.toString() ?: "" else "") }
    var salesDayText by remember { mutableStateOf(if (existing?.salesDay != 0.0) existing?.salesDay?.toLong()?.toString() ?: "" else "") }

    // Night Shift States
    var exp1NightText by remember { mutableStateOf(if (existing?.expenses1Night != 0.0) existing?.expenses1Night?.toLong()?.toString() ?: "" else "") }
    var exp2NightText by remember { mutableStateOf(if (existing?.expenses2Night != 0.0) existing?.expenses2Night?.toLong()?.toString() ?: "" else "") }
    var overNightText by remember { mutableStateOf(if (existing?.overShortNight != 0.0) existing?.overShortNight?.toLong()?.toString() ?: "" else "") }
    var salesNightText by remember { mutableStateOf(if (existing?.salesNight != 0.0) existing?.salesNight?.toLong()?.toString() ?: "" else "") }

    // Shared States
    var notesText by remember { mutableStateOf(existing?.notes ?: "") }

    val exp1Day = exp1DayText.toDoubleOrNull() ?: 0.0
    val exp2Day = exp2DayText.toDoubleOrNull() ?: 0.0
    val overDay = overDayText.toDoubleOrNull() ?: 0.0
    val salesDay = salesDayText.toDoubleOrNull() ?: 0.0

    val exp1Night = exp1NightText.toDoubleOrNull() ?: 0.0
    val exp2Night = exp2NightText.toDoubleOrNull() ?: 0.0
    val overNight = overNightText.toDoubleOrNull() ?: 0.0
    val salesNight = salesNightText.toDoubleOrNull() ?: 0.0

    val total = salesDay + salesNight + overDay + overNight

    val monthIndex = DateFormatSymbols().months.indexOf(monthName) + 1
    val dateStr = "$year-${monthIndex.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    LaunchedEffect(day, monthIndex, year) {
        if (existing == null || existing.salesDay == 0.0) {
            val pulledDay = viewModel.getShiftSales(day, monthIndex, year, true)
            if (pulledDay > 0) salesDayText = pulledDay.toLong().toString()
        }
        if (existing == null || existing.salesNight == 0.0) {
            val pulledNight = viewModel.getShiftSales(day, monthIndex, year, false)
            if (pulledNight > 0) salesNightText = pulledNight.toLong().toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("$monthName $day, $year", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Day Shift", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Night Shift", fontWeight = FontWeight.Bold) }
                    )
                }

                if (selectedTabIndex == 0) {
                    OutlinedTextField(
                        value = exp1DayText,
                        onValueChange = { exp1DayText = it },
                        label = { Text("Expenses 1 (Ingredients)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) }
                    )
                    OutlinedTextField(
                        value = exp2DayText,
                        onValueChange = { exp2DayText = it },
                        label = { Text("Expenses 2 (Other)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) }
                    )
                    OutlinedTextField(
                        value = overDayText,
                        onValueChange = { overDayText = it },
                        label = { Text("Over/Short") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) },
                        supportingText = { Text("Negative = short, Positive = over") }
                    )
                    OutlinedTextField(
                        value = salesDayText,
                        onValueChange = { salesDayText = it },
                        label = { Text("POS Sales (Day)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) }
                    )
                } else {
                    OutlinedTextField(
                        value = exp1NightText,
                        onValueChange = { exp1NightText = it },
                        label = { Text("Expenses 1 (Ingredients)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) }
                    )
                    OutlinedTextField(
                        value = exp2NightText,
                        onValueChange = { exp2NightText = it },
                        label = { Text("Expenses 2 (Other)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) }
                    )
                    OutlinedTextField(
                        value = overNightText,
                        onValueChange = { overNightText = it },
                        label = { Text("Over/Short") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) },
                        supportingText = { Text("Negative = short, Positive = over") }
                    )
                    OutlinedTextField(
                        value = salesNightText,
                        onValueChange = { salesNightText = it },
                        label = { Text("POS Sales (Night)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(Modifier.height(4.dp))
                Divider()

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Auto-computed total
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Final Sales", fontWeight = FontWeight.Bold)
                        Text("₱%,.2f".format(total), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        DailyBookEntry(
                            date = dateStr,
                            day = day,
                            month = monthIndex,
                            year = year,
                            expenses1Day = exp1Day,
                            expenses2Day = exp2Day,
                            overShortDay = overDay,
                            salesDay = salesDay,
                            expenses1Night = exp1Night,
                            expenses2Night = exp2Night,
                            overShortNight = overNight,
                            salesNight = salesNight,
                            totalSales = total,
                            notes = notesText
                        )
                    )
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun buildDailyBookExcelHtml(
    entries: List<DailyBookEntry>,
    monthName: String,
    year: Int,
    daysInMonth: Int
): String {
    val rowHtml = (1..daysInMonth).joinToString("") { day ->
        val e = entries.find { it.day == day }
        val exp1D = e?.expenses1Day ?: 0.0
        val exp2D = e?.expenses2Day ?: 0.0
        val overD = e?.overShortDay ?: 0.0
        val salesD = e?.salesDay ?: 0.0
        val exp1N = e?.expenses1Night ?: 0.0
        val exp2N = e?.expenses2Night ?: 0.0
        val overN = e?.overShortNight ?: 0.0
        val salesN = e?.salesNight ?: 0.0
        val total = e?.totalSales ?: 0.0
        fun fmt(v: Double) = if (v != 0.0) "%,.0f".format(v) else ""
        """
        <tr>
          <td style="text-align:center; font-weight:bold;">$day</td>
          <td style="text-align:right;">${fmt(exp1D)}</td>
          <td style="text-align:right;">${fmt(exp2D)}</td>
          <td style="text-align:right;">${fmt(overD)}</td>
          <td style="text-align:right; color:#1565C0;">${fmt(salesD)}</td>
          <td style="text-align:right;">${fmt(exp1N)}</td>
          <td style="text-align:right;">${fmt(exp2N)}</td>
          <td style="text-align:right;">${fmt(overN)}</td>
          <td style="text-align:right; color:#1565C0;">${fmt(salesN)}</td>
          <td style="text-align:right; font-weight:bold; color:#E65100;">${if (total != 0.0) "%,.0f".format(total) else ""}</td>
        </tr>
        """.trimIndent()
    }

    val totalExp1 = entries.sumOf { it.expenses1 }
    val totalExp2 = entries.sumOf { it.expenses2 }
    val totalOverShort = entries.sumOf { it.overShort }
    val totalSales = entries.sumOf { it.salesDay + it.salesNight }
    val totalFinal = entries.sumOf { it.totalSales }

    return """
        <html>
        <head>
          <meta charset="utf-8" />
          <style>
            body { font-family: Calibri, Arial, sans-serif; margin: 0; }
            table { border-collapse: collapse; width: 100%; }
            td, th { border: 1px solid #999; padding: 4px 6px; font-size: 11px; }
            .no-border { border: none !important; }
            .brand-name { font-size: 26px; font-weight: bold; color: #E65100; font-family: 'Georgia', serif; }
            .brand-name-large { font-size: 32px; font-weight: bold; color: #E65100; font-family: 'Georgia', serif; }
            .sub-title { font-size: 12px; color: #555; margin-top: 2px; }
            th { background-color: #FFF3E0; font-weight: bold; text-align: center; font-size: 10px; }
            .shift-header { background-color: #E3F2FD; color: #0D47A1; font-weight: bold; text-align: center; font-size: 11px; }
            .total-row td { font-weight: bold; color: #E65100; border-top: 2px solid #E65100; }
          </style>
        </head>
        <body>
          <table>
            <tr>
              <td class="no-border" colspan="3"></td>
              <td class="no-border" colspan="7" style="text-align:center; padding-bottom:6px;">
                <div class="brand-name">ARCEO'S</div>
                <div class="brand-name-large">LUGAW HOUSE</div>
                <div class="sub-title">Daily Book &mdash; $monthName $year</div>
              </td>
            </tr>
            <tr><td colspan="10" class="no-border" style="height:4px;"></td></tr>
            <tr>
              <th rowspan="2">DAY</th>
              <th class="shift-header" colspan="4">☀ DAY SHIFT (3AM-3PM)</th>
              <th class="shift-header" colspan="4">🌙 NIGHT SHIFT (3PM-3AM)</th>
              <th rowspan="2" style="background-color:#FFF3E0;">TOTAL</th>
            </tr>
            <tr>
              <th>Exp 1</th><th>Exp 2</th><th>Over</th><th>Sales</th>
              <th>Exp 1</th><th>Exp 2</th><th>Over</th><th>Sales</th>
            </tr>
            $rowHtml
            <tr class="total-row">
              <td style="text-align:center;">TOTAL</td>
              <td style="text-align:right;" colspan="2">${"%,.0f".format(totalExp1 + totalExp2)}</td>
              <td style="text-align:right;">${"%,.0f".format(totalOverShort)}</td>
              <td style="text-align:right;">${"%,.0f".format(totalSales)}</td>
              <td style="text-align:right;" colspan="2"></td>
              <td style="text-align:right;"></td>
              <td style="text-align:right;"></td>
              <td style="text-align:right; font-size:13px;">&amp;#8369; ${"%,.0f".format(totalFinal)}</td>
            </tr>
          </table>
        </body>
        </html>
    """.trimIndent()
}
