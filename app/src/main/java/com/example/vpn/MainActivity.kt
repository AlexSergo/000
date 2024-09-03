package com.example.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val START_VPN_REQUEST_CODE = 1
    private var whiteList by mutableStateOf(setOf<String>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FirewallApp(whiteList) { newWhiteList ->
                whiteList = newWhiteList
            }
        }
    }

    @Composable
    fun FirewallApp(whiteList: Set<String>, onWhiteListChange: (Set<String>) -> Unit) {
        var url by remember { mutableStateOf("") }

        MaterialTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
//                OutlinedTextField(
//                    value = url,
//                    onValueChange = { url = it },
//                    label = { Text("Enter allowed URL") },
//                    modifier = Modifier.fillMaxWidth()
//                )
//                Button(
//                    onClick = {
//                        if (url.isNotEmpty()) {
//                            onWhiteListChange(whiteList + url)
//                            url = ""
//                            Toast.makeText(this@MainActivity, "URL added to whitelist", Toast.LENGTH_SHORT).show()
//                        }
//                    },
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Text("Add URL")
//                }
                Button(
                    onClick = { startVpnService() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start VPN")
                }
                Button(
                    onClick = { stopVpnService() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Stop VPN")
                }
            }
        }
    }

    private fun startVpnService() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, START_VPN_REQUEST_CODE)
        } else {
            onActivityResult(START_VPN_REQUEST_CODE, RESULT_OK, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == START_VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            val intent = Intent(this, FirewallService::class.java).apply {
                putExtra("whiteList", whiteList.toTypedArray())
            }
            startService(intent)
        }
    }

    private fun stopVpnService() {
        val intent = Intent(this, FirewallService::class.java)
        stopService(intent)
    }
}
