package com.example.waterbottletracker.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.glance.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.waterbottletracker.data.WaterRepository
import com.example.waterbottletracker.data.WaterState
import com.example.waterbottletracker.util.formatVolume
import kotlinx.coroutines.flow.first
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color as ComposeColor

class WaterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WaterWidget()
}

class WaterWidget : GlanceAppWidget() {
    // Exact size mode allows us to use LocalSize.current for precise layout calculations
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = WaterRepository(context.applicationContext)

        provideContent {
            val state by repo.waterStateFlow.collectAsState(initial = WaterState())
            WidgetLayout(state)
        }
    }
}

// ---- Palette (matches the dark prototype) ----
private val BgColor = ComposeColor(0xFF111318)
private val RingBlue = ComposeColor(0xFF2F8CFF)
private val DividerColor = ComposeColor(0xFF2A2D34)
private val MinusButtonBg = ComposeColor(0xFF1E2026)
private val White = ComposeColor(0xFFFFFFFF)
private val GreyText = ComposeColor(0xFF9AA0A8)

@Composable
fun WidgetLayout(state: WaterState) {
    val size = LocalSize.current
    val isWide = size.width > size.height * 1.8f

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BgColor)
            .cornerRadius(32.dp)
    ) {
        if (isWide) {
            WideContent(state)
        } else {
            SquareContent(state)
        }
    }
}

@Composable
private fun SquareContent(state: WaterState) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp), // Reduced from 16.dp
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentAlignment = Alignment.Center
        ) {
            // Replaced hardcoded ringSizeDp with a flexible modifier
            ProgressRing(state, modifier = GlanceModifier.fillMaxSize())
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth().height(48.dp), // Reduced from 56.dp
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AddButton(state, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
            Spacer(modifier = GlanceModifier.width(8.dp)) // Reduced from 12.dp
            MinusButton(modifier = GlanceModifier.width(48.dp).fillMaxHeight()) // Reduced from 56.dp
        }
    }
}

@Composable
private fun WideContent(state: WaterState) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(16.dp), // Wide can keep larger padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier.fillMaxHeight().width(150.dp).padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Update to use the new modifier signature
            ProgressRing(state, modifier = GlanceModifier.fillMaxSize())
        }

        Spacer(modifier = GlanceModifier.width(1.dp).fillMaxHeight().background(DividerColor))
        Spacer(modifier = GlanceModifier.width(16.dp))

        AddButton(
            state,
            modifier = GlanceModifier.fillMaxHeight().defaultWeight()
        )

        Spacer(modifier = GlanceModifier.width(12.dp))

        MinusButton(modifier = GlanceModifier.fillMaxHeight().width(48.dp)) // Adjusted to match square
    }
}

@Composable
private fun ProgressRing(state: WaterState, modifier: GlanceModifier) {
    // 1. Updated to use state.dailyGoalMl and state.mlDrank
    val progress = if (state.dailyGoalMl > 0) {
        (state.mlDrank.toFloat() / state.dailyGoalMl).coerceIn(0f, 1f)
    } else 0f

    val ringBitmap = remember(progress) {
        createRingBitmap(
            sizePx = 250,
            strokePx = 24f,
            progress = progress,
            trackColorInt = AndroidColor.parseColor("#2A2D34"),
            progressColorInt = AndroidColor.parseColor("#2F8CFF")
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(ringBitmap),
            contentDescription = null,
            modifier = GlanceModifier.fillMaxSize()
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "💧",
                style = TextStyle(fontSize = 18.sp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                // 2. Updated to display state.mlDrank directly
                text = formatVolume(state.mlDrank),
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(White),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                // 3. Updated to display state.dailyGoalMl directly
                text = "of ${formatVolume(state.dailyGoalMl)}",
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(GreyText),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun AddButton(state: WaterState, modifier: GlanceModifier) {
    Box(
        modifier = modifier
            .background(RingBlue)
            .cornerRadius(24.dp)
            .clickable(actionRunCallback<IncrementWaterAction>()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            // 4. Updated to use state.lastAmountMl instead of bottleSizeMl
            text = "+${formatVolume(state.lastAmountMl)}",
            maxLines = 1,
            style = TextStyle(
                color = ColorProvider(White),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        )
    }
}

@Composable
private fun MinusButton(modifier: GlanceModifier) {
    Box(
        modifier = modifier
            .background(MinusButtonBg)
            .cornerRadius(24.dp) // Creates a perfect circle for a 48x48 layout
            .clickable(actionRunCallback<DecrementWaterAction>()),
        contentAlignment = Alignment.Center
    ) {
        Spacer(modifier = GlanceModifier.width(16.dp).height(4.dp).background(GreyText))
    }
}

private fun createRingBitmap(
    sizePx: Int,
    strokePx: Float,
    progress: Float,
    trackColorInt: Int,
    progressColorInt: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
    }

    // Added + 1f buffer. Without this, the anti-aliased edges of the ROUND cap
    // can slightly exceed the canvas bounds and look clipped.
    val inset = (strokePx / 2) + 1f
    val rect = RectF(inset, inset, sizePx - inset, sizePx - inset)

    paint.color = trackColorInt
    canvas.drawArc(rect, 0f, 360f, false, paint)

    if (progress > 0f) {
        paint.color = progressColorInt
        canvas.drawArc(rect, -90f, 360f * progress, false, paint)
    }

    return bitmap
}
class IncrementWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repo = WaterRepository(context.applicationContext)
        val state = repo.waterStateFlow.first()
        repo.logAmount(state.lastAmountMl)
        WaterWidget().updateAll(context)
    }
}

class DecrementWaterAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repo = WaterRepository(context.applicationContext)
        val state = repo.waterStateFlow.first()
        repo.undoLastAmount(state.lastAmountMl)
        WaterWidget().updateAll(context)
    }
}