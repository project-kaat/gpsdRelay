package io.github.project_kaat.gpsdrelay.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.project_kaat.gpsdrelay.database.GpsdServerType
import io.github.project_kaat.gpsdrelay.database.Server
import io.github.project_kaat.gpsdrelay.database.ServerDao
import kotlinx.coroutines.launch
import io.github.project_kaat.gpsdrelay.R
import org.intellij.lang.annotations.JdkConstants
import java.net.NetworkInterface

@Composable
fun MainScreenAddServerDialog(dao : ServerDao, onDismiss : () -> Unit, checkServerType : () -> GpsdServerType) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    val serverType = checkServerType()

    var ipv4temp by remember {mutableStateOf("0.0.0.0")}
    var porttemp by remember {mutableStateOf("")}
    var relayingEnabledTemp by remember {mutableStateOf(true)}
    var generationEnabledTemp by remember {mutableStateOf(true)}
    var showAddBroadcastServerDialog by remember {mutableStateOf(false)}

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
            Column(horizontalAlignment = Alignment.End) {
            if (GpsdServerType.UDP == serverType) {
                OutlinedButton(onClick = {
                    showAddBroadcastServerDialog = true;
                }) {
                    Text(stringResource(R.string.add_dialog_add_broadcast_button_text))
                }
            }
                Button(onClick = {
                    val newServer = Server.validateAndCreate(serverType, ipv4temp, porttemp, relayingEnabledTemp, generationEnabledTemp, System.currentTimeMillis(), false)
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
                },
                    enabled = (ipv4temp.isNotBlank() && porttemp.isNotBlank())) {
                    Text(stringResource(R.string.add_dialog_add_button_text))
                }
            }
        }
        /*confirmButton = {
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
        })*/
    )

    if (showAddBroadcastServerDialog) {
        AddBroadcastServerDialog(
            onAddressSelected = {
                ipv4temp = it
                showAddBroadcastServerDialog = false
            },
            onDismiss = {
                showAddBroadcastServerDialog = false
            }
        )
    }
}

data class BroadcastNetworkAddressElement(val interfaceName : String, val ipv4 : String)

@Composable
fun AddBroadcastServerDialog(onAddressSelected : (String) -> Unit, onDismiss: () -> Unit) {

    val broadcastInterfaces : MutableList<BroadcastNetworkAddressElement> = mutableListOf()

    val interfaces = NetworkInterface.getNetworkInterfaces()
    for (iface in interfaces) {
        if (iface.isUp && !iface.isLoopback) {
            for (interfaceAddress in iface.interfaceAddresses) {
                val broadcast = interfaceAddress.broadcast
                if (interfaceAddress.broadcast != null) {
                    broadcastInterfaces += BroadcastNetworkAddressElement(
                        iface.displayName,
                        broadcast.toString().substring(1)
                    )
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_broadcast_dialog_title), fontSize = 18.sp) },

        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn {
                    items(broadcastInterfaces) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddressSelected(item.ipv4)
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = item.interfaceName)
                            Text(text = item.ipv4)
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {}
    )

}