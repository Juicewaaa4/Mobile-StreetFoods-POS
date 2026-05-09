package com.streetfood.pos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.streetfood.pos.data.models.User
import com.streetfood.pos.data.models.UserRole
import com.streetfood.pos.ui.components.ConfirmDialog
import com.streetfood.pos.ui.components.EmptyStateView
import com.streetfood.pos.ui.components.LoadingIndicator
import com.streetfood.pos.viewmodel.UserManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    viewModel: UserManagementViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editTarget       by remember { mutableStateOf<User?>(null) }
    var deleteTarget     by remember { mutableStateOf<User?>(null) }

    // Show snackbar on success / error
    LaunchedEffect(state.successMessage) {
        val msg = state.successMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg, withDismissAction = true)
        viewModel.consumeSuccess()
    }
    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg, withDismissAction = true)
        viewModel.consumeError()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("User Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon    = { Icon(Icons.Default.PersonAdd, contentDescription = "Add User") },
                text    = { Text("Add User") }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingIndicator("Loading users…")

            state.users.isEmpty() -> EmptyStateView(
                "👤", "No Users Yet",
                "Tap the button below to create the first user.",
                modifier = Modifier.padding(padding)
            )

            else -> {
                LazyColumn(
                    modifier             = Modifier.fillMaxSize().padding(padding),
                    contentPadding       = PaddingValues(16.dp),
                    verticalArrangement  = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary chip
                    item {
                        val adminCount   = state.users.count { it.role == UserRole.ADMIN }
                        val cashierCount = state.users.count { it.role == UserRole.CASHIER }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuggestionChip(
                                onClick = {},
                                label   = { Text("$adminCount Admin${if (adminCount != 1) "s" else ""}") }
                            )
                            SuggestionChip(
                                onClick = {},
                                label   = { Text("$cashierCount Cashier${if (cashierCount != 1) "s" else ""}") }
                            )
                        }
                    }

                    items(state.users, key = { it.id }) { user ->
                        UserCard(
                            user     = user,
                            onEdit   = { editTarget   = user },
                            onDelete = { deleteTarget = user }
                        )
                    }

                    item { Spacer(Modifier.height(80.dp)) } // FAB clearance
                }
            }
        }

        // Loading overlay during save
        if (state.isSaving) {
            Box(
                modifier        = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (showCreateDialog) {
        CreateUserDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { username, password, role ->
                viewModel.createUser(username, password, role)
                showCreateDialog = false
            }
        )
    }

    editTarget?.let { user ->
        EditUserDialog(
            user      = user,
            onDismiss = { editTarget = null },
            onConfirm = { newUsername, newRole ->
                viewModel.updateUser(user.id, newUsername, newRole)
                editTarget = null
            }
        )
    }

    deleteTarget?.let { user ->
        ConfirmDialog(
            title        = "Delete User?",
            message      = "\"${user.username}\" will be permanently removed.",
            confirmLabel = "Delete",
            onConfirm    = { viewModel.deleteUser(user); deleteTarget = null },
            onDismiss    = { deleteTarget = null }
        )
    }
}

// ── User card ─────────────────────────────────────────────────────────────────
@Composable
private fun UserCard(user: User, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isAdmin = user.role == UserRole.ADMIN
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier           = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment  = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isAdmin) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                        contentDescription = null,
                        tint     = if (isAdmin) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.username,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (user.email.isBlank()) user.role.name.lowercase().replaceFirstChar { it.uppercase() }
                    else "${user.role.name.lowercase().replaceFirstChar { it.uppercase() }} • ${user.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAdmin) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actions
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ── Create user dialog ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (username: String, password: String, role: UserRole) -> Unit
) {
    var username      by remember { mutableStateOf("") }
    var password      by remember { mutableStateOf("") }
    var showPassword  by remember { mutableStateOf(false) }
    var selectedRole  by remember { mutableStateOf(UserRole.CASHIER) }
    var expanded      by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Add New User", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value         = username,
                    onValueChange = { username = it },
                    label         = { Text("Username") },
                    leadingIcon   = { Icon(Icons.Default.Person, null) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = password,
                    onValueChange = { password = it },
                    label         = { Text("Password") },
                    leadingIcon   = { Icon(Icons.Default.Lock, null) },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (showPassword) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    trailingIcon  = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password"
                            )
                        }
                    },
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Role picker
                Text("Role", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserRole.values().forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick  = { selectedRole = role },
                            label    = { Text(role.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            leadingIcon = if (selectedRole == role) {{ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }} else null
                        )
                    }
                }

                AnimatedVisibility(visible = username.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                "They can sign in with \"${username.trim().lowercase()}\".",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(username, password, selectedRole) },
                enabled  = username.isNotBlank() && password.isNotBlank(),
                shape    = RoundedCornerShape(10.dp)
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Edit user dialog ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: (newUsername: String, newRole: UserRole) -> Unit
) {
    var username     by remember { mutableStateOf(user.username) }
    var selectedRole by remember { mutableStateOf(user.role) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Edit User", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value         = username,
                    onValueChange = { username = it },
                    label         = { Text("Username") },
                    leadingIcon   = { Icon(Icons.Default.Person, null) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    modifier      = Modifier.fillMaxWidth()
                )

                Text("Role", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserRole.values().forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick  = { selectedRole = role },
                            label    = { Text(role.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            leadingIcon = if (selectedRole == role) {{ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }} else null
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(username, selectedRole) },
                enabled  = username.isNotBlank(),
                shape    = RoundedCornerShape(10.dp)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
