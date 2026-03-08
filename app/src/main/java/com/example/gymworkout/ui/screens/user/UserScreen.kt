package com.example.gymworkout.ui.screens.user

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import coil.compose.AsyncImage
import com.example.gymworkout.data.ChecklistItem
import com.example.gymworkout.data.ThemePreference
import com.example.gymworkout.data.UserProfile
import com.example.gymworkout.data.WorkoutReminder
import com.example.gymworkout.data.MotivationalQuote
import com.example.gymworkout.data.MotivationalQuotes
import com.example.gymworkout.data.QuotePreference
import com.example.gymworkout.data.sync.SyncPreference
import com.example.gymworkout.viewmodel.SyncState
import com.example.gymworkout.viewmodel.UserViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(viewModel: UserViewModel) {
    val context = LocalContext.current
    val profile by viewModel.getProfile().collectAsState(initial = null)
    val dos by viewModel.getDos().collectAsState(initial = emptyList())
    val donts by viewModel.getDonts().collectAsState(initial = emptyList())
    var showEditProfile by remember { mutableStateOf(false) }
    var showAddDo by remember { mutableStateOf(false) }
    var showAddDont by remember { mutableStateOf(false) }

    // Google sync state
    val syncState by viewModel.syncState.collectAsState()
    val accountEmail by SyncPreference.accountEmail.collectAsState()
    val lastSyncTime by SyncPreference.lastSyncTime.collectAsState()
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showWorkoutReminderDialog by remember { mutableStateOf(false) }
    val workoutReminders by viewModel.getWorkoutReminders().collectAsState(initial = emptyList())

    // Auto backup state
    var autoBackupEnabled by remember { mutableStateOf(viewModel.isAutoBackupEnabled()) }

    // Motivational quotes state
    var showQuoteDialog by remember { mutableStateOf(false) }
    val quoteEnabled by QuotePreference.enabled.collectAsState()
    val quoteSource by QuotePreference.source.collectAsState()
    val quoteTime by QuotePreference.time.collectAsState()
    val customQuotes by viewModel.getCustomQuotes().collectAsState(initial = emptyList())

    // Auto-dismiss success/error after 3 seconds
    LaunchedEffect(syncState) {
        if (syncState is SyncState.Success || syncState is SyncState.Error) {
            delay(3000)
            viewModel.clearSyncState()
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.data)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addPhotoUri(profile, it.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Your fitness journey",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ProfileCard(profile = profile, onEdit = { showEditProfile = true }) }

            // Google Sync card
            item {
                GoogleSyncCard(
                    accountEmail = accountEmail,
                    lastSyncTime = lastSyncTime,
                    syncState = syncState,
                    autoBackupEnabled = autoBackupEnabled,
                    onSignIn = {
                        signInLauncher.launch(viewModel.getGoogleSignInClient(context).signInIntent)
                    },
                    onBackup = { viewModel.backupToGoogleDrive() },
                    onRestore = { showRestoreConfirm = true },
                    onSignOut = {
                        viewModel.signOut(context)
                        autoBackupEnabled = false
                    },
                    onAutoBackupToggle = {
                        viewModel.setAutoBackupEnabled(it)
                        autoBackupEnabled = it
                    }
                )
            }

            item { ThemeToggleCard() }

            item {
                WorkoutNotificationCard(
                    reminders = workoutReminders,
                    onClick = { showWorkoutReminderDialog = true }
                )
            }

            item { AIGoalsCard() }

            item {
                PhotosSection(
                    profile = profile,
                    onAddPhoto = { photoPickerLauncher.launch("image/*") },
                    onRemovePhoto = { uri -> viewModel.removePhotoUri(profile, uri) }
                )
            }

            item {
                ChecklistSection(
                    title = "Do's",
                    subtitle = "Habits to maintain",
                    items = dos,
                    accentColor = Color(0xFF66BB6A),
                    onToggle = { id, checked -> viewModel.toggleChecklistItem(id, checked) },
                    onDelete = { viewModel.deleteChecklistItem(it) },
                    onAdd = { showAddDo = true }
                )
            }

            item {
                ChecklistSection(
                    title = "Don'ts",
                    subtitle = "Habits to avoid",
                    items = donts,
                    accentColor = Color(0xFFEF5350),
                    onToggle = { id, checked -> viewModel.toggleChecklistItem(id, checked) },
                    onDelete = { viewModel.deleteChecklistItem(it) },
                    onAdd = { showAddDont = true }
                )
            }

            item {
                MotivationalQuotesCard(
                    enabled = quoteEnabled,
                    source = quoteSource,
                    time = quoteTime,
                    customQuoteCount = customQuotes.size,
                    onClick = { showQuoteDialog = true }
                )
            }

            // Reset buttons for checklists
            if (dos.isNotEmpty() || donts.isNotEmpty()) {
                item {
                    ChecklistResetCard(
                        onResetDay = { viewModel.resetDayChecklist() },
                        onResetWeek = { viewModel.resetWeekChecklist() }
                    )
                }
            }

            // Feedback card
            item {
                FeedbackCard(onSendFeedback = { showFeedbackDialog = true })
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            onSend = { subject, body ->
                showFeedbackDialog = false
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("iaditya91@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    context.startActivity(Intent.createChooser(intent, "Send feedback"))
                }
            }
        )
    }

    if (showEditProfile) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditProfile = false },
            onSave = { name, weight, weightUnit, height, heightUnit ->
                viewModel.saveProfile(
                    (profile ?: UserProfile()).copy(
                        name = name, weight = weight, weightUnit = weightUnit,
                        height = height, heightUnit = heightUnit
                    )
                )
                showEditProfile = false
            }
        )
    }

    if (showAddDo) {
        AddChecklistDialog(
            title = "Add Do",
            onDismiss = { showAddDo = false },
            onSave = { text -> viewModel.addChecklistItem("DO", text); showAddDo = false }
        )
    }

    if (showAddDont) {
        AddChecklistDialog(
            title = "Add Don't",
            onDismiss = { showAddDont = false },
            onSave = { text -> viewModel.addChecklistItem("DONT", text); showAddDont = false }
        )
    }

    if (showWorkoutReminderDialog) {
        WorkoutReminderDialog(
            reminders = workoutReminders,
            onDismiss = { showWorkoutReminderDialog = false },
            onSave = { reminders ->
                viewModel.saveAllWorkoutReminders(reminders)
                showWorkoutReminderDialog = false
            }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore from Cloud?") },
            text = {
                Text("This will replace ALL your current data (workout plans, nutrition, stats, profile, reminders) with the backup from Google Drive. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    viewModel.restoreFromGoogleDrive()
                }) {
                    Text("Restore", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showQuoteDialog) {
        MotivationalQuotesDialog(
            enabled = quoteEnabled,
            source = quoteSource,
            time = quoteTime,
            customQuotes = customQuotes,
            onDismiss = { showQuoteDialog = false },
            onToggleEnabled = { viewModel.setQuoteEnabled(it) },
            onSourceChange = { viewModel.setQuoteSource(it) },
            onTimeChange = { viewModel.setQuoteTime(it) },
            onAddCustomQuote = { viewModel.addCustomQuote(it) },
            onDeleteCustomQuote = { viewModel.deleteCustomQuote(it) }
        )
    }
}

// --- Google Sync Card ---

@Composable
fun GoogleSyncCard(
    accountEmail: String?,
    lastSyncTime: Long,
    syncState: SyncState,
    autoBackupEnabled: Boolean,
    onSignIn: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onSignOut: () -> Unit,
    onAutoBackupToggle: (Boolean) -> Unit
) {
    val isSignedIn = accountEmail != null
    val googleBlue = Color(0xFF4285F4)

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (!isSignedIn) {
                // Sign-in state
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(googleBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "G",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = googleBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Google Backup",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Sign in to backup & sync your data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(googleBlue)
                        .clickable(onClick = onSignIn)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sign in with Google",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            } else {
                // Signed-in state
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(googleBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            accountEmail.first().uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = googleBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Google Backup",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            accountEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Sign out",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Sync status
                if (syncState is SyncState.Loading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = googleBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            (syncState as SyncState.Loading).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = googleBlue
                        )
                    }
                } else if (syncState is SyncState.Success) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        (syncState as SyncState.Success).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF66BB6A),
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (syncState is SyncState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        (syncState as SyncState.Error).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Last synced
                if (lastSyncTime > 0 && syncState !is SyncState.Loading) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val formatted = remember(lastSyncTime) {
                        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            .format(Date(lastSyncTime))
                    }
                    Text(
                        "Last synced: $formatted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action buttons
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isLoading = syncState is SyncState.Loading
                    // Backup button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isLoading) MaterialTheme.colorScheme.surfaceVariant
                                else googleBlue.copy(alpha = 0.12f)
                            )
                            .clickable(enabled = !isLoading, onClick = onBackup)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = if (isLoading) MaterialTheme.colorScheme.onSurfaceVariant
                                else googleBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Backup",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLoading) MaterialTheme.colorScheme.onSurfaceVariant
                                else googleBlue
                            )
                        }
                    }
                    // Restore button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isLoading) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.secondaryContainer
                            )
                            .clickable(enabled = !isLoading, onClick = onRestore)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = if (isLoading) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Restore",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLoading) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Auto backup toggle
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (autoBackupEnabled) googleBlue.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (autoBackupEnabled) googleBlue
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Auto Backup",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (autoBackupEnabled) "Daily at 2:00 AM"
                            else "Disabled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = onAutoBackupToggle,
                        modifier = Modifier.height(24.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = googleBlue,
                            checkedTrackColor = googleBlue.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}

