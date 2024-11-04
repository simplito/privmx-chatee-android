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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simplito.chatee.R
import com.simplito.chatee.ui.component.ChateeAvatar
import java.text.SimpleDateFormat


@Composable
fun AuthorTag(
    author: String,
    date: Long?,
    modifier: Modifier = Modifier,
    isSending: Boolean = false,
    hasError: Boolean = false,
    messageContent: @Composable () -> Unit = {}
) {
    val dateFormatter = remember {
        SimpleDateFormat("dd-MM-yyyy HH:mm")
    }
    val dateFormatted = remember(date) {
        date?.let {
            dateFormatter.format(date)
        }
    }
    val alphaAnimated by animateFloatAsState(
        targetValue = if (isSending) 0.5f else 1f,
        animationSpec = tween(
            delayMillis = 300,
            easing = LinearEasing
        ),
        label = "Message_sending_animation"
    )
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .alpha(alphaAnimated)
    ) {
        ChateeAvatar(
            username = author,
            size = { 40f }
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Row {
                Text(
                    author,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                if (hasError) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        ImageVector.vectorResource(R.drawable.baseline_warning_amber_24),
                        "Message sending error",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                } else if (dateFormatted != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        dateFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            messageContent()
        }
    }
}

@Preview(
    showBackground = true,
)
@Composable
fun AuthorTagPreview() {
    Column {
        AuthorTag(
            author = "author",
            System.currentTimeMillis(),
            hasError = false,
            isSending = false
        ) {
            TextMessageContent(
                text = "test",
            )
        }

        AuthorTag(
            author = "author",
            System.currentTimeMillis(),
            hasError = false,
            isSending = true
        ) {
            FileMessageContent(
                fileName = "file name",
                false,
                modifier = Modifier.fillMaxWidth()
            ) {}
        }
    }
}