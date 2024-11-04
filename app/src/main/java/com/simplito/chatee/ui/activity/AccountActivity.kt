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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simplito.chatee.ChateeSession
import com.simplito.chatee.R
import com.simplito.chatee.ui.component.ChateeAvatar
import com.simplito.chatee.ui.component.basic.Button

class AccountActivity : BasicActivity() {
    companion object {
        const val TAG = "[AccountActivity]"
    }

    @Composable
    override fun Content() {
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
                    text = stringResource(R.string.account_activity_title_account),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (ChateeSession.currentUser != null) {
                    ChateeAvatar(
                        username = ChateeSession.currentUser?.username ?: "Adam",
                        style = MaterialTheme.typography.displaySmall,
                        size = { minSize -> minSize + 20 }
                    )
                    Spacer(Modifier.size(5.dp))
                    Text(
                        ChateeSession.currentUser?.username ?: "Adam",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (ChateeSession.currentUser?.isStaff == true) {
                        Spacer(Modifier.size(5.dp))
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
                    Spacer(Modifier.size(25.dp))
                }
                Button(
                    onClick = {
                        logout()
                    },
                    isAsync = true,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = LocalContext.current.getString(
                            R.string.account_activity_button_logout
                        ),
                    )
                }
            }
            Text(
                text = "v${
                    this@AccountActivity.packageManager.getPackageInfo(
                        this@AccountActivity.packageName,
                        0
                    ).versionName
                }",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
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