// --- Existing composables below (unchanged) ---

@Composable
fun ProfileCard(profile: UserProfile?, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person, contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = profile?.name?.ifBlank { "Tap to set up profile" } ?: "Tap to set up profile",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStat(
                    icon = Icons.Default.MonitorWeight, label = "Weight",
                    value = if (profile != null && profile.weight > 0)
                        "${formatProfileValue(profile.weight)} ${profile.weightUnit}" else "--",
                    color = Color(0xFF42A5F5)
                )
                ProfileStat(
                    icon = Icons.Default.Height, label = "Height",
                    value = if (profile != null && profile.height > 0)
                        "${formatProfileValue(profile.height)} ${profile.heightUnit}" else "--",
                    color = Color(0xFF66BB6A)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onEdit) { Text("Edit Profile") }
        }
    }
}

@Composable
fun ThemeToggleCard() {
    val context = LocalContext.current
    val darkModePref by ThemePreference.isDarkMode.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val isDark = darkModePref ?: systemDark

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(
                        if (isDark) Color(0xFF1A237E).copy(alpha = 0.3f)
                        else Color(0xFFFFB74D).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF90CAF9) else Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Dark Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    if (isDark) "Dark theme active" else "Light theme active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isDark,
                onCheckedChange = { ThemePreference.setDarkMode(context, it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun ProfileStat(icon: ImageVector, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PhotosSection(
    profile: UserProfile?,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (String) -> Unit
) {
    val photos = profile?.photoUris?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Progress Photos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onAddPhoto) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Add photo")
                }
            }
            if (photos.isEmpty()) {
                Text(
                    "No photos yet. Add your progress pics!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(photos) { uri ->
                        Box {
                            AsyncImage(
                                model = Uri.parse(uri), contentDescription = "Progress photo",
                                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { onRemovePhoto(uri) },
                                modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistSection(
    title: String, subtitle: String, items: List<ChecklistItem>, accentColor: Color,
    onToggle: (Int, Boolean) -> Unit, onDelete: (ChecklistItem) -> Unit, onAdd: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = accentColor)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                    onClick = onAdd,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = accentColor, modifier = Modifier.size(20.dp))
                }
            }
            if (items.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("No items yet. Tap + to add.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                items.forEach { item ->
                    ChecklistRow(item = item, accentColor = accentColor,
                        onToggle = { onToggle(item.id, !item.isChecked) },
                        onDelete = { onDelete(item) })
                }
            }
        }
    }
}

