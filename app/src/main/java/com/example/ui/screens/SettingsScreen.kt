package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceDarkVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    isDefaultLauncher: Boolean,
    masterAutoStartEnabled: Boolean,
    globalDelay: Int,
    triggerOnBoot: Boolean,
    triggerOnPower: Boolean,
    triggerOnScreen: Boolean,
    onSetDefaultLauncher: () -> Unit,
    onMasterAutoStartChange: (Boolean) -> Unit,
    onGlobalDelayChange: (Int) -> Unit,
    onTriggerBootChange: (Boolean) -> Unit,
    onTriggerPowerChange: (Boolean) -> Unit,
    onTriggerScreenChange: (Boolean) -> Unit,
    onTestAutoStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(SurfaceDark, RoundedCornerShape(12.dp))
                    .testTag("btn_back_settings")
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Настройки системы Ottocast",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryCyan
                )
                Text(
                    text = "Параметры выхода из глубокого сна и выключения экрана",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Section 1: Default Home App Launcher Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.5.dp, if (isDefaultLauncher) AccentGreen else AccentOrange, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDefaultLauncher) AccentGreen.copy(alpha = 0.2f) else AccentOrange.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = if (isDefaultLauncher) AccentGreen else AccentOrange
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isDefaultLauncher) "Лаунчер по умолчанию установлен" else "Сделать OttoLauncher штатным",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isDefaultLauncher) "Автозапуск активен при каждом нажатии Home" else "Нажмите, чтобы зафиксировать замену штатного рабочего стола",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Button(
                    onClick = onSetDefaultLauncher,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDefaultLauncher) SurfaceDarkVariant else AccentOrange,
                        contentColor = if (isDefaultLauncher) PrimaryCyan else DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_set_default_launcher")
                ) {
                    Text(if (isDefaultLauncher) "Изменить" else "Выбрать", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 2: Master AutoStart & Global Delay
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ОБЩИЕ НАСТРОЙКИ АВТОЗАПУСКА",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Master Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Автозапуск выбранных приложений", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Глобальный переключатель работы автостарта", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = masterAutoStartEnabled,
                        onCheckedChange = onMasterAutoStartChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryCyan, checkedTrackColor = PrimaryCyan.copy(alpha = 0.3f)),
                        modifier = Modifier.testTag("switch_master_autostart")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Global Start Delay
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Начальная задержка перед стартом", fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("$globalDelay сек", fontWeight = FontWeight.Bold, color = PrimaryCyan)
                    }
                    Text("Пауза для инициализации GPS, Wi-Fi и Bluetooth в системе", fontSize = 12.sp, color = TextSecondary)
                    Slider(
                        value = globalDelay.toFloat(),
                        onValueChange = { onGlobalDelayChange(it.toInt()) },
                        valueRange = 1f..15f,
                        steps = 14,
                        colors = SliderDefaults.colors(thumbColor = PrimaryCyan, activeTrackColor = PrimaryCyan)
                    )
                }
            }
        }

        // Section 3: Trigger Conditions for Ottocast Deep Sleep
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "СОБЫТИЯ СРАБАТЫВАНИЯ (OTTOCAST SUSPEND / DEEP SLEEP)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Boot trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Холодный старт системы (BOOT)", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Запуск при полных перезагрузках Android", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = triggerOnBoot,
                        onCheckedChange = onTriggerBootChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryCyan)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Power Connected trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Подача питания (ACC / 12V)", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Запуск при подключении внешнего питания", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = triggerOnPower,
                        onCheckedChange = onTriggerPowerChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryCyan)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Screen On trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Включение экрана (Выход из сна)", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Срабатывание при просыпании Ottocast Ottoscreen", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = triggerOnScreen,
                        onCheckedChange = onTriggerScreenChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryCyan)
                    )
                }
            }
        }

        // Section 4: Manual Test Button
        Button(
            onClick = onTestAutoStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .testTag("btn_test_autostart_now"),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = DarkBackground)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Протестировать цикл автозапуска прямо сейчас", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info box about Ottocast Ottoscreen AI
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkBackground)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Совет для Ottocast Ottoscreen AI: назначьте OttoLauncher главным приложением Рабочего Стола (Home App). После включения зажигания или подачи питания Android мгновенно загружает OttoLauncher и автоматически открывает ваши Яндекс Карты, Навигатор или Музыку.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
