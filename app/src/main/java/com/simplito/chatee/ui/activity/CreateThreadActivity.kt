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

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simplito.chatee.ChateeSession
import com.simplito.chatee.R
import com.simplito.chatee.Utils.ChateeJson
import com.simplito.chatee.classes.async.AsyncCall
import com.simplito.chatee.classes.async.Promise
import com.simplito.chatee.extension.endpoint
import com.simplito.chatee.extension.mExecutor
import com.simplito.chatee.model.StorePrivateMeta
import com.simplito.chatee.model.ThreadPrivateMeta
import com.simplito.chatee.server.model.User
import com.simplito.chatee.ui.component.basic.Button
import com.simplito.chatee.ui.component.basic.TextField
import com.simplito.chatee.ui.theme.Borders
import com.simplito.chatee.ui.theme.PrivmxChateeTheme
import com.simplito.java.privmx_endpoint.model.UserWithPubKey
import kotlinx.serialization.encodeToString

class CreateThreadActivity : BasicActivity() {
    companion object {
        const val TAG = "[CreateThreadActivity]"
    }

    private val contacts = mutableStateListOf<UserElement>()

    override fun onPrivmxEndpointStart() {
        super.onPrivmxEndpointStart()
        synchronizeContacts()
    }

    @Composable
    override fun Content() {
        var threadName by remember {
            mutableStateOf("")
        }
        var threadNameError by remember {
            mutableStateOf<String?>(null)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .contentPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 15.dp)
            ) {
                Text(
                    text = stringResource(R.string.create_thread_activity_title_new_chat),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.size(15.dp))
            TextField(
                value = threadName,
                label = stringResource(R.string.create_thread_activity_input_field_label_chat_title),
                error = threadNameError,
                modifier = Modifier.fillMaxWidth()
            ) { newValue ->
                threadName = newValue
            }
            Spacer(modifier = Modifier.size(15.dp))
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.create_thread_activity_label_members),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.size(15.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                itemsIndexed(contacts) { index, item ->
                    ContactRow(
                        contact = item,
                        isLocked = item.user.publicKey == ChateeSession.currentUser?.publicKey,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        item.isSelected.value = !item.isSelected.value
                    }
                }
            }
            Spacer(modifier = Modifier.size(25.dp))
            Button(
                onClick = {
                    if (threadName.isNotBlank()) {
                        threadNameError = null
                        createThreadWithStore(threadName.trim())
                    } else {
                        threadNameError =
                            getString(R.string.create_thread_activity_text_field_error_chat_name_cannot_be_empty)
                        null
                    }
                },
                isAsync = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.create_thread_activity_button_label_save))
            }
        }
    }

    private fun createThreadWithStore(threadName: String): Promise<*>? {
        val privmxEndpoint = privmxEndpointContainer?.endpoint
        if (privmxEndpoint == null) {
            showError(getString(R.string.create_thread_activity_snackbar_error_cannot_create_thread_no_connection))
            return null
        }

        val contextId = ChateeSession.serverConnection?.cloudData?.contextId
        if (contextId == null) {
            showError(getString(R.string.create_thread_activity_snackbar_error_cannot_create_thread_user_not_logged_id))
            return null
        }

        if (privmxEndpoint.threadApi == null || privmxEndpoint.storeApi == null) {
            showError(getString(R.string.create_thread_activity_snackbar_error_required_modules_is_not_initialized))
            return null
        }

        return AsyncCall {
            val userArr = contacts
                .filter {
                    it.isSelected.value
                }
                .map {
                    UserWithPubKey(
                        it.user.username,
                        it.user.publicKey
                    )
                }
            val adminArr = contacts
                .filter {
                    it.isSelected.value && (it.user.isStaff || it.user.publicKey == ChateeSession.currentUser?.publicKey)
                }
                .map {
                    UserWithPubKey(
                        it.user.username,
                        it.user.publicKey
                    )
                }

            //create store
            val storeId = privmxEndpoint.storeApi.createStore(
                contextId,
                userArr,
                adminArr,
                ByteArray(0),
                ChateeJson.encodeToString(StorePrivateMeta(threadName)).toByteArray()
            )
//
//            //create thread
            privmxEndpoint.threadApi.createThread(
                contextId,
                userArr,
                adminArr,
                ByteArray(0),
                ChateeJson.encodeToString(
                    ThreadPrivateMeta(
                        threadName,
                        storeId
                    )
                ).toByteArray()
            )
        }.then(mExecutor) {
            finish()
        }.fail(mExecutor) {
            Log.e(TAG, "Cannot create thread [reason]: ${it.message} $it")
            it.printStackTrace()
            showError(getString(R.string.create_thread_activity_snackbar_error_cannot_create_thread))
        }
    }

    private fun synchronizeContacts() {
        ChateeSession.serverConnection?.let { serverConnection ->
            AsyncCall {
                val result = serverConnection.contacts()
                result.error?.let {
                    throw Exception("Cannot get contacts from server: ${it.message}")
                }
                result.result?.let {
                    return@AsyncCall it.contacts
                }
                throw Exception("Unknown excepiton")
            }.then(mExecutor) {
                contacts.clear()
                contacts.addAll(
                    it.map { user ->
                        UserElement(user, mutableStateOf(false))
                    }
                )
                if (ChateeSession.currentUser != null) {
                    if (contacts.firstOrNull { it.user.publicKey == ChateeSession.currentUser?.publicKey } == null) {
                        contacts.add(
                            UserElement(
                                ChateeSession.currentUser!!,
                                mutableStateOf(false)
                            ).apply { })
                    }
                }
                makeMeSelected()
            }.fail(mExecutor) {
                Log.e(TAG, "Cannot synchronize contacts [reason]: ${it.message}")
                showError(getString(R.string.create_thread_activity_snackbar_error_cannot_get_users_from_server))
            }
        }
    }

    private fun makeMeSelected() {
        contacts.firstOrNull {
            ChateeSession.currentUser?.publicKey != null
                    && it.user.publicKey == ChateeSession.currentUser?.publicKey
        }?.apply {
            isSelected.value = true
        }
    }

    @Preview(
        showBackground = true,
        showSystemUi = true
    )
    @Composable
    private fun ContentPreview() {
        Content()
    }
}

