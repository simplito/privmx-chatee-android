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


package com.simplito.chatee.config

import androidx.compose.ui.graphics.Color

val AvatarForegroundColors = listOf(
    Color(0xFFfa5252),
    Color(0xFFe64980),
    Color(0xFFbe4bdb),
    Color(0xFF7950f2),
    Color(0xFF4c6ef5),
    Color(0xFF228be6),
    Color(0xFF15aabf),
    Color(0xFF40c057),
    Color(0xFF82c91e),
    Color(0xFFfab005),
    Color(0xFFfd7e14)
)
private const val foregroundConstAlpha = (0.1 * 255).toInt()
val AvatarBackgroundColors = listOf(

    Color(250, 82, 82, foregroundConstAlpha),
    Color(230, 73, 128, foregroundConstAlpha),
    Color(190, 75, 219, foregroundConstAlpha),
    Color(121, 80, 242, foregroundConstAlpha),
    Color(76, 110, 245, foregroundConstAlpha),
    Color(34, 139, 230, foregroundConstAlpha),
    Color(21, 170, 191, foregroundConstAlpha),
    Color(64, 192, 87, foregroundConstAlpha),
    Color(130, 201, 30, foregroundConstAlpha),
    Color(250, 176, 5, foregroundConstAlpha),
    Color(253, 126, 20, foregroundConstAlpha),
)
