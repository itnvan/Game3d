package com.cozyhome.app.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cozyhome.app.R
import com.cozyhome.app.render.buildSkyEnvironment
import com.cozyhome.app.render.rememberFill
import com.cozyhome.app.render.rememberMeshInstance
import com.cozyhome.app.render.rememberSun
import com.cozyhome.app.ui.components.ChunkyButton
import com.cozyhome.app.ui.components.bouncyClick
import com.cozyhome.app.ui.play.scene.CharacterRig
import com.cozyhome.app.ui.play.scene.Part
import com.cozyhome.app.ui.play.scene.RigRefs
import com.cozyhome.app.ui.theme.Coral
import com.cozyhome.app.ui.theme.Cream
import com.cozyhome.app.ui.theme.Ink
import com.cozyhome.app.ui.theme.InkSoft
import com.cozyhome.app.ui.theme.Lilac
import com.cozyhome.app.ui.theme.Mint
import com.cozyhome.app.ui.theme.Shadow
import com.cozyhome.app.ui.theme.SkyBottom
import com.cozyhome.app.ui.theme.SkyTop
import com.cozyhome.app.ui.theme.Sun
import com.cozyhome.app.ui.theme.White
import com.cozyhome.game.anim.Anim
import com.cozyhome.game.anim.Animator
import com.cozyhome.game.math.V3
import com.cozyhome.game.mesh.Mat
import com.cozyhome.game.mesh.mesh
import com.cozyhome.game.mesh.mottled
import com.cozyhome.game.mesh.rgb
import com.cozyhome.game.models.Outfit
import com.cozyhome.game.models.Palette
import com.cozyhome.game.models.bush
import com.cozyhome.game.models.flower
import com.cozyhome.game.state.Appearance
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberView
import kotlinx.coroutines.isActive
import kotlin.random.Random

