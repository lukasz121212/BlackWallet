package pl.blackwallet

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberBitmap(path: String?): ImageBitmap? {
    val state = produceState<ImageBitmap?>(null, path) {
        value = if (path == null) null
        else withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    }
    return state.value
}

/**
 * Wizualna karta. [onImageTransform] (zoom, przesunięcie X, przesunięcie Y jako ułamki
 * rozmiaru karty) włącza przeciąganie i szczypanie grafiki bezpośrednio na karcie.
 */
@Composable
fun CardView(
    card: WalletCard,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onImageTransform: ((Float, Float, Float) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    val textColor = Color(card.text)
    val premium = card.style == CardStyle.PREMIUM
    val minimal = card.style == CardStyle.MINIMAL
    val bgImage = rememberBitmap(if (card.style == CardStyle.IMAGE) card.imagePath else null)
    val logoImage = rememberBitmap(card.logoPath)
    val transform = rememberUpdatedState(onImageTransform)

    val bgModifier = when (card.style) {
        CardStyle.GRADIENT -> Modifier.background(Brush.linearGradient(listOf(Color(card.bg), Color(card.bg2))))
        CardStyle.PREMIUM -> Modifier.background(
            Brush.linearGradient(
                listOf(Color(0xFF050505), Color(0xFF1B1B1D), Color(0xFF080808), Color(0xFF262628), Color(0xFF060606))
            )
        )
        else -> Modifier.background(Color(card.bg))
    }

    Box(
        modifier
            .aspectRatio(1.586f)
            .clip(shape)
            .then(bgModifier)
            .then(if (premium) Modifier.border(1.dp, Color(0x26FFFFFF), shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(
                if (onImageTransform != null) Modifier.pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        transform.value?.invoke(zoom, pan.x / size.width, pan.y / size.height)
                    }
                } else Modifier
            )
    ) {
        if (bgImage != null) {
            Image(
                bitmap = bgImage, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = card.scale; scaleY = card.scale
                    translationX = card.offX * size.width
                    translationY = card.offY * size.height
                },
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0x55000000), Color.Transparent, Color(0x99000000)))
                )
            )
        }

        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (!minimal) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (logoImage != null) {
                        Image(logoImage, null, Modifier.height(30.dp), contentScale = ContentScale.Fit)
                    } else {
                        Text(
                            text = if (premium) card.logoText.uppercase() else card.logoText,
                            color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = if (premium) 4.sp else 0.sp, maxLines = 1,
                        )
                    }
                    if (card.network != Network.NONE) {
                        Text(
                            card.network.label, color = textColor, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = if (card.network == Network.VISA) FontStyle.Italic else FontStyle.Normal,
                        )
                    }
                }
            } else Spacer(Modifier)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f, fill = false)) {
                    Text(
                        card.name.ifBlank { "Karta" }, color = textColor,
                        fontSize = if (minimal) 26.sp else 17.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                    )
                    if (card.bank.isNotBlank() && !minimal) {
                        Text(card.bank, color = textColor.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1)
                    }
                }
                if (card.last4.isNotBlank()) {
                    Text(
                        "•••• ${card.last4}", color = textColor, fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}
