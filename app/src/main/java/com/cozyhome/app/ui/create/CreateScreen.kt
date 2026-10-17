package com.cozyhome.app.ui.create

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.cozyhome.app.render.LocalTextureBank
import com.cozyhome.app.render.buildSkyEnvironment
import com.cozyhome.app.render.rememberFill
import com.cozyhome.app.render.rememberMeshInstance
import com.cozyhome.app.render.rememberMeshThumb
import com.cozyhome.app.render.rememberSun
import com.cozyhome.app.render.rememberTextureBank
import com.cozyhome.app.ui.components.ChunkyButton
import com.cozyhome.app.ui.components.GameIcon
import com.cozyhome.app.ui.components.Ico
import com.cozyhome.app.ui.components.IconButton
import com.cozyhome.app.ui.components.bouncyClick
import com.cozyhome.app.ui.components.outlined
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
import com.cozyhome.game.mesh.Mesh
import com.cozyhome.game.mesh.mesh
import com.cozyhome.game.mesh.rgb
import com.cozyhome.game.models.CharacterModel
import com.cozyhome.game.models.Outfit
import com.cozyhome.game.models.Palette
import com.cozyhome.game.models.bush
import com.cozyhome.game.models.flower
import com.cozyhome.game.models.grassTuft
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

private enum class Tab(val ico: Ico) { FACE(Ico.SMILE), HAIR(Ico.SPARKLE), TOP(Ico.SHIRT), BOTTOM(Ico.BOX), SHOES(Ico.RUN), HAT(Ico.STAR) }

@Composable
fun CreateScreen(initialName: String, initial: Appearance, editing: Boolean, onDone: (String, Appearance) -> Unit, onCancel: () -> Unit) {
    var look by remember { mutableStateOf(initial) }
    val defaultName = stringResource(R.string.default_name)
    var name by remember { mutableStateOf(initialName) }
    var tab by remember { mutableStateOf(Tab.FACE) }

    Row(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))) {
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Preview(look)
            Column(Modifier.align(Alignment.TopStart).padding(18.dp)) {
                Text(stringResource(if (editing) R.string.create_edit_title else R.string.create_title), style = outlined(24.sp))
                Text(stringResource(R.string.tagline), color = Ink.copy(alpha = 0.65f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.align(Alignment.BottomStart).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (editing) IconButton(Ico.BACK, InkSoft, size = 46.dp) { onCancel() }
                IconButton(Ico.DICE, Lilac, size = 46.dp, label = stringResource(R.string.create_random)) { look = randomLook() }
            }
        }
        val shape = RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp)
        Column(
            Modifier
                .width(372.dp)
                .fillMaxHeight()
                .shadow(18.dp, shape, ambientColor = Shadow, spotColor = Shadow)
                .background(Brush.verticalGradient(listOf(White, Cream)), shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NameField(name, defaultName) { name = it.take(16) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (t in Tab.entries) TabButton(t, t == tab) { tab = t }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    Tab.FACE -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Swatches(stringResource(R.string.create_skin), Palette.SKIN.map { Color(it.argb) }, look.skin) { look = look.copy(skin = it) }
                        StyleRow(stringResource(R.string.create_eyes), Palette.EYE_STYLES, look.eyeStyle, { i -> "eye-$i-${look.skin}-${look.eyeColor}-${look.hairColor}-${look.hairStyle}" }, { i -> headMesh(look.copy(eyeStyle = i)) }) { look = look.copy(eyeStyle = it) }
                        Swatches(stringResource(R.string.create_eye_color), Palette.EYES.map { Color(it.argb) }, look.eyeColor) { look = look.copy(eyeColor = it) }
                    }
                    Tab.HAIR -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Swatches(stringResource(R.string.create_hair_color), Palette.HAIR.map { Color(it.argb) }, look.hairColor) { look = look.copy(hairColor = it) }
                        StyleGrid(Palette.HAIR_STYLES, look.hairStyle, { i -> "hair-$i-${look.hairColor}-${look.skin}" }, { i -> headMesh(look.copy(hairStyle = i, hat = 0)) }) { look = look.copy(hairStyle = it) }
                    }
                    Tab.TOP -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Swatches(stringResource(R.string.create_shirt), Palette.SHIRT.map { Color(it.argb) }, look.shirt) { look = look.copy(shirt = it) }
                        StyleGrid(Palette.TOPS, look.top, { i -> "top-$i-${look.shirt}-${look.pants}-${look.skin}" }, { i -> bodyMesh(look.copy(top = i)) }, yaw = -24f) { look = look.copy(top = it) }
                    }
                    Tab.BOTTOM -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Swatches(stringResource(R.string.create_pants), Palette.PANTS.map { Color(it.argb) }, look.pants) { look = look.copy(pants = it) }
                        StyleGrid(Palette.BOTTOMS, look.bottom, { i -> "bottom-$i-${look.pants}-${look.shirt}-${look.top}-${look.skin}" }, { i -> bodyMesh(look.copy(bottom = i)) }, yaw = -24f) { look = look.copy(bottom = it) }
                    }
                    Tab.SHOES -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Swatches(stringResource(R.string.create_shoes), Palette.SHOES.map { Color(it.argb) }, look.shoes) { look = look.copy(shoes = it) }
                    }
                    Tab.HAT -> StyleGrid(Palette.HATS, look.hat, { i -> "hat-$i-${look.hairStyle}-${look.hairColor}-${look.skin}" }, { i -> headMesh(look.copy(hat = i)) }) { look = look.copy(hat = it) }
                }
            }
            ChunkyButton(stringResource(if (editing) R.string.create_save else R.string.create_start), Mint, emoji = "🏡", modifier = Modifier.fillMaxWidth(), height = 46.dp, fontSize = 15.sp) {
                onDone(name.ifBlank { defaultName }, look)
            }
        }
    }
}

