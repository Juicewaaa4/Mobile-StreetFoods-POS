import re

with open('c:/Streetfoods/app/src/main/java/com/streetfood/pos/ui/screens/PaymentScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

new_content = re.sub(
    r'NumericKeypad\(\s*onKeyPress = \{ key ->\s*cashInput = updateCashInput\(cashInput, key\)\s*\},\s*modifier = Modifier\.fillMaxWidth\(\)\s*\)',
    '''OutlinedTextField(
                        value = cashInput,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\\\d*\\\\.?\\\\d{0,2}$"))) {
                                cashInput = newValue
                            }
                        },
                        label = { Text("Amount Received") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )''',
    content,
    flags=re.DOTALL
)

with open('c:/Streetfoods/app/src/main/java/com/streetfood/pos/ui/screens/PaymentScreen.kt', 'w', encoding='utf-8') as f:
    f.write(new_content)

