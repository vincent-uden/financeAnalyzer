package bank

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.unit.dp
import colors
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.theme.Theme
import green
import red
import text

enum class AppButtonVariant {
    CONFIRM,
    DANGER
}

@Composable
fun AppButton (
    content: String,
    onClick: () -> Unit,
    type: AppButtonVariant = AppButtonVariant.CONFIRM
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val baseBgColor = when (type) {
        AppButtonVariant.CONFIRM -> Theme[colors][green]
        AppButtonVariant.DANGER -> Theme[colors][red]
    }
    val bgColor = if (isHovered) colorLerp(baseBgColor, Color.White, 0.3f) else baseBgColor
    UnstyledButton(
        modifier = Modifier.background(bgColor, RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
        interactionSource = interactionSource,
        onClick = onClick
    ) {
        Text(content, color = Theme[colors][text])
    }
}