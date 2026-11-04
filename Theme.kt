package com.karimbenkacem.yourtime.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween

val Emerald = Color(0xFF0B6B52); val EmeraldDeep = Color(0xFF064435)
val Navy = Color(0xFF0A1F33); val Gold = Color(0xFFD4AF37); val Cream = Color(0xFFFAF3E0)

val LocalAr = staticCompositionLocalOf { true }
@Composable fun t(ar: String, en: String) = if (LocalAr.current) ar else en

fun fmtMin(m: Int, ar: Boolean): String {
    val h = m / 60; val mm = m % 60
    return if (ar) buildString { if (h > 0) append("$h س"); if (mm > 0) { if (isNotEmpty()) append(" و "); append("$mm د") }; if (isEmpty()) append("0 د") }
    else buildString { if (h > 0) append("${h}h "); if (mm > 0 || h == 0) append("${mm}m") }.trim()
}
fun fmtMs(ms: Long, ar: Boolean) = fmtMin((ms / 60_000).toInt(), ar)

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(primary = Gold, onPrimary = Navy, secondary = Emerald, background = Navy,
        surface = Color(0xFF0F2E45), onBackground = Cream, onSurface = Cream, primaryContainer = EmeraldDeep, onPrimaryContainer = Cream)
    else lightColorScheme(primary = Emerald, onPrimary = Cream, secondary = Gold, background = Cream,
        surface = Color(0xFFFFFCF5), onBackground = Navy, onSurface = Navy, primaryContainer = Color(0xFFD9EFE5), onPrimaryContainer = EmeraldDeep)
    MaterialTheme(colorScheme = scheme, content = content)
}

// ---------- Islamic backgrounds ----------
@Composable
fun IslamicBackground(style: Int, dark: Boolean, content: @Composable BoxScope.() -> Unit) {
    val colors = when {
        dark && style == 1 -> listOf(EmeraldDeep, Navy)
        dark && style == 2 -> listOf(Navy, Color(0xFF05101B), EmeraldDeep)
        dark -> listOf(Navy, EmeraldDeep)
        style == 1 -> listOf(Color(0xFFE6F3EC), Cream)
        style == 2 -> listOf(Cream, Color(0xFFF1E4BF))
        else -> listOf(Cream, Color(0xFFDCEFE5))
    }
    val line = if (dark) Gold.copy(alpha = .10f) else Emerald.copy(alpha = .10f)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors))) {
        Canvas(Modifier.fillMaxSize()) {
            when (style) {
                1 -> arches(line)
                2 -> lattice(line, 90.dp.toPx())
                else -> starGrid(line, 110.dp.toPx())
            }
        }
        content()
    }
}

private fun DrawScope.starGrid(color: Color, cell: Float) {
    var y = 0f
    while (y < size.height + cell) {
        var x = 0f
        while (x < size.width + cell) {
            val r = cell * .32f
            for (a in listOf(0f, 45f)) rotate(a, Offset(x, y)) {
                drawRect(color, Offset(x - r, y - r), Size(2 * r, 2 * r), style = Stroke(1.5f))
            }
            drawCircle(color, r * .35f, Offset(x, y), style = Stroke(1.2f))
            x += cell
        }
        y += cell
    }
}

private fun DrawScope.lattice(color: Color, r: Float) {
    var row = 0; var y = 0f
    while (y < size.height + r) {
        var x = if (row % 2 == 0) 0f else r
        while (x < size.width + r) { drawCircle(color, r, Offset(x, y), style = Stroke(1.3f)); x += 2 * r }
        y += r * .87f; row++
    }
}

private fun DrawScope.arches(color: Color) {
    val w = 80.dp.toPx(); val h = 70.dp.toPx()
    var x = 0f
    val base = size.height
    while (x < size.width) {
        for (k in 0..1) {
            val inset = k * 10.dp.toPx()
            val p = Path().apply {
                moveTo(x + inset, base); lineTo(x + inset, base - h)
                quadraticBezierTo(x + inset, base - h - w * .45f, x + w / 2, base - h - w * .62f + inset)
                quadraticBezierTo(x + w - inset, base - h - w * .45f, x + w - inset, base - h)
                lineTo(x + w - inset, base)
            }
            drawPath(p, color, style = Stroke(1.6f))
        }
        x += w
    }
    starGrid(color.copy(alpha = color.alpha * .6f), 140.dp.toPx())
}

// ---------- Components ----------
@Composable
fun QuranIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height; val mid = w / 2
        drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = .35f), Color.Transparent), Offset(mid, h * .45f), h * .9f), h * .9f, Offset(mid, h * .45f))
        fun page(dir: Float) = Path().apply {
            moveTo(mid, h * .22f)
            cubicTo(mid + dir * w * .12f, h * .08f, mid + dir * w * .32f, h * .08f, mid + dir * w * .42f, h * .22f)
            lineTo(mid + dir * w * .42f, h * .88f)
            cubicTo(mid + dir * w * .32f, h * .76f, mid + dir * w * .12f, h * .76f, mid, h * .88f)
            close()
        }
        for (d in listOf(-1f, 1f)) {
            drawPath(page(d), Brush.verticalGradient(listOf(Cream, Color(0xFFEBDDB8))))
            drawPath(page(d), Gold, style = Stroke(3f))
            for (i in 0..5) {
                val y = h * (.33f + i * .08f)
                drawLine(Gold.copy(alpha = .55f), Offset(mid + d * w * .08f, y), Offset(mid + d * w * .36f, y), 2.5f, StrokeCap.Round)
            }
        }
        drawLine(Gold, Offset(mid, h * .22f), Offset(mid, h * .88f), 3f)
        drawCircle(Gold, 5f, Offset(mid, h * .1f))
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .85f), border = BorderStroke(1.dp, Gold.copy(alpha = .35f))) {
        Column(Modifier.padding(18.dp), Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally, content = content)
    }
}

@Composable
fun UsageRing(progress: Float, center: String, sub: String, modifier: Modifier = Modifier.size(190.dp)) {
    val anim by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900), label = "ring")
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 16.dp.toPx(); val tl = Offset(sw / 2, sw / 2); val sz = Size(size.width - sw, size.height - sw)
            drawArc(track, -90f, 360f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(Brush.sweepGradient(listOf(Emerald, Gold, Emerald)), -90f, 360f * anim, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(center, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
        }
    }
}

@Composable
fun BarChart(data: List<Pair<String, Long>>, goalMs: Long, ar: Boolean) {
    val maxV = maxOf(data.maxOfOrNull { it.second } ?: 1L, goalMs, 1L)
    Row(Modifier.fillMaxWidth().height(170.dp), Arrangement.SpaceEvenly, Alignment.Bottom) {
        data.forEachIndexed { i, (label, v) ->
            val h by animateFloatAsState(110f * v / maxV, tween(700), label = "bar")
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text(if (v > 0) fmtMs(v, ar) else "", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(22.dp).height(h.dp.coerceAtLeast(3.dp)).clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(if (i == data.lastIndex) listOf(Gold, Emerald) else listOf(Emerald, EmeraldDeep))))
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
