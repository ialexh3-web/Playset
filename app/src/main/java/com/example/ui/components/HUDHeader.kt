package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.BentoLightPurple
import com.example.ui.theme.BentoPurpleDark
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HUDHeader(
    isMasterAutoStart: Boolean,
    isDefaultLauncher: Boolean,
    onLaunchStockLauncher: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onTriggerAutoStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            delay(1000)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground),
        color = DarkBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title & Current Time
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ottocast AI",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = BentoLightPurple
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "• $currentTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            // Right Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // TcLauncher Button (com.tc.launcher2)
                Surface(
                    shape = CircleShape,
                    color = BentoPurpleDark,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onLaunchStockLauncher() }
                        .testTag("btn_stock_launcher")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "TcLauncher",
                            tint = BentoLightPurple,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "TcLauncher",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Default OttoLauncher Status / Selector pill (opens settings on click)
                Surface(
                    shape = CircleShape,
                    color = if (isDefaultLauncher) AccentGreen.copy(alpha = 0.2f) else AccentOrange.copy(alpha = 0.2f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onOpenSettings() }
                        .testTag("launcher_status_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "OttoLauncher",
                            tint = if (isDefaultLauncher) AccentGreen else AccentOrange,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isDefaultLauncher) "OttoLauncher" else "OttoLauncher (выбрать)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDefaultLauncher) AccentGreen else AccentOrange
                        )
                    }
                }

                // Trigger AutoStart button (matching height of pills)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(BentoLightPurple, CircleShape)
                        .clip(CircleShape)
                        .clickable { onTriggerAutoStart() }
                        .testTag("btn_header_trigger_autostart"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        tint = BentoPurpleDark,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}