@Composable
private fun ContactRow(
    contact: UserElement,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    onClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .heightIn(65.dp)
            .then(modifier)
            .clip(MaterialTheme.shapes.small)
            .clickable {
                if (!isLocked) {
                    onClick()
                }
            }
            .border(
                1.dp,
                Borders,
                MaterialTheme.shapes.small
            )
            .background(
                if (contact.isSelected.value) Borders.copy(alpha = 0.2f)/*MaterialTheme.colorScheme.primary*/ else MaterialTheme.colorScheme.surface
            )
            .padding(7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = contact.user.username,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (contact.user.isStaff) {
                Spacer(modifier = Modifier.widthIn(10.dp))
                Text(
                    text = "STAFF",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.shapes.medium
                        )
                        .padding(
                            vertical = 3.dp,
                            horizontal = 8.dp
                        )
                )
            }
        }
        Row {
            Spacer(modifier = Modifier.widthIn(20.dp))
            Switch(
                checked = contact.isSelected.value,
                onCheckedChange = {
                    contact.isSelected.value = it
                },
                enabled = !isLocked
            )
        }
    }
}

data class UserElement(
    val user: User,
    var isSelected: MutableState<Boolean>,
)

@Preview(
    showBackground = true
)
@Composable
private fun ContactRowPreview() {
    val user = remember {
        UserElement(User("Adam", "avc", false), mutableStateOf(true))
    }
    PrivmxChateeTheme {
        ContactRow(contact = user)
    }
}