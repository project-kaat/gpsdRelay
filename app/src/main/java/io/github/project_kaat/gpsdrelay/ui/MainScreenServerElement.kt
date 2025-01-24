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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.project_kaat.gpsdrelay.database.GpsdServerType
import io.github.project_kaat.gpsdrelay.database.Server
import io.github.project_kaat.gpsdrelay.database.ServerDao
import kotlinx.coroutines.launch
import io.github.project_kaat.gpsdrelay.R

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
                            Text(
                                stringResource(R.string.add_dialog_enable_relaying_title),
                                modifier = Modifier.padding(vertical = 14.dp)
                            )
                            Checkbox(checked = server.relayingEnabled, onCheckedChange = {
                                scope.launch() {
                                    dao.upsert(server.copy(relayingEnabled = !server.relayingEnabled))
                                }
                            })
                        }
                    }
                    Column() {
                        Row() {
                            Text(
                                stringResource(R.string.add_dialog_enable_generation_title),
                                modifier = Modifier.padding(vertical = 14.dp)
                            )
                            Checkbox(checked = server.generationEnabled, onCheckedChange = {
                                scope.launch() {
                                    dao.upsert(server.copy(generationEnabled = !server.generationEnabled))
                                }
                            })
                        }
                    }
                }

            }

        }

    }
}