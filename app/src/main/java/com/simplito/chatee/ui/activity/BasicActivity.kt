//
// PrivMX Chatee Android.
// Copyright © 2024 Simplito sp. z o.o.
//
// This file is part of demonstration software for the PrivMX Platform (https://privmx.dev).
// This software is Licensed under the MIT License.
//
// See the License for the specific language governing permissions and
// limitations under the License.
//


package com.simplito.chatee.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.simplito.chatee.ChateeSession
import com.simplito.chatee.R
import com.simplito.chatee.classes.async.AsyncCall
import com.simplito.chatee.classes.async.Promise
import com.simplito.chatee.extension.endpoint
import com.simplito.chatee.extension.mExecutor
import com.simplito.chatee.server.ServerApi
import com.simplito.chatee.ui.theme.PrivmxChateeTheme
import com.simplito.java.privmx_endpoint_android.activities.PrivmxEndpointBaseActivity
import com.simplito.java.privmx_endpoint_extra.events.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

abstract class BasicActivity : PrivmxEndpointBaseActivity() {
    companion object {
        private const val LOGOUT_INFO_EXTRA = "com.simplito.chatee.BasicActivity.LogoutInfo"
        private const val TAG = "[BasicActivity]"
        private const val CONNECTION_CALLBACK_ID =
            "com.simplito.chatee.BasicActivity-CONNECTION_CALLBACKS_ID"
    }

    private val snackBarHostState = SnackbarHostState()
    private var disconnecting = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChateeSession.loadFromCache(this@BasicActivity)
        setContent {
            val focusManager = LocalFocusManager.current
            val interactionSource = remember {
                MutableInteractionSource()
            }
            PrivmxChateeTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        }
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Content()
                    SnackbarHost(
                        hostState = snackBarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(10.dp)
                    ) {
                        Snackbar(
                            containerColor = Color(0xFFC41D3E),
                            contentColor = Color.White,
                            dismissActionContentColor = Color.White,
                            dismissAction = {
                                Icon(
                                    Icons.Filled.Close,
                                    "dismiss action",
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                        .clickable {
                                            it.dismiss()
                                        })
                            },
                        ) {
                            Text(text = it.visuals.message)
                        }

                    }
                }
            }
        }
        intent?.extras?.getBoolean(LOGOUT_INFO_EXTRA, false)?.let { logoutInfo ->
            if (logoutInfo) {
                showError(getString(R.string.basic_activity_snackbar_error_user_disconnected))
                intent?.extras?.remove(LOGOUT_INFO_EXTRA)
            }
        }
    }

    @Composable
    protected abstract fun Content()

    protected fun initServer(domain: String) {
        if (ChateeSession.serverConnection != null) {
            if (ChateeSession.serverConnection!!.domain == domain) {
                return
            } else {
                ChateeSession.logout()
            }
        }
        ChateeSession.serverConnection = ServerApi(domain)
    }

    protected fun showError(
        message: String
    ) {
        snackBarHostState.currentSnackbarData?.dismiss()
        CoroutineScope(Dispatchers.Main).launch {
            snackBarHostState.showSnackbar(
                message,
                duration = SnackbarDuration.Short
            )
        }
    }

    private fun goToLoginActivityOnDisconnect() {
        if (privmxEndpointContainer?.endpoint == null && this !is LoginActivity) {
            ChateeSession.logout()
            startActivity(
                Intent(
                    this@BasicActivity,
                    LoginActivity::class.java
                ).apply {
                    putExtra(LOGOUT_INFO_EXTRA, true)
                }
            )
        }
    }

    protected fun logout(
        withMessage: Boolean = false
    ): Promise<*>? {
        if (privmxEndpointContainer?.endpoint == null) {
            goToLoginActivityOnDisconnect()
            return null
        }
        if (disconnecting) return null
        disconnecting = true
        return AsyncCall {
            privmxEndpointContainer?.endpoint?.connection?.disconnect()
            ChateeSession.logout()
        }.then(mExecutor) {
            startActivity(
                Intent(
                    this@BasicActivity,
                    LoginActivity::class.java
                ).apply {
                    if (withMessage) putExtra(LOGOUT_INFO_EXTRA, true)
                }
            )
            disconnecting = false
        }.fail {
            Log.e(TAG, "Cannot disconnnect ${it.message}")
            it.printStackTrace()
            disconnecting = false
            showError(getString(R.string.basic_activity_snackbar_error_cannot_disconnect))
        }
    }

    protected fun Modifier.contentPadding() = this
        .padding(
            horizontal = 15.dp
        )
        .padding(
            bottom = 15.dp
        )

    protected fun Modifier.contentPaddingHorizontal() = this
        .padding(
            horizontal = 15.dp
        )

    override fun onPrivmxEndpointStart() {
        super.onPrivmxEndpointStart()
        goToLoginActivityOnDisconnect()
        privmxEndpointContainer?.startListening()
        privmxEndpointContainer?.endpoint?.registerCallback(
            CONNECTION_CALLBACK_ID,
            EventType.DisconnectedEvent
        ) {
            logout(true)
        }
    }

    override fun onStop() {
        privmxEndpointContainer.endpoint?.unregisterCallbacks(this)
        super.onStop()
    }

    override fun getCertPath(): String {
        return "${filesDir}/cacert.pem".also {
            installCaCerts(it)
        }
    }

    private fun installCaCerts(certsPath: String) {
        val certsInputStream =
            BasicActivity::class.java.getResourceAsStream("/cacert.pem")
        val file = File(certsPath);
        if (file.exists()) {
            return;
        }
        try {
            file.outputStream().use {
                certsInputStream?.copyTo(it)
                certsInputStream?.close()
            }
        } catch (e: Exception) {
            e.printStackTrace();
        }
    }

}