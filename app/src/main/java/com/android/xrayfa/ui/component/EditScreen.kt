package com.android.xrayfa.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.xrayfa.model.protocol.Protocol


@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditScreen() {


    var selectedProtocol by remember { mutableStateOf(Protocol.VLESS) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val collapsedFraction = scrollBehavior.state.collapsedFraction
                    val options = Protocol.entries
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "Edit")

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            items(items = options, key = {it}) { label ->
                                ToggleButton(
                                    checked = selectedProtocol == label,
                                    onCheckedChange = { selectedProtocol = label },
                                    shapes =
                                        when (label) {
                                            Protocol.VLESS ->  ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            Protocol.TROJAN ->  ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        },
                                    modifier = Modifier.semantics { role = Role.RadioButton },
                                ) {
                                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                    Text(label.name.lowercase())
                                }
                            }
                            options.forEachIndexed { index, label ->

                            }
                        }
                        // Main title is always visible


                    }
                },
                actions = {
                    FloatingActionButton(
                        onClick = { /*todo */},
                        shape = IconButtonDefaults.extraSmallRoundShape,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "save edit"
                        )
                    }

                },
                scrollBehavior = scrollBehavior
            )

        }
    ) { paddingValue ->
        Box(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValue)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when(selectedProtocol) {
                Protocol.VLESS -> { VlessEdit() }
                Protocol.VMESS -> { VmessEdit() }
                Protocol.SHADOW_SOCKS -> { ShadowsocksEdit() }
                Protocol.TROJAN -> { TrojanEdit() }
            }
        }
    }

}

@Composable
fun VlessEdit() {
    Text("VLESS")
}

@Composable
fun VmessEdit() {
    Text("VMESS")
}

@Composable
fun ShadowsocksEdit() {
    Text("shadowsocks")
}

@Composable
fun TrojanEdit() {
    Text("trojan")
}


@Composable
@Preview(device = "id:pixel_5")
fun EditScreenPreview() {
    EditScreen()
}