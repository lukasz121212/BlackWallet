package pl.blackwallet

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.UUID

private val PRESET = listOf(
    0xFF000000L, 0xFF1C1C1EL, 0xFF3A3A3CL, 0xFFFFFFFFL, 0xFFD32F2FL, 0xFFEF6C00L,
    0xFFC9A227L, 0xFF1B8E3EL, 0xFF00796BL, 0xFF1565C0L, 0xFF6A1B9AL, 0xFFC2185BL,
)
private val BANKS = listOf("mBank", "PKO BP", "ING", "Santander", "Pekao", "Millennium", "Revolut", "AMEX")

@Composable
private fun Section(title: String) =
    Text(title, color = Color(0xFFA8C7FA), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 10.dp))

@Composable
private fun <T> ChipRow(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { FilterChip(selected = it == selected, onClick = { onSelect(it) }, label = { Text(label(it)) }) }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text("$label: ${"%.2f".format(value)}", color = Color(0xFFC4C7C5))
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ColorRow(title: String, value: Long, onChange: (Long) -> Unit) {
    var hex by remember(value) { mutableStateOf(String.format("%06X", value and 0xFFFFFFL)) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Color(0xFFC4C7C5))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PRESET.forEach { c ->
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Color(c))
                        .border(2.dp, if (c == value) Color.White else Color(0x44FFFFFF), CircleShape)
                        .clickable { onChange(c) }
                )
            }
        }
        OutlinedTextField(
            value = hex,
            onValueChange = { s ->
                val t = s.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                hex = t
                if (t.length == 6) onChange(0xFF000000L or t.toLong(16))
            },
            label = { Text("Kolor HEX (#)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: WalletViewModel, id: String?) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val def = vm.data.defaultImage
    var card by remember {
        mutableStateOf(
            vm.data.cards.firstOrNull { it.id == id } ?: WalletCard(
                id = UUID.randomUUID().toString(),
                imagePath = def,
                style = if (def != null) CardStyle.IMAGE else CardStyle.SOLID,
            )
        )
    }
    val back = { vm.screen = if (id != null) Screen.Detail(id) else Screen.Home }

    val pickBg = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            vm.importImage(uri, false)?.let { p ->
                card = card.copy(imagePath = p, style = CardStyle.IMAGE, scale = 1f, offX = 0f, offY = 0f)
            }
        }
    }
    val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { vm.importImage(uri, true)?.let { p -> card = card.copy(logoPath = p) } }
    }
    val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(if (id == null) "Nowa karta" else "Edytuj kartę") },
                navigationIcon = {
                    IconButton(onClick = { back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(Color.Black).navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = { back() }, modifier = Modifier.weight(1f)) { Text("Anuluj") }
                Button(
                    onClick = {
                        vm.save(card.copy(name = card.name.trim(), bank = card.bank.trim(), logoText = card.logoText.trim()))
                        vm.screen = Screen.Detail(card.id)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Zapisz") }
            }
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardView(
                card, Modifier.fillMaxWidth(),
                onImageTransform = if (card.style == CardStyle.IMAGE && card.imagePath != null) {
                    { z, px, py ->
                        card = card.copy(
                            scale = (card.scale * z).coerceIn(0.5f, 5f),
                            offX = (card.offX + px).coerceIn(-1.5f, 1.5f),
                            offY = (card.offY + py).coerceIn(-1.5f, 1.5f),
                        )
                    }
                } else null,
            )
            if (card.style == CardStyle.IMAGE && card.imagePath != null) {
                Text("Przeciągnij i uszczypnij grafikę bezpośrednio na karcie.", color = Color(0xFF8E918F),
                    style = MaterialTheme.typography.bodySmall)
            }

            Section("Dane")
            OutlinedTextField(card.name, { card = card.copy(name = it) }, label = { Text("Nazwa karty") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(card.bank, { card = card.copy(bank = it) }, label = { Text("Bank") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            ChipRow(BANKS, card.bank, { it }) { b ->
                card = card.copy(bank = b, logoText = if (card.logoText.isBlank() || card.logoText == card.bank) b else card.logoText)
            }
            OutlinedTextField(
                card.last4, { s -> card = card.copy(last4 = s.filter(Char::isDigit).take(4)) },
                label = { Text("Ostatnie 4 cyfry (opcjonalnie)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Section("Styl karty")
            ChipRow(CardStyle.entries, card.style, { it.label }) { card = card.copy(style = it) }
            Button(
                onClick = { card = card.copy(style = CardStyle.SOLID, bg = 0xFF000000, text = 0xFFFFFFFF) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Ustaw całkowicie czarną kartę") }

            if (card.style == CardStyle.IMAGE) {
                Section("Grafika karty")
                Button(onClick = { pickBg.launch(imageOnly) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Wybierz zdjęcie z galerii")
                }
                if (card.imagePath != null) {
                    LabeledSlider("Rozmiar", card.scale, 0.5f..5f) { card = card.copy(scale = it) }
                    LabeledSlider("Położenie poziomo", card.offX, -1f..1f) { card = card.copy(offX = it) }
                    LabeledSlider("Położenie pionowo", card.offY, -1f..1f) { card = card.copy(offY = it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { card = card.copy(scale = 1f, offX = 0f, offY = 0f) }) { Text("Wyśrodkuj") }
                        OutlinedButton(onClick = { card = card.copy(imagePath = null) }) { Text("Usuń grafikę") }
                    }
                    OutlinedButton(
                        onClick = {
                            vm.setDefaultImage(card.imagePath)
                            Toast.makeText(ctx, "Ustawiono jako domyślną grafikę", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Ustaw tę grafikę jako domyślną") }
                }
                if (vm.data.defaultImage != null && card.imagePath != vm.data.defaultImage) {
                    OutlinedButton(
                        onClick = { card = card.copy(imagePath = vm.data.defaultImage, scale = 1f, offX = 0f, offY = 0f) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Użyj domyślnej grafiki") }
                }
            }

            Section("Kolory")
            if (card.style != CardStyle.PREMIUM) {
                ColorRow(if (card.style == CardStyle.GRADIENT) "Kolor tła (początek)" else "Kolor tła", card.bg) {
                    card = card.copy(bg = it)
                }
            }
            if (card.style == CardStyle.GRADIENT) {
                ColorRow("Kolor tła (koniec)", card.bg2) { card = card.copy(bg2 = it) }
            }
            ColorRow("Kolor napisów", card.text) { card = card.copy(text = it) }

            Section("Logo i napis")
            OutlinedTextField(card.logoText, { card = card.copy(logoText = it) }, label = { Text("Napis / logo tekstowe") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickLogo.launch(imageOnly) }) { Text("Własne logo (obraz)") }
                if (card.logoPath != null) OutlinedButton(onClick = { card = card.copy(logoPath = null) }) { Text("Usuń logo") }
            }
            Text("Sieć (tylko napis ozdobny)", color = Color(0xFFC4C7C5))
            ChipRow(Network.entries, card.network, { it.label }) { card = card.copy(network = it) }
        }
    }
}
