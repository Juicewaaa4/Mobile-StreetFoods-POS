with open('c:/Streetfoods/app/src/main/java/com/streetfood/pos/ui/screens/PaymentScreen.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = lines[:198] + [
    '                // ── Cash Input Field ──\n',
    '                item {\n',
    '                    OutlinedTextField(\n',
    '                        value = cashInput,\n',
    '                        onValueChange = { newValue ->\n',
    '                            if (newValue.isEmpty() || newValue.matches(Regex("^\\\\d*\\\\.?\\\\d{0,2}$"))) {\n',
    '                                cashInput = newValue\n',
    '                            }\n',
    '                        },\n',
    '                        label = { Text("Amount Received") },\n',
    '                        modifier = Modifier.fillMaxWidth(),\n',
    '                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(\n',
    '                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number\n',
    '                        ),\n',
    '                        singleLine = true,\n',
    '                        shape = RoundedCornerShape(12.dp)\n',
    '                    )\n',
    '                }\n'
] + lines[208:]

with open('c:/Streetfoods/app/src/main/java/com/streetfood/pos/ui/screens/PaymentScreen.kt', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)