private fun randomLook() = Appearance(
    skin = Random.nextInt(Palette.SKIN.size),
    hairStyle = Random.nextInt(Palette.HAIR_STYLES),
    hairColor = Random.nextInt(Palette.HAIR.size),
    shirt = Random.nextInt(Palette.SHIRT.size),
    pants = Random.nextInt(Palette.PANTS.size),
    shoes = Random.nextInt(Palette.SHOES.size),
    hat = if (Random.nextFloat() < 0.4f) Random.nextInt(1, Palette.HATS) else 0,
    top = Random.nextInt(Palette.TOPS),
    bottom = Random.nextInt(Palette.BOTTOMS),
    eyeColor = Random.nextInt(Palette.EYES.size),
    eyeStyle = Random.nextInt(Palette.EYE_STYLES),
)

private fun headMesh(a: Appearance): Mesh {
    val o = Outfit.of(a)
    return mesh {
        include(CharacterModel.head(o))
        group { translate(0f, CharacterModel.EYE_Y, 0f); include(CharacterModel.eyes(o)) }
    }
}

private fun bodyMesh(a: Appearance): Mesh = CharacterModel.full(Outfit.of(a))

@Composable
private fun TabButton(t: Tab, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(48.dp, 40.dp)
            .bouncyClick(onClick = onClick)
            .shadow(if (selected) 5.dp else 0.dp, shape, ambientColor = Shadow, spotColor = Shadow)
            .background(if (selected) Brush.verticalGradient(listOf(Coral.copy(green = 0.55f), Coral)) else Brush.verticalGradient(listOf(White, White)), shape)
            .border(1.5.dp, if (selected) White else Ink.copy(alpha = 0.08f), shape),
        contentAlignment = Alignment.Center,
    ) {
        GameIcon(t.ico, 22.dp, if (selected) White else InkSoft)
    }
}

