package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.HardwareCapabilities
import com.example.camera.ProSettings
import com.example.ui.theme.CameraYellow
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.GlassSurfaceElevated

private enum class ProTab(val label: String) {
    ISO("ISO"),
    SHUTTER("SEC"),
    EV("EV"),
    WB("WB"),
    FOCUS("FOCUS")
}

@Composable
fun ProControlsBar(
    proSettings: ProSettings,
    hardwareCapabilities: HardwareCapabilities,
    onUpdateProSettings: ((ProSettings) -> ProSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ProTab.ISO) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GlassSurfaceDark)
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tab Pills (ISO, SEC, EV, WB, FOCUS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val badgeText = when (tab) {
                    ProTab.ISO -> if (proSettings.isIsoAuto) "ISO AUTO" else "ISO ${proSettings.iso}"
                    ProTab.SHUTTER -> if (proSettings.isShutterAuto) "SEC AUTO" else "1/${proSettings.shutterSpeedIndex}s"
                    ProTab.EV -> if (proSettings.evCompensation == 0) "EV 0.0" else "EV ${if (proSettings.evCompensation > 0) "+" else ""}${proSettings.evCompensation}"
                    ProTab.WB -> if (proSettings.isWbAuto) "WB AUTO" else "${proSettings.wbKelvin}K"
                    ProTab.FOCUS -> if (proSettings.isFocusAuto) "AF" else "MF ${(proSettings.focusDistance * 100).toInt()}%"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) CameraYellow else GlassSurfaceElevated)
                        .border(1.dp, if (isSelected) CameraYellow else GlassBorder, RoundedCornerShape(14.dp))
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("pro_tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Tab Controls Slider / Options
        when (selectedTab) {
            ProTab.ISO -> {
                val isoList = listOf(0, 100, 200, 400, 800, 1600, 3200)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    isoList.forEach { iso ->
                        val isAuto = iso == 0
                        val isSel = if (isAuto) proSettings.isIsoAuto else (!proSettings.isIsoAuto && proSettings.iso == iso)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) Color(0x66FFCC00) else GlassSurfaceElevated)
                                .border(1.dp, if (isSel) CameraYellow else GlassBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                onUpdateProSettings { it.copy(isIsoAuto = isAuto, iso = if (isAuto) 100 else iso) }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isAuto) "AUTO" else "$iso",
                                color = if (isSel) CameraYellow else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            ProTab.SHUTTER -> {
                val shutterList = listOf(0, 2000, 1000, 500, 250, 125, 60, 30, 15, 4, 1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    shutterList.forEach { speed ->
                        val isAuto = speed == 0
                        val isSel = if (isAuto) proSettings.isShutterAuto else (!proSettings.isShutterAuto && proSettings.shutterSpeedIndex == speed)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) Color(0x66FFCC00) else GlassSurfaceElevated)
                                .border(1.dp, if (isSel) CameraYellow else GlassBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                onUpdateProSettings { it.copy(isShutterAuto = isAuto, shutterSpeedIndex = if (isAuto) 0 else speed) }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isAuto) "AUTO" else "1/${speed}",
                                color = if (isSel) CameraYellow else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            ProTab.EV -> {
                Slider(
                    value = proSettings.evCompensation.toFloat(),
                    onValueChange = { ev ->
                        onUpdateProSettings { it.copy(evCompensation = ev.toInt()) }
                    },
                    valueRange = -4f..4f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = CameraYellow,
                        activeTrackColor = CameraYellow,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
            ProTab.WB -> {
                val wbList = listOf(0, 3200, 4000, 5500, 6500, 7500)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wbList.forEach { wb ->
                        val isAuto = wb == 0
                        val isSel = if (isAuto) proSettings.isWbAuto else (!proSettings.isWbAuto && proSettings.wbKelvin == wb)
                        val label = when (wb) {
                            0 -> "AUTO"
                            3200 -> "3200K Tungsten"
                            4000 -> "4000K Fluorescent"
                            5500 -> "5500K Daylight"
                            6500 -> "6500K Cloudy"
                            else -> "7500K Shade"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) Color(0x66FFCC00) else GlassSurfaceElevated)
                                .border(1.dp, if (isSel) CameraYellow else GlassBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                onUpdateProSettings { it.copy(isWbAuto = isAuto, wbKelvin = if (isAuto) 5500 else wb) }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) CameraYellow else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            ProTab.FOCUS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (proSettings.isFocusAuto) CameraYellow else GlassSurfaceElevated)
                            .border(1.dp, if (proSettings.isFocusAuto) CameraYellow else GlassBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            onUpdateProSettings { it.copy(isFocusAuto = !it.isFocusAuto) }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (proSettings.isFocusAuto) "AF (AUTO)" else "MANUAL",
                            color = if (proSettings.isFocusAuto) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    if (!proSettings.isFocusAuto) {
                        Slider(
                            value = proSettings.focusDistance,
                            onValueChange = { dist ->
                                onUpdateProSettings { it.copy(isFocusAuto = false, focusDistance = dist) }
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = CameraYellow,
                                activeTrackColor = CameraYellow,
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
