package bible.app.data

/** Размеры текста при чтении, sp: от очень мелкого до очень крупного. */
val TEXT_SIZES_SP = listOf(11f, 13f, 15f, 17f, 19f, 21f, 23f, 25f, 27f)

const val DEFAULT_TEXT_SIZE_SP = 19f

/** Шкала из пяти шагов в версии 0.1 (хранился индекс). */
private val LEGACY_SIZES_SP = listOf(15f, 17f, 19f, 21f, 23f)

/** Сохранённый размер (или индекс из старой версии) → ближайший размер текущей шкалы. */
fun resolveTextSizeSp(storedSp: Float?, legacyIndex: Int?): Float {
    val raw = storedSp ?: legacyIndex?.let { LEGACY_SIZES_SP.getOrNull(it) } ?: DEFAULT_TEXT_SIZE_SP
    return TEXT_SIZES_SP.minBy { kotlin.math.abs(it - raw) }
}
