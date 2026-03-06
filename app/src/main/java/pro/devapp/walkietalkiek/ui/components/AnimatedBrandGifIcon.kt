package pro.devapp.walkietalkiek.ui.components

import android.widget.ImageView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import pl.droidsonroids.gif.GifImageView
import pro.devapp.walkietalkiek.R

@Composable
internal fun AnimatedBrandGifIcon(
    size: Dp,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            GifImageView(ctx).apply {
                setImageResource(R.drawable.icon_animated)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        },
        modifier = modifier
            .size(size)
    )
}
