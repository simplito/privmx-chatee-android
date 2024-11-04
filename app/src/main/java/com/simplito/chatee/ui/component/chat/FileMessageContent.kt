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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simplito.chatee.R
import com.simplito.chatee.Utils
import com.simplito.chatee.ui.component.ChateeAvatar
import com.simplito.chatee.ui.theme.Borders
import com.simplito.chatee.ui.theme.LightGrey
import java.text.SimpleDateFormat


data class AdditionalFileMessageContentInfo(
    val date: Long?,
    val author: String?,
    val fileSize: Long?
)

@Composable
fun FileMessageContent(
    fileName: String,
    downloading: Boolean,
    modifier: Modifier = Modifier,
    additionalInfo: AdditionalFileMessageContentInfo? = null,
    onDownload: () -> Unit
) {
    val dateFormatter = remember {
        SimpleDateFormat("dd-MM-yyyy HH:mm")
    }
    val dateFormatted = remember(additionalInfo, additionalInfo?.date) {
        additionalInfo?.date?.let {
            dateFormatter.format(it)
        }
    }
    val sizeFormatted = remember(additionalInfo, additionalInfo?.fileSize) {
        additionalInfo?.fileSize?.let {
            Utils.calculateFormattedFileSize(it)
        }
    }
    val textStyle = MaterialTheme.typography.bodyMedium
    val additionalInfoTextStyle = MaterialTheme.typography.bodySmall
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .width(IntrinsicSize.Max)
            .border(
                1.dp,
                Borders,
                MaterialTheme.shapes.small
            )
            .padding(8.dp)

    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.file),
            contentDescription = "file_icon",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            Modifier.weight(1f)
        ) {
            Text(
                fileName,
                style = textStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            if (additionalInfo != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChateeAvatar(
                        username = additionalInfo.author ?: "",
                        style = additionalInfoTextStyle,
                        size = { minSize -> minSize + 4 },
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        additionalInfo.author ?: "",
                        style = additionalInfoTextStyle,
                        fontWeight = FontWeight.Normal,
                        color = LightGrey,
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .weight(1f, false)
                    )
                    if (dateFormatted != null) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            dateFormatted,
                            style = additionalInfoTextStyle,
                            fontWeight = FontWeight.Normal,
                            color = LightGrey,
                            modifier = Modifier.weight(1f, false)
                        )
                    }

                    if (sizeFormatted != null) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            sizeFormatted,
                            style = additionalInfoTextStyle,
                            fontWeight = FontWeight.Normal,
                            color = LightGrey,
                            maxLines = 1,
                            modifier = Modifier.width(IntrinsicSize.Max)
                        )
                    }
                }
            }
        }

        if (downloading) {
            CircularProgressIndicator(
                color = LightGrey,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(42.dp)
                    .padding(9.dp)

            )
        } else {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.download),
                contentDescription = "download_file",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable {
                        onDownload()
                    }
                    .size(42.dp)
                    .padding(9.dp)
            )
        }

    }
}

@Preview(
    showBackground = true
)
@Composable
fun FileMessageContentPreview() {
    Column {

        FileMessageContent(
            fileName = "file name djksadj sal kdja skjd",
            false
        ) {}

        FileMessageContent(
            fileName = "file name djksadj sal kdja skjd kdjsakld klsajkd askjkdj lsajdk lsjakd jdlks jakldkjsa ",
            true,
            additionalInfo = AdditionalFileMessageContentInfo(
                System.currentTimeMillis(),
                "me",
                123_400
            )
        ) {}

        FileMessageContent(
            fileName = "file name djksadj sal kdja skjd kdjsakld klsajkd askjkdj lsajdk lsjakd jdlks jakldkjsa ",
            false,
            additionalInfo = AdditionalFileMessageContentInfo(
                System.currentTimeMillis(),
                "SecretNonAdmin",
                123_400
            ),
            modifier = Modifier.widthIn(max = 350.dp)
        ) {}

        FileMessageContent(
            fileName = "sdkjasldja",
            false,
            additionalInfo = AdditionalFileMessageContentInfo(
                System.currentTimeMillis(),
                "me",
                123_400
            )
        ) {}
        FileMessageContent(
            fileName = "sdkjasldja",
            false,
            additionalInfo = AdditionalFileMessageContentInfo(
                System.currentTimeMillis(),
                "me",
                123_400
            ),
            modifier = Modifier.fillMaxWidth()
        ) {}
    }
}