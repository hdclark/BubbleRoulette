package com.hdclark.bubbleroulette

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private val Night = Color(0xFF160C20)
private val Plum = Color(0xFF32143E)
private val Velvet = Color(0xFF632447)
private val Gold = Color(0xFFFFD36A)
private val Champagne = Color(0xFFFFE19A)
private val SoapBlue = Color(0xFF7DE3F4)
private val Cream = Color(0xFFFFF5DF)
private val Ink = Color(0xFF2A1832)
private val BubblePink = Color(0xFFFF91C8)

private enum class GamePhase {
    REVEAL,
    SHUFFLING,
    OCCLUDED,
    CHOOSE,
    SUCCESS,
    GAME_OVER,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BubbleRouletteTheme {
                BubbleRouletteGame()
            }
        }
    }
}

@Composable
private fun BubbleRouletteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            onPrimary = Ink,
            secondary = BubblePink,
            onSecondary = Ink,
            background = Night,
            onBackground = Cream,
            surface = Plum,
            onSurface = Cream,
            error = Color(0xFFFF6F7F),
        ),
        content = content,
    )
}

@Composable
private fun BubbleRouletteGame() {
    val roundFactory = remember { RoundFactory() }
    var level by rememberSaveable { mutableIntStateOf(1) }
    var bestLevel by rememberSaveable { mutableIntStateOf(1) }
    var phase by remember { mutableStateOf(GamePhase.REVEAL) }
    var round by remember { mutableStateOf(roundFactory.create(level)) }

    LaunchedEffect(phase, round.level) {
        when (phase) {
            GamePhase.REVEAL -> {
                delay(2_250)
                phase = when (round.transition) {
                    TransitionStyle.VISIBLE_SHUFFLE -> GamePhase.SHUFFLING
                    TransitionStyle.NADINE_OCCLUSION -> GamePhase.OCCLUDED
                }
            }

            GamePhase.SHUFFLING -> {
                repeat(4) {
                    delay(340)
                    round = roundFactory.shuffled(round)
                }
                delay(500)
                phase = GamePhase.CHOOSE
            }

            GamePhase.OCCLUDED -> {
                delay(350)
                repeat(3) {
                    round = roundFactory.shuffled(round)
                    delay(250)
                }
                delay(850)
                phase = GamePhase.CHOOSE
            }

            GamePhase.SUCCESS -> {
                delay(1_150)
                val nextLevel = GameRules.nextLevel(level)
                level = nextLevel
                bestLevel = max(bestLevel, nextLevel)
                round = roundFactory.create(nextLevel)
                phase = GamePhase.REVEAL
            }

            GamePhase.CHOOSE,
            GamePhase.GAME_OVER,
            -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Night, Plum, Velvet),
                ),
            ),
    ) {
        AmbientBubbles()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(level = level, bestLevel = bestLevel)
            Spacer(Modifier.height(12.dp))
            Prompt(phase = phase, transition = round.transition)
            Spacer(Modifier.height(12.dp))

            TableBoard(
                modifier = Modifier.weight(1f),
                round = round,
                phase = phase,
                onGlassSelected = { glass ->
                    if (phase != GamePhase.CHOOSE) return@TableBoard
                    phase = when (GameRules.guess(round, glass.id)) {
                        GuessResult.CHAMPAGNE -> GamePhase.SUCCESS
                        GuessResult.BUBBLE_MIXTURE -> GamePhase.GAME_OVER
                    }
                },
            )

            Spacer(Modifier.height(10.dp))
            Text(
                text = "One real glass. $level suspicious ${if (level == 1) "decoy" else "decoys"}.",
                color = Cream.copy(alpha = 0.78f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.navigationBarsPadding(),
            )
        }

        AnimatedVisibility(
            visible = phase == GamePhase.OCCLUDED,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.75f),
            exit = fadeOut(tween(220)) + scaleOut(targetScale = 1.08f),
        ) {
            NadineOcclusion()
        }

        AnimatedVisibility(
            visible = phase == GamePhase.SUCCESS,
            enter = fadeIn() + scaleIn(initialScale = 0.6f),
            exit = fadeOut() + scaleOut(targetScale = 1.2f),
        ) {
            SuccessOverlay(level = level)
        }

        AnimatedVisibility(
            visible = phase == GamePhase.GAME_OVER,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(150)),
        ) {
            GameOverOverlay(
                level = level,
                onRestart = {
                    level = 1
                    round = roundFactory.create(level = 1)
                    phase = GamePhase.REVEAL
                },
            )
        }
    }
}

