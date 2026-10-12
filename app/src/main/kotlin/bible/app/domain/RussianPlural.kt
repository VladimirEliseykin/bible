package bible.app.domain

private fun plural(n: Int, one: String, few: String, many: String): String {
    val d = n % 10
    val h = n % 100
    val word = when {
        d == 1 && h != 11 -> one
        d in 2..4 && h !in 12..14 -> few
        else -> many
    }
    return "$n $word"
}

fun chaptersLabel(n: Int) = plural(n, "глава", "главы", "глав")

fun booksLabel(n: Int) = plural(n, "книга", "книги", "книг")
