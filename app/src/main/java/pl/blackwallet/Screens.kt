package pl.blackwallet

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue

private const val NOTE = "Black Wallet to wizualny portfel – nie wykonuje płatności NFC. " +
    "Prawdziwe płatności nadal obsługuje Google Wallet."

@Composable
fun HomeScreen(vm: WalletViewModel) {
    val cards = vm.data.cards
    Scaffold(
        containerColor = Color.Black,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.screen = Screen.Edit(null) },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Dodaj kartę") },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = pad.calculateTopPadding() + 12.dp,
                bottom = pad.calculateBottomPadding() + 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    "Black Wallet", style = MaterialTheme.typography.headlineMedium,
                    color = Color.White, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
            }
            if (cards.isEmpty()) {
                item {
                    Text(
                        "Nie masz jeszcze żadnych kart.\nDotknij „Dodaj kartę”, aby stworzyć pierwszą.",
                        color = Color(0xFFC4C7C5), textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    )
                }
            }
            items(cards, key = { it.id }) { c ->
                CardView(
                    c,
                    Modifier.fillMaxWidth().animateItem(),
                    onClick = { vm.screen = Screen.Detail(c.id) },
                )
            }
            item {
                Text(NOTE, color = Color(0xFF8E918F), style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: WalletViewModel, id: String) {
    val cards = vm.data.cards
    if (cards.isEmpty()) {
        LaunchedEffect(Unit) { vm.screen = Screen.Home }
        return
    }
    val pager = rememberPagerState(
        initialPage = cards.indexOfFirst { it.id == id }.coerceAtLeast(0),
        pageCount = { vm.data.cards.size },
    )
    var confirm by remember { mutableStateOf(false) }
    val current = cards[pager.currentPage.coerceIn(cards.indices)]

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Black Wallet") },
                navigationIcon = {
                    IconButton(onClick = { vm.screen = Screen.Home }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wstecz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = 28.dp),
                pageSpacing = 14.dp,
            ) { page ->
                val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction)
                    .absoluteValue.coerceIn(0f, 1f)
                val s = lerp(0.88f, 1f, 1f - off)
                val c = vm.data.cards.getOrNull(page)
                if (c != null) {
                    CardView(
                        c,
                        Modifier.fillMaxWidth().graphicsLayer {
                            scaleX = s; scaleY = s; alpha = lerp(0.6f, 1f, 1f - off)
                        },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(cards.size) { i ->
                    val sz by animateDpAsState(if (i == pager.currentPage) 9.dp else 6.dp, label = "dot")
                    Box(
                        Modifier.size(sz).clip(CircleShape)
                            .background(if (i == pager.currentPage) Color.White else Color(0xFF5F6368))
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(current.name.ifBlank { "Karta" }, color = Color.White, style = MaterialTheme.typography.titleLarge)
            if (current.bank.isNotBlank()) Text(current.bank, color = Color(0xFFC4C7C5))
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { vm.screen = Screen.Edit(current.id) }) {
                    Icon(Icons.Default.Edit, null); Spacer(Modifier.size(8.dp)); Text("Edytuj")
                }
                OutlinedButton(onClick = { confirm = true }) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFF28B82))
                    Spacer(Modifier.size(8.dp)); Text("Usuń", color = Color(0xFFF28B82))
                }
            }
            Spacer(Modifier.weight(1f))
            Text(NOTE, color = Color(0xFF8E918F), style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Usunąć kartę?") },
            text = { Text("Usunięta zostanie tylko wizualna karta w tej aplikacji.") },
            confirmButton = {
                TextButton(onClick = { confirm = false; vm.delete(current.id); vm.screen = Screen.Home }) {
                    Text("Usuń", color = Color(0xFFF28B82))
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Anuluj") } },
        )
    }
}