@Composable
private fun Header(level: Int, bestLevel: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "BUBBLE",
                color = Gold,
                fontWeight = FontWeight.Black,
                fontSize = 27.sp,
                letterSpacing = 2.sp,
            )
            Text(
                text = "ROULETTE",
                color = Cream,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 4.sp,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScorePill(label = "LEVEL", value = level)
            ScorePill(label = "BEST", value = bestLevel)
        }
    }
}

@Composable
private fun ScorePill(label: String, value: Int) {
    Surface(
        color = Color.Black.copy(alpha = 0.24f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(
            width = 1.dp,
            color = Gold.copy(alpha = 0.32f),
            shape = RoundedCornerShape(16.dp),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, color = Cream.copy(alpha = 0.62f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value.toString(), color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun Prompt(phase: GamePhase, transition: TransitionStyle) {
    val title: String
    val subtitle: String

    when (phase) {
        GamePhase.REVEAL -> {
            title = "Memorize the champagne!"
            subtitle = "Soap has a blue label. Champagne has the gold crown."
        }

        GamePhase.SHUFFLING -> {
            title = "Eyes on the glasses!"
            subtitle = "They are shuffling in plain sight."
        }

        GamePhase.OCCLUDED -> {
            title = "Nadine, move!"
            subtitle = "The glasses are moving behind her."
        }

        GamePhase.CHOOSE -> {
            title = "Choose wisely"
            subtitle = when (transition) {
                TransitionStyle.VISIBLE_SHUFFLE -> "Which glass did you track?"
                TransitionStyle.NADINE_OCCLUSION -> "Which glass survived Nadine's entrance?"
            }
        }

        GamePhase.SUCCESS -> {
            title = "A refined palate!"
            subtitle = "Preparing one more decoy…"
        }

        GamePhase.GAME_OVER -> {
            title = "That was extremely soapy"
            subtitle = "Nadine has activated the emergency bubbles."
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = Cream,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            color = Cream.copy(alpha = 0.72f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TableBoard(
    modifier: Modifier,
    round: RoundSpec,
    phase: GamePhase,
    onGlassSelected: (DrinkGlass) -> Unit,
) {
    val reveal = phase == GamePhase.REVEAL
    val enabled = phase == GamePhase.CHOOSE

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF351D38).copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF442548), Color(0xFF241329)),
                    ),
                )
                .drawBehind {
                    val tableTop = size.height * 0.84f
                    drawRect(
                        color = Color(0xFF7C3F39),
                        topLeft = Offset(0f, tableTop),
                        size = Size(size.width, size.height - tableTop),
                    )
                    drawLine(
                        color = Gold.copy(alpha = 0.28f),
                        start = Offset(0f, tableTop),
                        end = Offset(size.width, tableTop),
                        strokeWidth = 4f,
                    )
                }
                .padding(14.dp),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (round.glasses.size > 12) 64.dp else 82.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            ) {
                items(
                    items = round.glasses,
                    key = { it.id },
                ) { glass ->
                    GlassCard(
                        glass = glass,
                        reveal = reveal,
                        enabled = enabled,
                        modifier = Modifier,
                        onClick = { onGlassSelected(glass) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassCard(
    glass: DrinkGlass,
    reveal: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val description = when {
        reveal && glass.kind == DrinkKind.CHAMPAGNE -> "Legitimate champagne glass"
        reveal -> "Decoy bubble mixture glass"
        else -> "Mystery glass"
    }

    Card(
        modifier = modifier
            .aspectRatio(0.72f)
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFFF7EBD8) else Color(0xFFE9DCCB),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 9.dp else 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = when {
                    !reveal -> "?"
                    glass.kind == DrinkKind.CHAMPAGNE -> "♛"
                    else -> "SOAP"
                },
                color = when {
                    !reveal -> Ink
                    glass.kind == DrinkKind.CHAMPAGNE -> Color(0xFFA86C00)
                    else -> Color(0xFF087D91)
                },
                fontSize = if (reveal && glass.kind == DrinkKind.BUBBLE_MIXTURE) 10.sp else 18.sp,
                fontWeight = FontWeight.Black,
            )

            ChampagneFlute(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                liquid = if (reveal && glass.kind == DrinkKind.BUBBLE_MIXTURE) SoapBlue else Champagne,
                disguise = !reveal,
            )

            Text(
                text = when {
                    !reveal -> "MYSTERY"
                    glass.kind == DrinkKind.CHAMPAGNE -> "REAL"
                    else -> "DECOY"
                },
                color = Ink.copy(alpha = 0.78f),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
            )
        }
    }
}

@Composable
private fun ChampagneFlute(
    modifier: Modifier = Modifier,
    liquid: Color,
    disguise: Boolean,
) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val bowlTop = size.height * 0.08f
        val bowlBottom = size.height * 0.60f
        val bowlHalfWidth = size.width * 0.28f
        val stemBottom = size.height * 0.87f

        val bowl = Path().apply {
            moveTo(centerX - bowlHalfWidth, bowlTop)
            quadraticBezierTo(centerX - bowlHalfWidth * 0.82f, bowlBottom, centerX, bowlBottom)
            quadraticBezierTo(centerX + bowlHalfWidth * 0.82f, bowlBottom, centerX + bowlHalfWidth, bowlTop)
        }

        drawPath(
            path = bowl,
            color = Color(0xFF6F6474),
            style = Stroke(width = max(2.5f, size.width * 0.035f), cap = StrokeCap.Round),
        )

        val liquidTop = size.height * 0.28f
        val liquidPath = Path().apply {
            moveTo(centerX - bowlHalfWidth * 0.75f, liquidTop)
            quadraticBezierTo(centerX - bowlHalfWidth * 0.55f, bowlBottom * 0.94f, centerX, bowlBottom * 0.96f)
            quadraticBezierTo(centerX + bowlHalfWidth * 0.55f, bowlBottom * 0.94f, centerX + bowlHalfWidth * 0.75f, liquidTop)
            close()
        }
        drawPath(
            path = liquidPath,
            color = if (disguise) Champagne else liquid,
        )

        drawLine(
            color = Color(0xFF6F6474),
            start = Offset(centerX, bowlBottom),
            end = Offset(centerX, stemBottom),
            strokeWidth = max(2.5f, size.width * 0.035f),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0xFF6F6474),
            start = Offset(centerX - bowlHalfWidth * 0.55f, stemBottom),
            end = Offset(centerX + bowlHalfWidth * 0.55f, stemBottom),
            strokeWidth = max(2.5f, size.width * 0.035f),
            cap = StrokeCap.Round,
        )

        val bubbleColor = Color.White.copy(alpha = 0.72f)
        val bubbles = listOf(
            Offset(centerX - bowlHalfWidth * 0.30f, size.height * 0.43f),
            Offset(centerX + bowlHalfWidth * 0.23f, size.height * 0.35f),
            Offset(centerX, size.height * 0.50f),
        )
        bubbles.forEachIndexed { index, center ->
            drawCircle(
                color = bubbleColor,
                radius = size.minDimension * (0.025f + index * 0.006f),
                center = center,
                style = Stroke(width = max(1.4f, size.width * 0.015f)),
            )
        }
    }
}

@Composable
private fun NadineOcclusion() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF100817).copy(alpha = 0.92f))
            .semantics {
                contentDescription = "Nadine is blocking the glasses while they shuffle"
            },
        contentAlignment = Alignment.Center,
    ) {
        AmbientBubbles(intense = true)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = Cream,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 12.dp,
            ) {
                Text(
                    text = "Pardon me! Very important table business!",
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            NadineCharacter(modifier = Modifier.size(270.dp))
        }
    }
}