@Composable
private fun NameField(value: String, placeholder: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(White, shape)
            .border(2.dp, Mint.copy(alpha = 0.6f), shape)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GameIcon(Ico.PERSON, 18.dp, Mint)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = InkSoft.copy(alpha = 0.6f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun Swatches(title: String, colors: List<Color>, selected: Int, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            colors.forEachIndexed { i, c ->
                val on = i == selected
                Box(
                    Modifier
                        .size(30.dp)
                        .bouncyClick { onPick(i) }
                        .shadow(if (on) 5.dp else 2.dp, CircleShape)
                        .background(Brush.verticalGradient(listOf(c.copy(red = (c.red + 0.12f).coerceAtMost(1f), green = (c.green + 0.12f).coerceAtMost(1f), blue = (c.blue + 0.12f).coerceAtMost(1f)), c)), CircleShape)
                        .border(if (on) 3.5.dp else 2.dp, if (on) Sun else White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (on) GameIcon(Ico.CHECK, 14.dp, White) }
            }
        }
    }
}

@Composable
private fun StyleCard(thumbKey: String, build: () -> Mesh, selected: Boolean, yaw: Float, modifier: Modifier, onClick: () -> Unit) {
    val img = rememberMeshThumb(thumbKey, 144, yaw, 6f, build)
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .aspectRatio(1f)
            .bouncyClick(onClick = onClick)
            .shadow(if (selected) 6.dp else 2.dp, shape, ambientColor = Shadow, spotColor = Shadow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(if (selected) Sun.copy(alpha = 0.35f) else White, if (selected) Sun.copy(alpha = 0.18f) else Cream)))
            .border(if (selected) 3.dp else 1.5.dp, if (selected) Sun else Ink.copy(alpha = 0.08f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (img != null) Image(img, null, Modifier.fillMaxSize().padding(4.dp))
        else Box(Modifier.size(18.dp).background(Ink.copy(alpha = 0.08f), CircleShape))
    }
}

@Composable
private fun StyleGrid(count: Int, selected: Int, key: (Int) -> String, build: (Int) -> Mesh, yaw: Float = -14f, onPick: (Int) -> Unit) {
    LazyVerticalGrid(GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items((0 until count).toList()) { i ->
            StyleCard(key(i), { build(i) }, i == selected, yaw, Modifier) { onPick(i) }
        }
    }
}

@Composable
private fun StyleRow(title: String, count: Int, selected: Int, key: (Int) -> String, build: (Int) -> Mesh, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until count) StyleCard(key(i), { build(i) }, i == selected, -10f, Modifier.size(76.dp)) { onPick(i) }
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
        focalLength = 40.0
        position = Position(0.25f, 1.3f, 4.6f)
        lookAt(Position(0.25f, 0.78f, 0f))
    }
    val sun = rememberSun(engine)
    val fill = rememberFill(engine)
    val refs = remember { RigRefs() }
    val bank = rememberTextureBank(engine)
    LaunchedEffect(bank) { bank.load() }
    LaunchedEffect(view) {
        view.colorGrading = com.google.android.filament.ColorGrading.Builder()
            .toneMapper(com.google.android.filament.ToneMapper.PBRNeutralToneMapper())
            .contrast(1.05f)
            .build(engine)
    }
    val outfit = remember(look) { Outfit.of(look) }
    val animator = remember { Animator() }
    var cheer by remember { mutableIntStateOf(0) }
    LaunchedEffect(outfit) { cheer++ }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        var clock = 0f
        var lastCheer = 0
        var cheerTime = 0f
        while (isActive) {
            withFrameNanos { t ->
                val dt = ((t - last) / 1e9f).coerceIn(0f, 0.05f)
                last = t
                clock += dt
                if (cheer != lastCheer) {
                    lastCheer = cheer
                    cheerTime = 0.9f
                }
                cheerTime -= dt
                val phase = clock % 9f
                animator.play(
                    when {
                        cheerTime > 0f && lastCheer > 1 -> Anim.CHEER
                        phase in 4f..5.6f -> Anim.WAVE
                        else -> Anim.IDLE
                    }
                )
                animator.update(dt, 0f)
                refs.pose(V3.ZERO, 14f + kotlin.math.sin(clock * 0.45f) * 24f, animator.pose)
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
        CompositionLocalProvider(LocalTextureBank provides bank) { PreviewStage(outfit, refs) }
    }
}

@Composable
private fun io.github.sceneview.SceneScope.PreviewStage(outfit: Outfit, refs: RigRefs) {
    val stage = rememberMeshInstance(modelLoader, "stage-v2") {
        mesh {
            withMat(Mat.GRASS) { cylinder(0f, -0.2f, 0f, 1.3f, 0.2f, rgb(0x78A552), sides = 48) }
            withMat(Mat.STONE) { cylinder(0f, -0.26f, 0f, 1.38f, 0.14f, rgb(0xB5AFA4), sides = 48) }
            bush(-0.98f, -0.55f, 0.75f, 3, Palette.PINK)
            bush(1.02f, -0.42f, 0.6f, 4)
            for (k in 0 until 7) flower(-0.7f + k * 0.24f, 0.86f + (k % 2) * 0.12f, 0.2f, listOf(Palette.PINK, Palette.YELLOW, Palette.WHITE, Palette.LILAC)[k % 4], k)
            for (k in 0 until 10) {
                val a = k * 0.63f
                grassTuft(kotlin.math.cos(a) * 1.05f, kotlin.math.sin(a) * 1.05f, 0.9f, 400 + k)
            }
        }
    }
    stage?.let { Part(it) }
    CharacterRig(outfit, refs, "preview")
}
