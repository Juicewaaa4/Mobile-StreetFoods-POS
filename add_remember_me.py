import re

with open('c:/Streetfoods/app/src/main/java/com/streetfood/pos/ui/screens/LoginScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if 'import android.content.Context' not in content:
    content = content.replace('import androidx.compose.animation.*', 'import android.content.Context\nimport androidx.compose.animation.*')

vars_block = '''    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }'''

new_vars = '''    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE) }
    var username by remember { mutableStateOf(sharedPref.getString("username", "") ?: "") }
    var password by remember { mutableStateOf(sharedPref.getString("password", "") ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(sharedPref.getBoolean("rememberMe", false)) }'''

content = content.replace(vars_block, new_vars)

old_effect = '''    // Navigate once login succeeds
    LaunchedEffect(isLoggedIn, userRole) {
        if (isLoggedIn && userRole != null) {
            onLoginSuccess(userRole!!)
        }
    }'''

new_effect = '''    // Navigate once login succeeds
    LaunchedEffect(isLoggedIn, userRole) {
        if (isLoggedIn && userRole != null) {
            if (rememberMe) {
                sharedPref.edit()
                    .putString("username", username)
                    .putString("password", password)
                    .putBoolean("rememberMe", true)
                    .apply()
            } else {
                sharedPref.edit().clear().apply()
            }
            onLoginSuccess(userRole!!)
        }
    }'''

content = content.replace(old_effect, new_effect)

old_ui = '''                    // Error message
                    AnimatedVisibility(visible = loginError != null) {'''

new_ui = '''                    // Remember Me Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Remember Me",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Error message
                    AnimatedVisibility(visible = loginError != null) {'''

content = content.replace(old_ui, new_ui)

with open('c:/Streetfoods/app/src/main/java/com/streetfood/pos/ui/screens/LoginScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
