package pro.devapp.walkietalkiek.ui

import android.text.BidiFormatter
import android.text.TextDirectionHeuristics
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import pl.droidsonroids.gif.GifImageView
import pro.devapp.walkietalkiek.R
import pro.devapp.walkietalkiek.BuildConfig

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutContent() {
    var showDeveloperDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val contactNumberRaw = stringResource(R.string.about_developer_contact_number)
    val contactNumberDisplay = remember(contactNumberRaw) {
        BidiFormatter.getInstance().unicodeWrap(
            contactNumberRaw,
            TextDirectionHeuristics.LTR
        )
    }
    val cfg = LocalConfiguration.current
    val minScreen = cfg.screenWidthDp.dp.coerceAtMost(cfg.screenHeightDp.dp)
    val scale = (minScreen / 400.dp).coerceIn(0.82f, 1.30f)
    val dims = remember(scale) { AboutDims(scale = scale) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dims.screenPadding),
        verticalArrangement = Arrangement.spacedBy(dims.sectionSpacing)
    ) {
        Card(
            shape = RoundedCornerShape(dims.heroCorner),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(dims.heroPadding)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(dims.innerSpacing)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dims.innerSpacing)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                GifImageView(ctx).apply {
                                    setImageResource(R.drawable.icon_animated)
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                }
                            },
                            modifier = Modifier
                                .size(dims.brandIconSize)
                                .clip(RoundedCornerShape(dims.brandIconCorner))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(dims.brandIconPadding)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = stringResource(
                                    R.string.about_version_format,
                                    BuildConfig.VERSION_NAME,
                                    BuildConfig.VERSION_CODE
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.about_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dims.chipSpacing),
                        verticalArrangement = Arrangement.spacedBy(dims.chipSpacing)
                    ) {
                        FeatureChip(
                            text = stringResource(R.string.about_feature_ptt_audio),
                            color = MaterialTheme.colorScheme.primary,
                            dims = dims
                        )
                        FeatureChip(
                            text = stringResource(R.string.about_feature_peer_discovery),
                            color = MaterialTheme.colorScheme.secondary,
                            dims = dims
                        )
                        FeatureChip(
                            text = stringResource(R.string.about_feature_text_chat),
                            color = MaterialTheme.colorScheme.tertiary,
                            dims = dims
                        )
                    }
                }
            }
        }

        DeveloperProfileCard(
            onImageClick = { showDeveloperDialog = true },
            dims = dims
        )

        SectionCard(
            title = stringResource(R.string.about_how_it_works_title),
            dims = dims
        ) {
            Text(
                text = stringResource(R.string.about_how_it_works_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
        }

        SectionCard(
            title = stringResource(R.string.about_support_title),
            dims = dims
        ) {
            Text(
                text = stringResource(R.string.about_support_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
        }
    }

    if (showDeveloperDialog) {
        Dialog(onDismissRequest = { showDeveloperDialog = false }) {
            Card(
                shape = RoundedCornerShape(dims.dialogCorner),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = dims.dialogElevation)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(dims.dialogPadding),
                    verticalArrangement = Arrangement.spacedBy(dims.dialogSpacing)
                ) {
                    Text(
                        text = stringResource(R.string.about_developer_popup_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Image(
                        painter = painterResource(id = R.drawable.dev_picture),
                        contentDescription = stringResource(R.string.about_developer_profile_name),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(dims.dialogImageAspectRatio)
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(dims.dialogImageCorner))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(dims.dialogImageCorner)
                            )
                    )
                    Text(
                        text = stringResource(R.string.about_developer_popup_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(dims.dialogContactCorner))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(
                                horizontal = dims.dialogContactHorizontalPadding,
                                vertical = dims.dialogContactVerticalPadding
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(dims.dialogMiniSpacing)
                        ) {
                            Text(
                                text = stringResource(R.string.about_developer_contact_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = contactNumberDisplay,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(contactNumberRaw))
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.about_developer_contact_copied),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Text(text = stringResource(R.string.about_developer_copy_button))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDeveloperDialog = false }) {
                            Text(text = stringResource(R.string.about_developer_popup_close))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    dims: AboutDims,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(dims.sectionCorner),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(dims.sectionPadding),
            verticalArrangement = Arrangement.spacedBy(dims.innerSpacing),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                content()
            }
        )
    }
}

@Composable
private fun FeatureChip(
    text: String,
    color: Color,
    dims: AboutDims
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(dims.chipCorner))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(dims.chipCorner))
            .padding(horizontal = dims.chipHorizontalPadding, vertical = dims.chipVerticalPadding)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DeveloperProfileCard(
    onImageClick: () -> Unit,
    dims: AboutDims
) {
    SectionCard(
        title = stringResource(R.string.about_developer_card_title),
        dims = dims
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(dims.developerCardCorner))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.76f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(dims.developerCardCorner)
                )
                .clickable(onClick = onImageClick)
                .padding(dims.developerCardPadding),
            horizontalArrangement = Arrangement.spacedBy(dims.developerCardSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.dev_picture),
                contentDescription = stringResource(R.string.about_developer_profile_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(dims.developerImageSize)
                    .clip(RoundedCornerShape(dims.developerImageCorner))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(dims.developerImageCorner)
                    )
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(dims.dialogMiniSpacing)
            ) {
                Text(
                    text = stringResource(R.string.about_developer_profile_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.about_developer_profile_role),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.about_developer_profile_tap_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private data class AboutDims(
    val scale: Float,
    val screenPadding: Dp = (16 * scale).dp,
    val sectionSpacing: Dp = (14 * scale).dp,
    val heroCorner: Dp = (20 * scale).dp,
    val heroPadding: Dp = (16 * scale).dp,
    val sectionCorner: Dp = (18 * scale).dp,
    val sectionPadding: Dp = (14 * scale).dp,
    val innerSpacing: Dp = (10 * scale).dp,
    val chipSpacing: Dp = (8 * scale).dp,
    val chipCorner: Dp = (12 * scale).dp,
    val chipHorizontalPadding: Dp = (10 * scale).dp,
    val chipVerticalPadding: Dp = (8 * scale).dp,
    val brandIconSize: Dp = (64 * scale).dp,
    val brandIconCorner: Dp = (14 * scale).dp,
    val brandIconPadding: Dp = (6 * scale).dp,
    val developerCardCorner: Dp = (16 * scale).dp,
    val developerCardPadding: Dp = (12 * scale).dp,
    val developerCardSpacing: Dp = (12 * scale).dp,
    val developerImageSize: Dp = (90 * scale).dp,
    val developerImageCorner: Dp = (12 * scale).dp,
    val dialogCorner: Dp = (22 * scale).dp,
    val dialogElevation: Dp = (16 * scale).dp,
    val dialogPadding: Dp = (16 * scale).dp,
    val dialogSpacing: Dp = (12 * scale).dp,
    val dialogImageCorner: Dp = (18 * scale).dp,
    val dialogImageAspectRatio: Float = 0.78f,
    val dialogContactCorner: Dp = (12 * scale).dp,
    val dialogContactHorizontalPadding: Dp = (10 * scale).dp,
    val dialogContactVerticalPadding: Dp = (8 * scale).dp,
    val dialogMiniSpacing: Dp = (3 * scale).dp
)
