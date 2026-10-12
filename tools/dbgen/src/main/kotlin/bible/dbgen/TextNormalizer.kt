package bible.dbgen

import org.tartarus.snowball.ext.RussianStemmer

object TextNormalizer {
    private val word = Regex("""[а-я]+(?:-[а-я]+)*""")

    fun stems(text: String): String {
        val stemmer = RussianStemmer()
        val lower = text.lowercase().replace('ё', 'е')
        return word.findAll(lower).joinToString(" ") { m ->
            stemmer.current = m.value
            stemmer.stem()
            stemmer.current
        }
    }
}
