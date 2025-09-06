package io.github.project_kaat.gpsdrelay.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.project_kaat.gpsdrelay.database.GpsdServerType
import io.github.project_kaat.gpsdrelay.database.Server
import io.github.project_kaat.gpsdrelay.database.ServerDao
import kotlinx.coroutines.launch
import io.github.project_kaat.gpsdrelay.R

@Composable
fun MainScreenAddServerDialog(dao : ServerDao, onDismiss : () -> Unit, checkServerType : () -> GpsdServerType) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    val serverType = checkServerType()

    var ipv4temp by remember {mutableStateOf("0.0.0.0")}
    var porttemp by remember {mutableStateOf("")}
    var relayingEnabledTemp by remember {mutableStateOf(true)}
    var generationEnabledTemp by remember {mutableStateOf(true)}

    AlertDialog( onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string.add_server, if (serverType == GpsdServerType.UDP) {
                        stringResource(R.string.udp_server)
                    } else {
                        stringResource(R.string.tcp_server)
                    }
                )
            )
        },
        text = {

            Column() {
                OutlinedTextField(
                    value = ipv4temp,
                    label = { Text(stringResource(R.string.add_dialog_ipv4_title)) },
                    placeholder = {
                        Text(
                            stringResource(
                                R.string.add_dialog_ipv4_placeholder,
                                if (serverType == GpsdServerType.TCP) {
                                    stringResource(R.string.add_dialog_placeholder_tcp)
                                } else {
                                    stringResource(R.string.add_dialog_placeholder_udp)
                                }
                            )
                        )
                    },
                    onValueChange = {
                    ipv4temp = it
                })
                OutlinedTextField(
                    value = porttemp,
                    label = { Text(stringResource(R.string.add_dialog_port_title)) },
                    placeholder = {
                        Text(
                            stringResource(
                                R.string.add_dialog_port_placeholder,
                                if (serverType == GpsdServerType.TCP) {
                                    stringResource(R.string.add_dialog_placeholder_tcp)
                                } else {
                                    stringResource(R.string.add_dialog_placeholder_udp)
                                }
                            )
                        )
                    },
                    onValueChange = {
                        porttemp = it

                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.add_dialog_enable_relaying_title))
                    Checkbox(checked = relayingEnabledTemp, onCheckedChange = {
                        relayingEnabledTemp = !relayingEnabledTemp
                    })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.add_dialog_enable_generation_title))
                    Checkbox(checked = generationEnabledTemp, onCheckedChange = {
                        generationEnabledTemp = !generationEnabledTemp
                    })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newServer = Server.validateAndCreate(
                    serverType,
                    ipv4temp,
                    porttemp,
                    relayingEnabledTemp,
                    generationEnabledTemp,
                    System.currentTimeMillis(),
                    false,
                    emptyList()
                )
                if (newServer == null) {
                    Toast.makeText(
                        ctx,
                        ctx.getString(R.string.add_dialog_invalid_data),
                        Toast.LENGTH_LONG
                    ).show()
                }
                else {
                    scope.launch() {
                        dao.upsert(newServer)
                    }
                    onDismiss()
                }
            }) {
                Text(stringResource(R.string.add_dialog_add_button_text))
            }
        })
}