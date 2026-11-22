package pl.blackwallet

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.max

enum class CardStyle(val label: String) {
    SOLID("Jednolita"), GRADIENT("Gradient"), IMAGE("Grafika"),
    PREMIUM("Czarna premium"), MINIMAL("Minimalna")
}

enum class Network(val label: String) { NONE("Brak"), VISA("VISA"), MASTERCARD("Mastercard"), AMEX("AMEX") }

/** Tylko dane wizualne. Brak numeru, CVV, daty ważności i PIN-u – celowo. */
data class WalletCard(
    val id: String,
    val name: String = "",
    val bank: String = "",
    val last4: String = "",
    val logoText: String = "",
    val logoPath: String? = null,
    val network: Network = Network.NONE,
    val style: CardStyle = CardStyle.SOLID,
    val bg: Long = 0xFF000000,
    val bg2: Long = 0xFF2A2A2D,
    val text: Long = 0xFFFFFFFF,
    val imagePath: String? = null,
    val scale: Float = 1f,
    val offX: Float = 0f,
    val offY: Float = 0f,
) {
    fun toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("bank", bank); put("last4", last4)
        put("logoText", logoText); put("logoPath", logoPath ?: JSONObject.NULL)
        put("network", network.name); put("style", style.name)
        put("bg", bg); put("bg2", bg2); put("text", text)
        put("imagePath", imagePath ?: JSONObject.NULL)
        put("scale", scale.toDouble()); put("offX", offX.toDouble()); put("offY", offY.toDouble())
    }

    companion object {
        fun fromJson(o: JSONObject) = WalletCard(
            id = o.getString("id"),
            name = o.optString("name"),
            bank = o.optString("bank"),
            last4 = o.optString("last4"),
            logoText = o.optString("logoText"),
            logoPath = if (o.isNull("logoPath")) null else o.optString("logoPath"),
            network = runCatching { Network.valueOf(o.optString("network")) }.getOrDefault(Network.NONE),
            style = runCatching { CardStyle.valueOf(o.optString("style")) }.getOrDefault(CardStyle.SOLID),
            bg = o.optLong("bg", 0xFF000000),
            bg2 = o.optLong("bg2", 0xFF2A2A2D),
            text = o.optLong("text", 0xFFFFFFFF),
            imagePath = if (o.isNull("imagePath")) null else o.optString("imagePath"),
            scale = o.optDouble("scale", 1.0).toFloat(),
            offX = o.optDouble("offX", 0.0).toFloat(),
            offY = o.optDouble("offY", 0.0).toFloat(),
        )
    }
}

data class AppData(val cards: List<WalletCard> = emptyList(), val defaultImage: String? = null)

/** Lokalny zapis w prywatnym katalogu aplikacji (filesDir). Nic nie opuszcza telefonu. */
class Store(private val ctx: Context) {
    private val file = File(ctx.filesDir, "wallet.json")
    private val imagesDir = File(ctx.filesDir, "images").apply { mkdirs() }

    fun load(): AppData = runCatching {
        if (!file.exists()) return AppData()
        val o = JSONObject(file.readText())
        val arr = o.optJSONArray("cards") ?: JSONArray()
        AppData(
            cards = (0 until arr.length()).map { WalletCard.fromJson(arr.getJSONObject(it)) },
            defaultImage = if (o.isNull("defaultImage")) null else o.optString("defaultImage"),
        )
    }.getOrDefault(AppData())

    fun save(d: AppData) {
        val o = JSONObject()
        o.put("cards", JSONArray(d.cards.map { it.toJson() }))
        o.put("defaultImage", d.defaultImage ?: JSONObject.NULL)
        val tmp = File(ctx.filesDir, "wallet.json.tmp")
        tmp.writeText(o.toString())
        tmp.renameTo(file)
    }

    /** Kopiuje wybrany obraz do prywatnego katalogu aplikacji (maks. 1600 px). */
    fun importImage(uri: Uri, png: Boolean): String? = runCatching {
        val src = ImageDecoder.createSource(ctx.contentResolver, uri)
        val bmp = ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            val m = max(w, h)
            if (m > 1600) {
                val f = 1600f / m
                decoder.setTargetSize((w * f).toInt().coerceAtLeast(1), (h * f).toInt().coerceAtLeast(1))
            }
        }
        val f = File(imagesDir, UUID.randomUUID().toString() + if (png) ".png" else ".jpg")
        f.outputStream().use {
            bmp.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 92, it)
        }
        f.absolutePath
    }.getOrNull()

    fun cleanup(d: AppData) {
        val used = d.cards.flatMap { listOfNotNull(it.imagePath, it.logoPath) }.toSet() +
            listOfNotNull(d.defaultImage)
        imagesDir.listFiles()?.forEach { if (it.absolutePath !in used) it.delete() }
    }
}

sealed interface Screen {
    data object Home : Screen
    data class Detail(val id: String) : Screen
    data class Edit(val id: String?) : Screen
}

class WalletViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    var data by mutableStateOf(store.load()); private set
    var screen by mutableStateOf<Screen>(Screen.Home)

    fun save(card: WalletCard) {
        val exists = data.cards.any { it.id == card.id }
        val list = if (exists) data.cards.map { if (it.id == card.id) card else it } else data.cards + card
        commit(data.copy(cards = list), cleanup = true)
    }

    fun delete(id: String) = commit(data.copy(cards = data.cards.filterNot { it.id == id }), cleanup = true)

    fun setDefaultImage(path: String?) = commit(data.copy(defaultImage = path), cleanup = false)

    suspend fun importImage(uri: Uri, png: Boolean): String? =
        withContext(Dispatchers.IO) { store.importImage(uri, png) }

    private fun commit(d: AppData, cleanup: Boolean) {
        data = d
        store.save(d)
        if (cleanup) store.cleanup(d)
    }
}
