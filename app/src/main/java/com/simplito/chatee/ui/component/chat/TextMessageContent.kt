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


package com.simplito.chatee.ui.component.chat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview


@Composable
fun TextMessageContent(
    text: String,
    modifier: Modifier = Modifier,
    isMy: Boolean = true,
) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Normal,
        modifier = modifier
    )
}

@Preview(
    showBackground = true,
)
@Composable
fun TextMessageContentPreview() {
    TextMessageContent(
        text = "text"
    )
}