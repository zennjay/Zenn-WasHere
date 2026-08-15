package com.example.settings

import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.HardwareCapabilities
import com.example.ui.theme.CameraYellow
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: CameraSettings,
    hardwareCapabilities: HardwareCapabilities,
    preferencesRepository: CameraPreferencesRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Storage stats
    val (storageUsed, storageAvailable) = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.availableBytes
            val bytesTotal = stat.totalBytes
            val bytesUsed = bytesTotal - bytesAvailable
            Pair(Formatter.formatFileSize(context, bytesUsed), Formatter.formatFileSize(context, bytesAvailable))
        } catch (_: Exception) {
            Pair("N/A", "N/A")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pengaturan Kamera",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Formats & Capture
            item {
                SettingsSectionHeader(title = "KAMERA & PENGAMBILAN GAMBAR")
                SettingsGroupCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.GridView,
                        title = "Kisi / Grid 3x3",
                        subtitle = "Bantu komposisi foto rule of thirds",
                        checked = settings.gridEnabled,
                        onCheckedChange = { scope.launch { preferencesRepository.updateGrid(it) } }
                    )
                    Divider()
                    SettingsSwitchRow(
                        icon = Icons.Default.Tune,
                        title = "Waterpass / Horizon Level",
                        subtitle = "Indikator kerataan sensor gyro",
                        checked = settings.levelEnabled,
                        onCheckedChange = { scope.launch { preferencesRepository.updateLevel(it) } }
                    )
                    Divider()
                    SettingsSwitchRow(
                        icon = Icons.Default.CameraAlt,
                        title = "Mirror Kamera Depan",
                        subtitle = "Balik foto selfie seperti cermin",
                        checked = settings.mirrorSelfieEnabled,
                        onCheckedChange = { scope.launch { preferencesRepository.updateMirrorSelfie(it) } }
                    )
                    Divider()
                    SettingsSwitchRow(
                        icon = Icons.Default.VolumeUp,
                        title = "Suara Rana / Shutter Sound",
                        subtitle = "Putar efek suara saat memotret",
                        checked = settings.shutterSoundEnabled,
                        onCheckedChange = { scope.launch { preferencesRepository.updateShutterSound(it) } }
                    )
                }
            }

            // Section: Video & Stabilization
            item {
                SettingsSectionHeader(title = "REKAMAN VIDEO & STABILISASI")
                SettingsGroupCard {
                    SettingsValueSelectorRow(
                        icon = Icons.Default.Videocam,
                        title = "Resolusi Video",
                        value = settings.videoResolution,
                        options = if (hardwareCapabilities.supports4K) listOf("720p", "1080p", "4K") else listOf("720p", "1080p"),
                        onSelected = { scope.launch { preferencesRepository.updateVideoResolution(it) } }
                    )
                    Divider()
                    SettingsValueSelectorRow(
                        icon = Icons.Default.Videocam,
                        title = "Frame Rate (FPS)",
                        value = "${settings.videoFps} FPS",
                        options = if (hardwareCapabilities.supports60Fps) listOf("30 FPS", "60 FPS") else listOf("30 FPS"),
                        onSelected = {
                            val fps = it.replace(" FPS", "").toIntOrNull() ?: 30
                            scope.launch { preferencesRepository.updateVideoFps(fps) }
                        }
                    )
                    Divider()
                    SettingsSwitchRow(
                        icon = Icons.Default.Tune,
                        title = "Stabilisasi Video (OIS / EIS)",
                        subtitle = if (hardwareCapabilities.supportsOis) "OIS Hardware Aktif" else if (hardwareCapabilities.supportsEis) "EIS Gyro Aktif" else "Stabilisasi Standar",
                        checked = settings.stabilizationEnabled,
                        onCheckedChange = { scope.launch { preferencesRepository.updateStabilization(it) } }
                    )
                }
            }

            // Section: Cinematic Mode
            item {
                SettingsSectionHeader(title = "MODE SINEMATIK")
                SettingsGroupCard {
                    SettingsValueSelectorRow(
                        icon = Icons.Default.Movie,
                        title = "Cinematic FPS",
                        value = "${settings.cinematicFps} FPS",
                        options = listOf("24 FPS", "30 FPS"),
                        onSelected = {
                            val fps = it.replace(" FPS", "").toIntOrNull() ?: 24
                            scope.launch { preferencesRepository.updateCinematicFps(fps) }
                        }
                    )
                    Divider()
                    SettingsValueSelectorRow(
                        icon = Icons.Default.Movie,
                        title = "Cinematic Resolusi",
                        value = settings.cinematicResolution,
                        options = if (hardwareCapabilities.supports4K) listOf("1080p", "4K") else listOf("1080p"),
                        onSelected = { scope.launch { preferencesRepository.updateCinematicRes(it) } }
                    )
                    Divider()
                    SettingsSwitchRow(
                        icon = Icons.Default.Tune,
                        title = "Smart Subject Tracking",
                        subtitle = "Kunci & ikuti fokus subjek secara mulus",
                        checked = settings.subjectTrackingEnabled,
                        onCheckedChange = { scope.launch { preferencesRepository.updateSubjectTracking(it) } }
                    )
                }
            }

            // Section: Storage
            item {
                SettingsSectionHeader(title = "PENYIMPANAN")
                SettingsGroupCard {
                    SettingsInfoRow(
                        icon = Icons.Default.SdStorage,
                        title = "Ruang Tersedia",
                        value = storageAvailable
                    )
                    Divider()
                    SettingsInfoRow(
                        icon = Icons.Default.SdStorage,
                        title = "Lokasi Simpan",
                        value = "DCIM/Camera (Lokal MediaStore)"
                    )
                }
            }

            // Section: Advanced & Reset
            item {
                SettingsSectionHeader(title = "TINGKAT LANJUT")
                SettingsGroupCard {
                    SettingsValueSelectorRow(
                        icon = Icons.Default.Tune,
                        title = "Peredam Bising / Noise Reduction",
                        value = settings.noiseReduction,
                        options = listOf("High Quality", "Balanced", "Fast"),
                        onSelected = { scope.launch { preferencesRepository.updateNoiseReduction(it) } }
                    )
                    Divider()
                    SettingsClickableRow(
                        icon = Icons.Default.Refresh,
                        title = "Reset Pengaturan Kamera",
                        subtitle = "Kembalikan semua konfigurasi ke awal",
                        onClick = { scope.launch { preferencesRepository.resetDefaults() } }
                    )
                }
            }

            // Section: About
            item {
                SettingsSectionHeader(title = "TENTANG")
                SettingsGroupCard {
                    SettingsInfoRow(
                        icon = Icons.Default.Info,
                        title = "Aplikasi",
                        value = "KAMERA IOS"
                    )
                    Divider()
                    SettingsInfoRow(
                        icon = Icons.Default.Info,
                        title = "Versi",
                        value = "1.0 (Flagship Studio)"
                    )
                    Divider()
                    SettingsInfoRow(
                        icon = Icons.Default.Info,
                        title = "Hardware Support",
                        value = if (hardwareCapabilities.hasUltraWide) "Multi-Lens Pro" else "Standard Lens"
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsGroupCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            content()
        }
    }
}

@Composable
fun Divider() {
    HorizontalDivider(color = DarkBorder, thickness = 0.6.dp)
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0x22FFCC00)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CameraYellow, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CameraYellow,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun SettingsValueSelectorRow(
    icon: ImageVector,
    title: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0x22FFCC00)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CameraYellow, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { opt ->
                val isSel = opt == value
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) CameraYellow else Color(0x331C1C1E))
                        .clickable { onSelected(opt) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = opt,
                        color = if (isSel) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0x22FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))

        Text(text = value, color = TextSecondary, fontSize = 13.sp)
    }
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0x22FF453A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFFF453A), modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}
