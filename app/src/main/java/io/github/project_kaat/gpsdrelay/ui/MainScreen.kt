package io.github.project_kaat.gpsdrelay.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.project_kaat.gpsdrelay.SettingsActivity
import io.github.project_kaat.gpsdrelay.database.GpsdServerType
import io.github.project_kaat.gpsdrelay.database.Server
import io.github.project_kaat.gpsdrelay.database.ServerDao
import io.github.project_kaat.gpsdrelay.gpsdRelay
import io.github.project_kaat.gpsdrelay.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(dao : ServerDao) {

    val ctx = LocalContext.current
    val serverManager = (ctx.applicationContext as gpsdRelay).serverManager

    val servers : List<Server> by dao.getAllSortByCreationTime().collectAsState(initial=emptyList())
    val isServiceRunning by (ctx.applicationContext as gpsdRelay).serverManager.isServiceRunning.collectAsState()

    var showingAddServerDialog by remember {mutableStateOf(false)}
    var addingServerType by remember{mutableStateOf(GpsdServerType.TCP)}

    var locationPermissionGranted by remember { mutableStateOf(serverManager.checkLocationPermission()) }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            locationPermissionGranted = it
        }
    LaunchedEffect(locationPermissionGranted) {
        if (!locationPermissionGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (locationPermissionGranted) {
            var bgLocationPermissionGranted by remember { mutableStateOf(serverManager.checkBackgroundLocationPermission()) }

            val bgLocationPermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                    bgLocationPermissionGranted = it
                }


            LaunchedEffect(bgLocationPermissionGranted) {
                if (!bgLocationPermissionGranted) {
                    bgLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    Toast.makeText(
                        ctx,
                        ctx.getString(R.string.bg_location_permission_rationale),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(stringResource(R.string.app_name))
                },
                actions = {
                    FilledIconButton(shape = RoundedCornerShape(10.dp),
                        onClick= {
                            if (isServiceRunning) {
                                (serverManager.stopService())
                            }
                            else {
                                serverManager.startService()
                            }
                        }) {

                        if (isServiceRunning) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.stop_service_icon_description))
                        }
                        else {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.start_service_icon_description))
                        }
                    }

                    FilledIconButton(shape = RoundedCornerShape(10.dp), onClick = {
                        ctx.startActivity((Intent(ctx, SettingsActivity::class.java)))
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_icon_description))
                    }
                }
            )
        },
        floatingActionButton = {
            var fabMenuExpanded by remember { mutableStateOf(false) }
            FloatingActionButton(onClick = {
                fabMenuExpanded = true
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_icon_description))
                DropdownMenu(expanded = fabMenuExpanded, onDismissRequest = {
                    fabMenuExpanded = false
                }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.udp_server))},
                        onClick = {
                            addingServerType = GpsdServerType.UDP
                            showingAddServerDialog = true
                            fabMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tcp_server))},
                        onClick = {
                            addingServerType = GpsdServerType.TCP
                            showingAddServerDialog = true
                            fabMenuExpanded = false
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (servers.isEmpty()) {
            Row (modifier = Modifier.padding(innerPadding).fillMaxWidth()) {
                Text(stringResource(R.string.no_servers_placeholder), style= MaterialTheme.typography.bodyLarge, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
        else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                for (server in servers) {
                    MainScreenServerElement(server = server, dao = dao)
                }
                Text(stringResource(R.string.remove_servers_hint), style= MaterialTheme.typography.bodyLarge, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }

    if (showingAddServerDialog) {
        MainScreenAddServerDialog(dao, onDismiss = {
            showingAddServerDialog = false
        }, checkServerType = {
            addingServerType
        })
    }
}