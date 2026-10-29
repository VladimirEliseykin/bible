package bible.dbgen

import org.tartarus.snowball.ext.RussianStemmer

object TextNormalizer {
    private val word = Regex("""[а-я]+(?:-[а-я]+)*""")

    /** В приложении везде короткое тире «–» (решение владельца): длинное «—» в тексте перевода заменяется. */
    fun dashes(text: String): String = text.replace('—', '–')

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