@Composable
fun CreateScreen(initialName: String, initial: Appearance, editing: Boolean, onDone: (String, Appearance) -> Unit, onCancel: () -> Unit) {
    var look by remember { mutableStateOf(initial) }
    val defaultName = stringResource(R.string.default_name)
    var name by remember { mutableStateOf(initialName) }

    Row(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom))),
    ) {
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Preview(look)
            Column(Modifier.align(Alignment.TopStart).padding(22.dp)) {
                Text(stringResource(if (editing) R.string.create_edit_title else R.string.create_title), color = White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.tagline), color = Ink.copy(alpha = 0.65f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        val shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
        Column(
            Modifier
                .width(340.dp)
                .fillMaxHeight()
                .shadow(16.dp, shape, ambientColor = Shadow, spotColor = Shadow)
                .background(Cream, shape)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NameField(name, defaultName) { name = it.take(16) }
                SwatchLine(stringResource(R.string.create_skin), Palette.SKIN.map { Color(it.argb) }, look.skin) { look = look.copy(skin = it) }
                OptionLine(stringResource(R.string.create_hair), listOf("👦", "👧", "⚡", "🍡", "🎀"), look.hairStyle) { look = look.copy(hairStyle = it) }
                SwatchLine(stringResource(R.string.create_hair_color), Palette.HAIR.map { Color(it.argb) }, look.hairColor) { look = look.copy(hairColor = it) }
                SwatchLine(stringResource(R.string.create_shirt), Palette.SHIRT.map { Color(it.argb) }, look.shirt) { look = look.copy(shirt = it) }
                SwatchLine(stringResource(R.string.create_pants), Palette.PANTS.map { Color(it.argb) }, look.pants) { look = look.copy(pants = it) }
                SwatchLine(stringResource(R.string.create_shoes), Palette.SHOES.map { Color(it.argb) }, look.shoes) { look = look.copy(shoes = it) }
                OptionLine(stringResource(R.string.create_hat), listOf("➖", "🎩", "🌸", "❄️"), look.hat) { look = look.copy(hat = it) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (editing) com.cozyhome.app.ui.components.RoundButton("⬅️", InkSoft, size = 46.dp) { onCancel() }
                com.cozyhome.app.ui.components.RoundButton("🎲", Lilac, size = 46.dp) {
                    look = Appearance(
                        skin = Random.nextInt(Palette.SKIN.size),
                        hairStyle = Random.nextInt(Palette.HAIR_STYLES),
                        hairColor = Random.nextInt(Palette.HAIR.size),
                        shirt = Random.nextInt(Palette.SHIRT.size),
                        pants = Random.nextInt(Palette.PANTS.size),
                        shoes = Random.nextInt(Palette.SHOES.size),
                        hat = Random.nextInt(Palette.HATS),
                    )
                }
                ChunkyButton(stringResource(if (editing) R.string.create_save else R.string.create_start), Mint, emoji = "🏡", modifier = Modifier.weight(1f), height = 46.dp, fontSize = 14.sp) {
                    onDone(name.ifBlank { defaultName }, look)
                }
            }
        }
    }
}

@Composable
private fun NameField(value: String, placeholder: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .background(White, shape)
            .border(2.dp, Mint, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) Text(placeholder, color = InkSoft.copy(alpha = 0.6f), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun SwatchLine(title: String, colors: List<Color>, selected: Int, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            colors.forEachIndexed { i, c ->
                Box(
                    Modifier
                        .size(30.dp)
                        .bouncyClick { onPick(i) }
                        .shadow(2.dp, CircleShape)
                        .background(c, CircleShape)
                        .border(if (i == selected) 4.dp else 2.dp, if (i == selected) Sun else White, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun OptionLine(title: String, emojis: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            emojis.forEachIndexed { i, e ->
                Box(
                    Modifier
                        .size(42.dp)
                        .bouncyClick { onPick(i) }
                        .background(if (i == selected) Sun else White, RoundedCornerShape(12.dp))
                        .border(2.dp, if (i == selected) Coral else Ink.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text(e, fontSize = 20.sp) }
            }
        }
    }
}

@Composable
private fun Preview(look: Appearance) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val view = rememberView(engine)
    val context = androidx.compose.ui.platform.LocalContext.current
    val environment = rememberEnvironment(environmentLoader) { buildSkyEnvironment(engine, context, rgb(0xBDE9FF), 9_000f) }
    val camera = rememberCameraNode(engine) {
        near = 0.05f
        far = 60f
        focalLength = 36.0
        position = Position(0.35f, 1.35f, 5.4f)
        lookAt(Position(0.35f, 0.62f, 0f))
    }
    val sun = rememberSun(engine)
    val fill = rememberFill(engine)
    val refs = remember { RigRefs() }
    val outfit = remember(look) { Outfit.of(look) }
    val animator = remember { Animator() }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        var clock = 0f
        while (isActive) {
            withFrameNanos { t ->
                val dt = ((t - last) / 1e9f).coerceIn(0f, 0.05f)
                last = t
                clock += dt
                val phase = (clock % 9f)
                animator.play(if (phase in 4f..5.6f) Anim.WAVE else Anim.IDLE)
                animator.update(dt, 0f)
                refs.pose(V3.ZERO, 18f + kotlin.math.sin(clock * 0.5f) * 28f, animator.pose)
            }
        }
    }
    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        environmentLoader = environmentLoader,
        view = view,
        environment = environment,
        mainLightNode = sun,
        fillLightNode = fill,
        cameraNode = camera,
        cameraManipulator = null,
        onGestureListener = null,
        autoCenterContent = false,
    ) {
        val stage = rememberMeshInstance(modelLoader, "stage") {
            mesh {
                mat = Mat(roughness = 1f)
                cylinder(0f, -0.2f, 0f, 1.25f, 0.2f, mottled(Palette.GRASS, Palette.GRASS_DARK, 2f), sides = 36)
                mat = Mat.MATTE
                cylinder(0f, -0.25f, 0f, 1.32f, 0.12f, Palette.SOIL, sides = 36)
                bush(-0.95f, -0.55f, 0.75f, 3, Palette.PINK)
                bush(1.0f, -0.4f, 0.6f, 4)
                for (k in 0 until 7) flower(-0.7f + k * 0.25f, 0.85f + (k % 2) * 0.12f, 0.2f, listOf(Palette.PINK, Palette.YELLOW, Palette.WHITE, Palette.LILAC)[k % 4], k)
            }
        }
        stage?.let { Part(it) }
        CharacterRig(outfit, refs, "preview")
    }
}
