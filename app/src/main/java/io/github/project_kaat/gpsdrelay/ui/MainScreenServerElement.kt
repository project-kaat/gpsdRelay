package io.github.project_kaat.gpsdrelay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.project_kaat.gpsdrelay.database.GpsdServerType
import io.github.project_kaat.gpsdrelay.database.Server
import io.github.project_kaat.gpsdrelay.database.ServerDao
import kotlinx.coroutines.launch
import io.github.project_kaat.gpsdrelay.R
import io.github.project_kaat.gpsdrelay.database.ServerTypeConverters
import io.github.project_kaat.gpsdrelay.database.staticFromListOfStringToString
import io.github.project_kaat.gpsdrelay.database.staticFromStringToListOfString

@Composable
fun ExpandableSectionTitle(modifier: Modifier = Modifier, isExpanded: Boolean, server : Server, dao : ServerDao) {

    val icon = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown

    val scope = rememberCoroutineScope()

    Row(modifier = modifier
        .padding(8.dp)
        .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Absolute.SpaceBetween) {
        Image(
            modifier = Modifier.size(24.dp),
            imageVector = icon,
            contentDescription = stringResource(R.string.server_menu_collapse_icon_description)
        )
        Text(text = "${if (server.type == GpsdServerType.TCP.code) "T" else "U"}:${server.ipv4}:${server.port}", style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = server.enabled,
            onCheckedChange = {
                scope.launch() {
                    //only 1 tcp server is supported for now
                    if (server.type == GpsdServerType.TCP.code) {
                        dao.deactivateAllTcp()
                    }
                    dao.upsert(server.copy(enabled = !server.enabled))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreenServerElement(modifier : Modifier = Modifier, server : Server, dao : ServerDao) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .combinedClickable(onClick = {
                expanded = !expanded
            }, onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch() {
                    dao.delete(server)
                }
            })
            .fillMaxWidth()
    ) {
        ExpandableSectionTitle(isExpanded = expanded, server = server, dao = dao)
        AnimatedVisibility(
            modifier = Modifier.fillMaxWidth(),
            visible = expanded
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp)) {
                Row() {
                    Column() {
                        Row() {
                            Checkbox(checked = server.relayingEnabled, onCheckedChange = {
                                scope.launch() {
                                    dao.upsert(server.copy(relayingEnabled = !server.relayingEnabled))
                                }
                            })
                            Text(
                                stringResource(R.string.add_dialog_enable_relaying_title),
                                modifier = Modifier.padding(vertical = 14.dp)
                            )
                        }
                        Row() {
                            Checkbox(checked = server.generationEnabled, onCheckedChange = {
                                scope.launch() {
                                    dao.upsert(server.copy(generationEnabled = !server.generationEnabled))
                                }
                            })
                            Text(
                                stringResource(R.string.add_dialog_enable_generation_title),
                                modifier = Modifier.padding(vertical = 14.dp)
                            )
                        }
                        Text(stringResource(R.string.relay_filter_title), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.relay_filter_help), fontWeight = FontWeight.Light)
                        Row() {
                            RelayFilterCheckbox(dao, server, "GGA") ; RelayFilterCheckbox(dao, server, "GLL")
                        }
                        Row() {
                            RelayFilterCheckbox(dao, server, "GSA") ; RelayFilterCheckbox(dao, server, "GSV")
                        }
                        Row() {
                            RelayFilterCheckbox(dao, server, "RMC") ; RelayFilterCheckbox(dao, server, "VTG")
                        }
                        val rawFilter = staticFromListOfStringToString(server.relayFilter)
                        var editingCustomFilter by remember {mutableStateOf(false)}
                        var rawFilterEditBuffer by remember {mutableStateOf(rawFilter)}
                        OutlinedTextField(
                            value = if (!editingCustomFilter) rawFilter else rawFilterEditBuffer,
                            label = { Text(stringResource(R.string.relay_filter_raw_title)) },
                            onValueChange = {
                                editingCustomFilter = true
                                rawFilterEditBuffer = it
                            },
                            trailingIcon = {
                                if (editingCustomFilter) {
                                    Row() {
                                        IconButton(
                                            content = {
                                                Icon(
                                                    Icons.Default.Check,
                                                    stringResource(R.string.server_menu_custom_filter_save_icon_description)
                                                )
                                            },
                                            onClick = {
                                                val newServer = server.copy(
                                                    relayFilter = staticFromStringToListOfString(
                                                        rawFilterEditBuffer.uppercase()
                                                    )
                                                )
                                                scope.launch { dao.upsert(newServer) }
                                                editingCustomFilter = false
                                            })
                                        IconButton(
                                            content = {
                                                Icon(
                                                    Icons.Default.Close,
                                                    stringResource(R.string.server_menu_custom_filter_cancel_icon_description)
                                                )
                                            },
                                            onClick = {
                                                editingCustomFilter = false
                                            })
                                    }
                                }
                            },
                            modifier = Modifier.onFocusChanged {
                                if (!it.isFocused) {
                                    editingCustomFilter = false
                                }
                            }.padding(end = 70.dp))
                    }
                }

            }

        }

    }
}

@Composable
fun RelayFilterCheckbox(dao : ServerDao, server : Server, sentenceType : String) {
    var enabled = sentenceType in server.relayFilter
    val scope = rememberCoroutineScope()
    Row() {
        Checkbox(checked = enabled, onCheckedChange = {
            val newServer = if (enabled) {
                server.copy(relayFilter = server.relayFilter - sentenceType)
            }
            else {
                server.copy(relayFilter = server.relayFilter + sentenceType)
            }
            enabled = !enabled

            scope.launch { dao.upsert(newServer) }
        })
        Text(sentenceType, modifier = Modifier.padding(vertical = 14.dp))
    }
}