@Composable
fun ChecklistRow(item: ChecklistItem, accentColor: Color, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (item.isChecked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.text, style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
            color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun EditProfileDialog(
    profile: UserProfile?, onDismiss: () -> Unit,
    onSave: (String, Float, String, Float, String) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var weight by remember { mutableStateOf(if (profile != null && profile.weight > 0) formatProfileValue(profile.weight) else "") }
    var weightUnit by remember { mutableStateOf(profile?.weightUnit ?: "kg") }
    var height by remember { mutableStateOf(if (profile != null && profile.height > 0) formatProfileValue(profile.height) else "") }
    var heightUnit by remember { mutableStateOf(profile?.heightUnit ?: "cm") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Name") },
                    singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Text("Weight", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = weight, onValueChange = { weight = it },
                            placeholder = { Text(if (weightUnit == "kg") "e.g. 70" else "e.g. 154") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        UnitToggle(options = listOf("kg", "lb"), selected = weightUnit, onSelect = { weightUnit = it })
                    }
                }
                Column {
                    Text("Height", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = height, onValueChange = { height = it },
                            placeholder = { Text(if (heightUnit == "cm") "e.g. 175" else "e.g. 5.9") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        UnitToggle(options = listOf("cm", "ft"), selected = heightUnit, onSelect = { heightUnit = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(name.trim(), weight.toFloatOrNull() ?: 0f, weightUnit, height.toFloatOrNull() ?: 0f, heightUnit)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun UnitToggle(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option, style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatProfileValue(v: Float): String {
    return if (v == v.toLong().toFloat()) v.toLong().toString()
    else String.format("%.1f", v)
}

@Composable
fun AddChecklistDialog(title: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, label = { Text("Item") },
                placeholder = { Text("e.g. Drink 3L water daily") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onSave(text.trim()) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AIGoalsCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFAB47BC).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFAB47BC),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "AI Trainer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Create workout and nutrition goals using AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFAB47BC).copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "Soon",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFAB47BC)
                )
            }
        }
    }
}

@Composable
fun WorkoutNotificationCard(reminders: List<WorkoutReminder>, onClick: () -> Unit) {
    val dayShortNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val enabledReminders = reminders.filter { it.enabled && it.time.isNotBlank() }
    val enabledCount = enabledReminders.size

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (enabledCount > 0) Color(0xFF42A5F5).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (enabledCount > 0) Color(0xFF42A5F5)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Workout Reminders",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (enabledCount > 0) "$enabledCount day(s) active"
                        else "Tap to set up reminders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Show scheduled times
            if (enabledReminders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                // Check if all have the same time
                val allSameTime = enabledReminders.map { it.time }.distinct().size == 1
                if (allSameTime && enabledCount == 7) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF42A5F5),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Every day at ${formatTime12h(enabledReminders.first().time)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF42A5F5)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        enabledReminders.forEach { r ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF42A5F5).copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        dayShortNames.getOrElse(r.dayOfWeek) { "" },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF42A5F5)
                                    )
                                    Text(
                                        formatTime12h(r.time),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime12h(time: String): String {
    return try {
        val parts = time.split(":")
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        val amPm = if (h < 12) "AM" else "PM"
        val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
        String.format("%d:%02d %s", h12, m, amPm)
    } catch (_: Exception) {
        time
    }
}

@Composable
fun WorkoutReminderDialog(
    reminders: List<WorkoutReminder>,
    onDismiss: () -> Unit,
    onSave: (List<WorkoutReminder>) -> Unit
) {
    val context = LocalContext.current
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val reminderMap = reminders.associateBy { it.dayOfWeek }

    // State for each day
    val dayStates = remember(reminders) {
        (0..6).map { day ->
            val existing = reminderMap[day]
            Triple(
                mutableStateOf(existing?.enabled ?: false),
                mutableStateOf(existing?.time ?: ""),
                day
            )
        }
    }

    // Single time mode
    var singleTimeMode by remember { mutableStateOf(false) }
    var singleTime by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workout Reminders") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Same time for all days",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = singleTimeMode,
                        onCheckedChange = { singleTimeMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                if (singleTimeMode) {
                    // Single time picker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF42A5F5).copy(alpha = 0.1f))
                            .clickable {
                                val parts = singleTime.split(":")
                                val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
                                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                TimePickerDialog(context, { _, hour, minute ->
                                    singleTime = String.format("%02d:%02d", hour, minute)
                                }, h, m, false).show()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF42A5F5),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                if (singleTime.isNotBlank()) formatTime12h(singleTime) else "Tap to set time",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (singleTime.isNotBlank()) Color(0xFF42A5F5)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        "This will set the same time for all 7 days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    dayStates.forEach { (enabledState, timeState, day) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = enabledState.value,
                                onCheckedChange = { enabledState.value = it },
                                modifier = Modifier.size(36.dp),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                dayNames[day].take(3),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(36.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Time picker button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (enabledState.value) Color(0xFF42A5F5).copy(alpha = 0.1f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable(enabled = enabledState.value) {
                                        val parts = timeState.value.split(":")
                                        val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
                                        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        TimePickerDialog(context, { _, hour, minute ->
                                            timeState.value = String.format("%02d:%02d", hour, minute)
                                        }, h, m, false).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (enabledState.value && timeState.value.isNotBlank())
                                            Color(0xFF42A5F5)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (timeState.value.isNotBlank()) formatTime12h(timeState.value)
                                        else "Set time",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (timeState.value.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (enabledState.value && timeState.value.isNotBlank())
                                            Color(0xFF42A5F5)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = if (singleTimeMode && singleTime.isNotBlank()) {
                    (0..6).map { day ->
                        WorkoutReminder(dayOfWeek = day, time = singleTime.trim(), enabled = true)
                    }
                } else {
                    dayStates.map { (enabledState, timeState, day) ->
                        WorkoutReminder(
                            dayOfWeek = day,
                            time = timeState.value.trim(),
                            enabled = enabledState.value && timeState.value.isNotBlank()
                        )
                    }
                }
                onSave(result)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MotivationalQuotesCard(
    enabled: Boolean,
    source: String,
    time: String,
    customQuoteCount: Int,
    onClick: () -> Unit
) {
    val sourceLabel = when (source) {
        "APP" -> "App quotes"
        "CUSTOM" -> "Custom quotes ($customQuoteCount)"
        "BOTH" -> "App + Custom ($customQuoteCount)"
        else -> "App quotes"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (enabled) Color(0xFFFF7043).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = if (enabled) Color(0xFFFF7043)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Motivational Quotes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (enabled) "Daily at ${formatTime12h(time)}"
                        else "Tap to set up daily quotes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF7043).copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            sourceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF7043)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF7043).copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFFF7043),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                formatTime12h(time),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF7043)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MotivationalQuotesDialog(
    enabled: Boolean,
    source: String,
    time: String,
    customQuotes: List<MotivationalQuote>,
    onDismiss: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSourceChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onAddCustomQuote: (String) -> Unit,
    onDeleteCustomQuote: (MotivationalQuote) -> Unit
) {
    val context = LocalContext.current
    var newQuoteText by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = Color(0xFFFF7043),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Motivational Quotes")
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Enable/Disable toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Daily Quote Notification",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Get inspired every day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { onToggleEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFF7043),
                                checkedTrackColor = Color(0xFFFF7043).copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                if (enabled) {
                    // Notification time
                    item {
                        Text(
                            "Notification Time",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF7043).copy(alpha = 0.1f))
                                .clickable {
                                    val parts = time.split(":")
                                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    TimePickerDialog(context, { _, hour, minute ->
                                        onTimeChange(String.format("%02d:%02d", hour, minute))
                                    }, h, m, false).show()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFFFF7043),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    formatTime12h(time),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFF7043)
                                )
                            }
                        }
                    }

                    // Quote source
                    item {
                        Text(
                            "Quote Source",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuoteSourceOption(
                                label = "App's Dictionary",
                                description = "${MotivationalQuotes.quotes.size} curated quotes",
                                selected = source == "APP",
                                onClick = { onSourceChange("APP") }
                            )
                            QuoteSourceOption(
                                label = "Custom Only",
                                description = "${customQuotes.size} quote(s) added",
                                selected = source == "CUSTOM",
                                onClick = { onSourceChange("CUSTOM") }
                            )
                            QuoteSourceOption(
                                label = "App's Dictionary + Custom",
                                description = "${MotivationalQuotes.quotes.size + customQuotes.size} total quotes",
                                selected = source == "BOTH",
                                onClick = { onSourceChange("BOTH") }
                            )
                        }
                    }

                    // Custom quotes section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Custom Quotes",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${customQuotes.size} added",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Add new quote input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newQuoteText,
                                onValueChange = { newQuoteText = it },
                                placeholder = { Text("Enter your quote...", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (newQuoteText.isNotBlank()) Color(0xFFFF7043)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable(enabled = newQuoteText.isNotBlank()) {
                                        onAddCustomQuote(newQuoteText.trim())
                                        newQuoteText = ""
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add quote",
                                    tint = if (newQuoteText.isNotBlank()) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // List custom quotes
                    if (customQuotes.isNotEmpty()) {
                        items(customQuotes, key = { "cq_${it.id}" }) { quote ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = Color(0xFFFF7043).copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    quote.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { onDeleteCustomQuote(quote) }
                                )
                            }
                        }
                    }

                    // Preview a random quote
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .clickable { showPreview = !showPreview }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Preview Random Quote",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (showPreview) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val previewQuote = remember(showPreview) {
                                        when (source) {
                                            "APP" -> MotivationalQuotes.getRandomQuote()
                                            "CUSTOM" -> customQuotes.randomOrNull()?.text ?: MotivationalQuotes.getRandomQuote()
                                            "BOTH" -> {
                                                val all = MotivationalQuotes.quotes + customQuotes.map { it.text }
                                                all.randomOrNull() ?: MotivationalQuotes.getRandomQuote()
                                            }
                                            else -> MotivationalQuotes.getRandomQuote()
                                        }
                                    }
                                    Text(
                                        "\"$previewQuote\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun QuoteSourceOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) Color(0xFFFF7043).copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) Color(0xFFFF7043).copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) Color(0xFFFF7043) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color(0xFFFF7043) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ChecklistResetCard(onResetDay: () -> Unit, onResetWeek: () -> Unit) {
    var showResetDayConfirm by remember { mutableStateOf(false) }
    var showResetWeekConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Reset Checklists",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Uncheck all Do's & Don'ts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .clickable { showResetDayConfirm = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Reset Day",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        .clickable { showResetWeekConfirm = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Reset Week",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showResetDayConfirm) {
        AlertDialog(
            onDismissRequest = { showResetDayConfirm = false },
            title = { Text("Reset Day?") },
            text = { Text("This will uncheck all Do's and Don'ts for today.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDayConfirm = false
                    onResetDay()
                }) {
                    Text("Reset", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDayConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetWeekConfirm) {
        AlertDialog(
            onDismissRequest = { showResetWeekConfirm = false },
            title = { Text("Reset Week?") },
            text = { Text("This will uncheck all Do's and Don'ts and reset the weekly tracker.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetWeekConfirm = false
                    onResetWeek()
                }) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetWeekConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun FeedbackCard(onSendFeedback: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onSendFeedback() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Feedback,
                contentDescription = "Feedback",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Send Feedback",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Help us improve by sharing your thoughts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "›",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FeedbackDialog(onDismiss: () -> Unit, onSend: (String, String) -> Unit) {
    var subject by remember { mutableStateOf("Gym Workout App Feedback") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Feedback") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    placeholder = { Text("Tell us what you think...") },
                    minLines = 4,
                    maxLines = 8,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (message.isNotBlank()) onSend(subject.trim(), message.trim()) },
                enabled = message.isNotBlank()
            ) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
