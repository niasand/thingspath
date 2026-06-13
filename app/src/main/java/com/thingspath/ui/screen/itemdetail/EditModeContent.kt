package com.thingspath.ui.screen.itemdetail

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun EditModeContent(
    state: ItemDetailState,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onPurchaseDateChange: (String) -> Unit,
    onPurchasePriceChange: (String) -> Unit,
    onUsageDaysChange: (String) -> Unit,
    onReminderDateChange: (String) -> Unit,
    onReminderTypeChange: (String) -> Unit,
    onReminderNoteChange: (String) -> Unit,
    onClearReminder: () -> Unit,
    onNoteChange: (String) -> Unit,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            onPurchaseDateChange(selectedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    val reminderDatePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            onReminderDateChange(selectedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    OutlinedTextField(
        value = state.name,
        onValueChange = onNameChange,
        label = { Text("名称 *") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = state.name.trim().isEmpty()
    )

    OutlinedTextField(
        value = state.location,
        onValueChange = onLocationChange,
        label = { Text("位置") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Box(modifier = Modifier.clickable { datePickerDialog.show() }) {
        OutlinedTextField(
            value = state.purchaseDate,
            onValueChange = {},
            label = { Text("购买日期 (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            singleLine = true,
            placeholder = { Text("例如 2024-01-15") },
            trailingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = "选择日期")
            }
        )
    }

    OutlinedTextField(
        value = state.purchasePrice,
        onValueChange = onPurchasePriceChange,
        label = { Text("价格") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
        placeholder = { Text("0.00") }
    )

    OutlinedTextField(
        value = state.usageDays,
        onValueChange = onUsageDaysChange,
        label = { Text("使用天数") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("例如 30") }
    )

    ReminderEditor(
        state = state,
        onReminderDateClick = { reminderDatePickerDialog.show() },
        onReminderTypeChange = onReminderTypeChange,
        onReminderNoteChange = onReminderNoteChange,
        onClearReminder = onClearReminder
    )

    OutlinedTextField(
        value = state.note,
        onValueChange = onNoteChange,
        label = { Text("备注") },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 5,
        placeholder = { Text("添加备注...") }
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "标签",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.tagInput,
                onValueChange = onTagInputChange,
                label = { Text("添加标签") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddTag() })
            )
            IconButton(onClick = onAddTag) {
                Icon(Icons.Default.Add, contentDescription = "添加标签")
            }
        }

        if (state.tags.isNotEmpty()) {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "移除标签",
                                modifier = Modifier.size(16.dp).clickable { onRemoveTag(tag) }
                            )
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditor(
    state: ItemDetailState,
    onReminderDateClick: () -> Unit,
    onReminderTypeChange: (String) -> Unit,
    onReminderNoteChange: (String) -> Unit,
    onClearReminder: () -> Unit
) {
    val reminderTypes = listOf("到期提醒", "保修提醒", "维护提醒", "补货提醒")
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "提醒",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = state.reminderType,
                onValueChange = {},
                readOnly = true,
                label = { Text("提醒类型") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                reminderTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            onReminderTypeChange(type)
                            expanded = false
                        }
                    )
                }
            }
        }

        Box(modifier = Modifier.clickable { onReminderDateClick() }) {
            OutlinedTextField(
                value = state.reminderDate,
                onValueChange = {},
                label = { Text("提醒日期 (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                singleLine = true,
                placeholder = { Text("例如 2026-12-31") },
                trailingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = "选择提醒日期")
                }
            )
        }

        OutlinedTextField(
            value = state.reminderNote,
            onValueChange = onReminderNoteChange,
            label = { Text("提醒备注") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            placeholder = { Text("例如 滤芯更换、保修结束、药品过期") }
        )

        if (state.reminderDate.isNotBlank() || state.reminderNote.isNotBlank()) {
            TextButton(onClick = onClearReminder) {
                Text("清除提醒")
            }
        }
    }
}
