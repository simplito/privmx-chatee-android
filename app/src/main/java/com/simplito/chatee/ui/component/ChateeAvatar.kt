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


package com.simplito.chatee.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simplito.chatee.config.AvatarBackgroundColors
import com.simplito.chatee.config.AvatarForegroundColors

@Composable
fun ChateeAvatar(
    username: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    size: (minSize: Float) -> Float = { it },
    backgroundColors: List<Color> = AvatarBackgroundColors,
    foregroundColors: List<Color> = AvatarForegroundColors
) {
    val bgColor = getColorFromColorsForName(backgroundColors, username)
    val fgColor = getColorFromColorsForName(foregroundColors, username)
    val density = LocalDensity.current
    val minSize = remember(density, style) {
        with(density) {
            ((style.lineHeight).toPx()).toDp().value
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .sizeIn(minSize.dp, minSize.dp)
            .size(size(minSize).dp)
            .then(modifier)
    ) {
        Text(
            text = if (username.isEmpty()) "" else username.first().uppercase(),
            style = style,
            fontWeight = FontWeight.Bold,
            color = fgColor
        )
    }
}

private fun getColorFromColorsForName(
    colors: List<Color>,
    username: String?
): Color {
    if (username == null) return colors[0]

    var hash = 0
    username.chars().forEach {
        hash = (hash shl 5) - hash + it
        hash = hash and hash
    }
    return colors[Math.abs(hash) % colors.size]
}

@Composable
@Preview
fun ChateeAvatarPreview() {
    ChateeAvatar(username = "A")
}