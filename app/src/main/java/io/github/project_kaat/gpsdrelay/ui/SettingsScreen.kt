package io.github.project_kaat.gpsdrelay.ui

import android.app.KeyguardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.project_kaat.gpsdrelay.database.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.project_kaat.gpsdrelay.database.SettingsDao
import kotlinx.coroutines.launch
import io.github.project_kaat.gpsdrelay.R
import io.github.project_kaat.gpsdrelay.gpsdRelay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsDao : SettingsDao) {

    val settings : List<Settings> by settingsDao.getSettings().collectAsState(initial = emptyList())
    if (settings.isEmpty()) {
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val ctx = LocalContext.current

    val isServiceRunning = (ctx.applicationContext as gpsdRelay).serverManager.isServiceRunning.collectAsState()

    var generationIntervalTmp by remember {mutableStateOf(settings[0].nmeaGenerationIntervalMs.toString())}
    var autoStartEnabledTmp by remember {mutableStateOf(settings[0].autostartEnabled)}
    var autoStartTimeoutTmp by remember {mutableStateOf(settings[0].autostartNetworkTimeoutS.toString())}
    var monitorDefaultNetworkTmp by remember {mutableStateOf(settings[0].monitorDefaultNetworkEnabled)}

    var mutated by remember {mutableStateOf(false)}

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(stringResource(R.string.settings_title))
                },
                actions = {
                    Button(enabled = mutated, onClick = {
                        coroutineScope.launch {
                            try {
                                settingsDao.upsert(
                                    Settings(
                                        1,
                                        autoStartEnabledTmp,
                                        autoStartTimeoutTmp.toInt(),
                                        generationIntervalTmp.toLong(),
                                        monitorDefaultNetworkTmp
                                    )
                                )
                            } catch (_ : Exception) {
                                Toast.makeText(
                                    ctx,
                                    ctx.getString(R.string.settings_failed_to_apply),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }) {
                        Text(stringResource(R.string.settings_apply_button_text))
                    }
                }
            )
        }
    ) { innerPadding ->
        //TODO: is this scrollable?
        Column(modifier = Modifier
            .padding(innerPadding)
            .padding(horizontal = 10.dp, vertical = 4.dp)) {
            OutlinedTextField(
                label = { Text(stringResource(R.string.settings_generation_interval_title)) },
                value = generationIntervalTmp,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                onValueChange = {
                    mutated = true
                    generationIntervalTmp = it
                })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = autoStartEnabledTmp, onCheckedChange = {
                    autoStartEnabledTmp = !autoStartEnabledTmp
                    mutated = true
                    //check if device has a secure lockscreen and warn the user about it
                    if (autoStartEnabledTmp) {
                        val keyman = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                        if (keyman.isDeviceSecure) {
                            Toast.makeText(ctx, ctx.getString(R.string.settings_lockscreen_warning), Toast.LENGTH_LONG).show()
                        }
                    }
                })
                Text(stringResource(R.string.settings_autostart_title))
            }
            OutlinedTextField(
                label = { Text(stringResource(R.string.settings_autostart_timeout_title)) },
                value = autoStartTimeoutTmp.toString(),
                enabled = autoStartEnabledTmp,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                onValueChange = {
                    mutated = true
                    autoStartTimeoutTmp = it
                })


            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = monitorDefaultNetworkTmp, onCheckedChange = {
                    monitorDefaultNetworkTmp = !monitorDefaultNetworkTmp
                    mutated = true
                }, enabled = !isServiceRunning.value)
                Column() {
                    Text(stringResource(R.string.settings_monitor_default_network_title))
                    Text(stringResource(R.string.settings_monitor_default_network_explanation), fontWeight = FontWeight.Light, fontSize = 14.sp)
                }
            }

        }
    }

}