@Composable
private fun NadineCharacter(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val faceCenter = Offset(cx, size.height * 0.35f)
            val faceRadius = size.minDimension * 0.20f

            drawCircle(
                color = Color(0xFF4B263B),
                radius = faceRadius * 1.34f,
                center = faceCenter.copy(y = faceCenter.y - faceRadius * 0.08f),
            )
            drawCircle(
                color = Color(0xFFF2B58C),
                radius = faceRadius,
                center = faceCenter,
            )
            drawArc(
                color = Color(0xFF4B263B),
                startAngle = 185f,
                sweepAngle = 170f,
                useCenter = true,
                topLeft = Offset(cx - faceRadius * 1.18f, faceCenter.y - faceRadius * 1.2f),
                size = Size(faceRadius * 2.36f, faceRadius * 1.78f),
            )

            val eyeY = faceCenter.y - faceRadius * 0.08f
            drawCircle(Color(0xFF2B1A25), faceRadius * 0.09f, Offset(cx - faceRadius * 0.36f, eyeY))
            drawCircle(Color(0xFF2B1A25), faceRadius * 0.09f, Offset(cx + faceRadius * 0.36f, eyeY))
            drawArc(
                color = Color(0xFFA23D52),
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(cx - faceRadius * 0.37f, faceCenter.y + faceRadius * 0.12f),
                size = Size(faceRadius * 0.74f, faceRadius * 0.42f),
                style = Stroke(width = faceRadius * 0.09f, cap = StrokeCap.Round),
            )

            val shoulderY = size.height * 0.55f
            val body = Path().apply {
                moveTo(cx - size.width * 0.28f, size.height)
                quadraticBezierTo(cx - size.width * 0.24f, shoulderY, cx, shoulderY)
                quadraticBezierTo(cx + size.width * 0.24f, shoulderY, cx + size.width * 0.28f, size.height)
                close()
            }
            drawPath(body, Color(0xFFFF6E8F))
            drawCircle(
                color = Color(0xFFF2B58C),
                radius = size.minDimension * 0.08f,
                center = Offset(size.width * 0.18f, size.height * 0.64f),
            )
            drawCircle(
                color = Color(0xFFF2B58C),
                radius = size.minDimension * 0.08f,
                center = Offset(size.width * 0.82f, size.height * 0.64f),
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 42.dp)
                .shadow(8.dp, RoundedCornerShape(8.dp)),
            color = Cream,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFC62956)),
        ) {
            Text(
                text = "Nadine",
                color = Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun SuccessOverlay(level: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E6B55).copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center,
    ) {
        BubbleStorm(subtle = true)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🥂", fontSize = 74.sp)
            Text(
                text = "CORRECT!",
                color = Cream,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Text(
                text = "Level ${level + 1} adds another suspicious glass.",
                color = Cream.copy(alpha = 0.86f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun GameOverOverlay(
    level: Int,
    onRestart: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF7A1B5E), Color(0xFF160C20)),
                ),
            )
            .semantics {
                contentDescription = "Game over. Bubble mixture selected."
            },
        contentAlignment = Alignment.Center,
    ) {
        BubbleStorm()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NadineCharacter(modifier = Modifier.size(220.dp))
            Text(
                text = "SOAP CATASTROPHE!",
                color = Gold,
                fontSize = 31.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "BLORP! Nadine has deployed every bubble in the building.",
                color = Cream,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = "You reached level $level.",
                color = Cream.copy(alpha = 0.78f),
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            )
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("TRY AGAIN, NADINE", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }
}

private data class BubbleSeed(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val phase: Float,
    val colorIndex: Int,
)

@Composable
private fun BubbleStorm(subtle: Boolean = false) {
    val seeds = remember {
        val random = Random(88421)
        List(if (subtle) 38 else 84) {
            BubbleSeed(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = random.nextFloat() * 22f + 8f,
                speed = random.nextFloat() * 0.85f + 0.65f,
                phase = random.nextFloat() * (2f * PI.toFloat()),
                colorIndex = random.nextInt(4),
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "bubble storm")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_700, easing = LinearEasing),
        ),
        label = "bubble rise",
    )
    val colors = listOf(SoapBlue, BubblePink, Champagne, Color.White)

    Canvas(modifier = Modifier.fillMaxSize()) {
        seeds.forEach { seed ->
            val travel = size.height + seed.radius * 6f
            val rawY = seed.y * size.height - progress * travel * seed.speed
            val y = ((rawY % travel) + travel) % travel - seed.radius * 2f
            val xWobble = sin(progress * 2f * PI.toFloat() + seed.phase) * seed.radius * 1.6f
            val x = seed.x * size.width + xWobble
            val radius = seed.radius * density
            val alpha = if (subtle) 0.30f else 0.68f

            drawCircle(
                color = colors[seed.colorIndex].copy(alpha = alpha),
                radius = radius,
                center = Offset(x, y),
                style = Stroke(width = max(2f, radius * 0.13f)),
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.70f),
                radius = radius * 0.14f,
                center = Offset(x - radius * 0.33f, y - radius * 0.33f),
            )
        }
    }
}

@Composable
private fun AmbientBubbles(intense: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "ambient bubbles")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (intense) 3_300 else 7_000, easing = LinearEasing),
        ),
        label = "ambient drift",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val count = if (intense) 26 else 13
        repeat(count) { index ->
            val radius = (8f + (index % 5) * 5f) * density
            val xBase = ((index * 83) % 100) / 100f * size.width
            val x = xBase + cos(drift * 2f * PI.toFloat() + index) * radius
            val rawY = ((index * 47) % 100) / 100f * size.height - drift * (size.height + radius * 4f)
            val travel = size.height + radius * 4f
            val y = ((rawY % travel) + travel) % travel
            drawCircle(
                color = Color.White.copy(alpha = if (intense) 0.18f else 0.08f),
                radius = radius,
                center = Offset(x, y),
                style = Stroke(width = max(1.5f, radius * 0.09f)),
            )
        }
    }
}
