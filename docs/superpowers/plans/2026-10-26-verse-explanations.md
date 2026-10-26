# Объяснение стиха — план реализации

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans (выбрано владельцем: выполнение в этой сессии). Шаги — чекбоксы `- [ ]`.

**Goal:** Нажатие на стих открывает страницу объяснения (утверждённый экран); объяснения — офлайн-пакет `commentary.db`, скачиваемый одним файлом; пилот данных — Сирах 2.

**Architecture:** `tools/commentary` (Kotlin/JVM) собирает пакет из JSON по главам и общедоступных источников, проверяет его и пишет `commentary.db` + манифест. Приложение скачивает пакет с GitHub Releases (`PackManager`), читает его (`CommentaryDatabase`), показывает `VerseScreen` (HorizontalPager по всем стихам Библии, шапка на Haze-стекле, вкладки, нижняя плашка).

**Изменение после Task 8 (решение владельца):** база объяснений входит в APK, скачивания нет. `PackManager`, карточка «Скачать», разрешение INTERNET и релиз пакета убраны; `commentary.db` собирает задача приложения `generateCommentaryDb`, ставит `AssetInstaller`. Пункты ниже про скачивание выполнены и затем заменены этим решением.

**Tech Stack:** Kotlin 2.2.20, Compose (BOM 2025.09.01), kotlinx-serialization-json 1.9.0, Haze 1.6.10, sqlite-jdbc (только инструмент), `HttpURLConnection` (без OkHttp), `java.util.zip`.

**Spec:** `docs/superpowers/specs/2026-10-26-verse-explanations-design.md`

## Global Constraints

- Офлайн после скачивания пакета; в манифест добавляется `android.permission.INTERNET` только ради скачивания пакета.
- В текстах объяснений — только короткое тире «–»; текст Синодального перевода не изменяется.
- `verse.id = book*1_000_000 + chapter*1_000 + verse`; Сирах = 27, Матфей = 51, Иаков = 56.
- Пакет: `https://github.com/VladimirEliseykin/bible/releases/latest/download/commentary.json` (манифест: `version`, `url`, `size`, `sha256`, `bytes_unpacked`).
- Цвета: золотая пилюля фон `#E9D3A0`, текст `#3B2C10`; стекло `rgba(18,20,26,.42)` + blur 14dp; без Haze (Android < 12) — `rgba(18,20,26,.72)`.
- Церковнославянский — шрифт Ponomar Unicode (OFL), `res/font/ponomar.ttf`, лицензия в `res/raw/ofl_ponomar.txt`.
- Не использовать python-скрипты; каждая git-команда отдельно; коммиты `type: description` на английском с подписью Co-Authored-By.
- Полный прогон генерации через Claude API в этот план не входит.

## Review Focus

1. **Длинный стих и шрифт ×2** — плашка в шапке растёт вниз и не перекрывает «‹ Сирах 2» / «Поделиться». Тест: Task 7.
2. **Первый стих Библии и последний** — нет левой/правой подписи и стрелки, свайп в пустоту ничего не делает. Тест: Task 5.
3. **Пакет скачан частично / повреждён / нет места** — не считается установленным, понятная ошибка, «Повторить». Тест: Task 4.
4. **Стих без объяснения в пакете** — видны «Переводы», остальное «ещё готовится», страница не падает. Тест: Task 7.
5. **Возврат с экрана стиха** — системная «Назад» ведёт к главе на тот же стих, позиция чтения не сбрасывается. Тест: Task 8.

---

### Task 1: Модель объяснений и сериализация (общая для инструмента и приложения)

**Files:** `gradle/libs.versions.toml`, `build.gradle.kts`, `tools/commentary/build.gradle.kts` (новый модуль), `settings.gradle.kts`, `app/build.gradle.kts`; `tools/commentary/src/main/kotlin/bible/commentary/Model.kt`; `app/src/main/kotlin/bible/app/commentary/Model.kt` (тот же формат); тесты `ModelTest.kt` в обоих.

**Produces:**
```kotlin
@Serializable data class Item(val title: String, val text: String)
@Serializable data class Ref(val verse_id: Int, val to: Int? = null)
@Serializable data class Father(val author: String, val quote: String, val source: String)
@Serializable data class VerseNote(val lead: String = "", val meaning: List<Item> = emptyList(), val context: List<Item> = emptyList(), val refs: List<Ref> = emptyList(), val fathers: List<Father> = emptyList())
@Serializable data class ChapterNote(val context: List<Item> = emptyList())
// Входной файл главы (только инструмент):
@Serializable data class ChapterFile(val book: String, val chapter: Int, val chapter_note: ChapterNote = ChapterNote(), val verses: Map<String, VerseNote>)
fun encodeNote(n: VerseNote): ByteArray   // deflate(JSON)
fun decodeNote(b: ByteArray): VerseNote
```
- [ ] Тест: `decodeNote(encodeNote(x)) == x`; неизвестные поля JSON игнорируются (`ignoreUnknownKeys`). RED → реализация (`Json { ignoreUnknownKeys = true }`, `Deflater`/`Inflater`) → GREEN → commit `feat: commentary note model`.

### Task 2: Сборщик пакета `tools/commentary`

**Files:** `Validator.kt`, `PackWriter.kt`, `Main.kt`, `ValidatorTest.kt`, `PackWriterTest.kt`.

**Produces:** `fun validate(files: List<ChapterFile>, verseIds: Set<Int>, bookIds: Map<String, Int>, sources: Set<String>): List<String>`; `fun writePack(out: File, notes: Map<Int, VerseNote>, chapters: Map<Pair<Int,Int>, ChapterNote>, csl: Map<Int, String>, sources: List<Source>, version: Int)`; `main(bibleDb, notesDir, cslJson, outDir, version)` пишет `commentary.db`, `commentary.db.gz`, `commentary.json` (sha256 и размер `.gz`).

Проверки (каждая — отдельный тест RED→GREEN):
- стих из файла главы не существует в `bible.db` → «Сир 2:40: такого стиха нет»;
- `refs.verse_id`/`to` не существует → ошибка;
- в любом тексте объяснений есть «—» → «длинное тире»;
- пустой `title`/`text`/`lead` → ошибка; `lead` > 300 знаков → ошибка;
- `fathers.source` не из `sources` → ошибка.
- Церковнославянский: из `CSlElizabeth-CS.json` — книги по порядку (77, порядок совпадает с `BookCatalog`; проверка числа глав у каждой книги, иначе ошибка).
- [ ] commit `feat: commentary pack builder with validation`.

### Task 3: Пилот данных — Сирах 2

**Files:** `tools/commentary/notes/Сир/002.json`, `tools/commentary/sources/README.md` (откуда скачивать CS JSON, sha256), `tools/commentary/sources.json` (`lopukhin`, `filaret_slova`, …).

- [ ] Вычитать Лопухина на Сир 2 целиком (Азбука веры, `…/tolkovanie-na-knigu-premudrosti-iisusa-syna-sirahova/2`), дословно, порциями; цитаты — только дословные.
- [ ] Для каждого из 18 стихов: `lead`, 2–4 пункта `meaning`, `refs` (каждая сверяется по `bible.db`), `fathers` (Лопухин, где есть комментарий к стиху). `chapter_note.context`: кто и когда, обстановка, место в книге.
- [ ] Сборка пакета пилота проходит проверки; `commentary.db` открывается. commit `feat: pilot commentary for Sirach 2`.

### Task 4: Пакет в приложении — скачивание и чтение

**Files:** `app/src/main/kotlin/bible/app/commentary/{CommentaryDatabase.kt,PackManager.kt,PackState.kt}`, `AndroidManifest.xml` (INTERNET), `BibleApp.kt` (контейнер), тесты `PackManagerTest.kt` (JVM), `CommentaryDatabaseTest.kt` (androidTest).

**Produces:**
```kotlin
sealed interface PackState { data object Missing; data class Downloading(val progress: Float); data object Ready; data class Failed(val reason: Reason) }
enum class Reason { NoNetwork, NoSpace, Corrupt, Other }
class PackManager(fetch: (String) -> InputStream, dir: File) { val state: StateFlow<PackState>; suspend fun download(); fun installed(): File? }
class CommentaryDatabase(file: File) { fun note(verseId: Int): VerseNote?; fun chapter(bookId: Int, chapter: Int): ChapterNote?; fun translation(verseId: Int, code: String): String? }
```
- [ ] Тесты PackManager на подменённом `fetch`: успешная установка (sha256 совпал → файл на месте, `Ready`); неверная sha256 → `Failed(Corrupt)`, файла нет; поток рвётся `ENOSPC` → `Failed(NoSpace)`, временного файла нет; `UnknownHostException` → `NoNetwork`. RED→GREEN.
- [ ] Инструментальный тест: пилотный `commentary.db` (из `androidTest/assets`) — `note(27002001).lead` не пустой, `translation(27002001,"csl")` начинается с «Ча́до». commit `feat: commentary pack download and reader`.

### Task 5: Навигация по стихам

**Files:** `app/src/main/kotlin/bible/app/domain/VerseNavigator.kt`, `BibleDatabase.kt` (`verseIds(): IntArray` одним запросом), тест `VerseNavigatorTest.kt`.

**Produces:** `class VerseNavigator(ids: IntArray) { val size; fun indexOf(id: Int): Int; fun idAt(i: Int): Int; fun prev(id): Int?; fun next(id): Int?; fun label(id): String /* "1:30" */ }`
- [ ] Тесты: соседи внутри главы, через границу главы (Сир 2:1 → 1:30), через границу книги, первый и последний стих → null. commit `feat: verse navigator`.

### Task 6: Золотая пилюля в плашке главы

**Files:** `ui/common/GoldPill.kt`, `ui/reader/ReaderScreen.kt`.
- [ ] `GoldPill(text)` — прописные, разрядка 0.08em, фон/цвет из констант. Плашка главы: по центру `GoldPill("${book.shortTitle} ${chapter}")`. Существующий тест плашки (`chapter-bar`, текст главы) остаётся зелёным (поиск без учёта регистра). commit `feat: gold pill in chapter bar`.

### Task 7: Экран «Стих»

**Files:** `ui/verse/{VerseScreen.kt,VerseHeader.kt,VerseTabs.kt,VerseBottomBar.kt,VerseViewModel.kt}`, `res/font/ponomar.ttf`, `res/raw/ofl_ponomar.txt`, тест `VerseScreenTest.kt`.

**Produces:** `@Composable fun VerseScreen(db: BibleDatabase, container: AppContainer, startVerseId: Int, onBack: (verseId: Int) -> Unit, onOpenRef: (verseId: Int) -> Unit)`.
- [ ] Тесты (RED→GREEN): стих и золотая пилюля «СИРАХ 2:1» видны; подписи «ПРЕДЫДУЩИЙ 1:30» и «СЛЕДУЮЩИЙ 2:2»; нажатие на «СЛЕДУЮЩИЙ» → стих 2:2; первый стих Библии — нет «ПРЕДЫДУЩИЙ»; вкладки: пустая скрыта; нижняя плашка появляется после прокрутки; Пс 118:176 при fontScale 2: границы плашки не пересекают кнопки шапки; пакет отсутствует → карточка «Скачать»; стих без объяснения → «ещё готовится», «Переводы» доступны.
- [ ] Реализация по спецификации: HorizontalPager (ключ — verse id), шапка (ArtImage + Haze + `GoldPill` + D1-подписи), «Кратко», вкладки `ScrollableTabRow`-подобные с золотым подчёркиванием (sticky через `LazyColumn` `stickyHeader`), пункты, карточки ссылок, свёрнутые цитаты, нижняя плашка (как плашка главы), «Поделиться» (`Intent.ACTION_SEND`). commit `feat: verse explanation screen`.

### Task 8: Переходы

**Files:** `ui/AppRoot.kt`, `ui/reader/ChapterPage.kt`, `ReaderScreen.kt`, тест `VerseNavigationTest.kt`.
- [ ] Нажатие на стих в чтении → маршрут `verse/{id}`; вкладки скрыты на этом маршруте. «Назад» → глава на том же стихе (позиция сохраняется через `onPositionSettled`). Нажатие на карточку ссылки → чтение на этом месте. Тест: открыть Сир 2:1, перейти к 2:2, «Назад» → чтение Сирах 2, видим стих 2. commit `feat: open verse page from reader`.

### Task 9: Публикация пилота и проверка

- [x] База пилота собирается вместе с приложением (`generateCommentaryDb`).
- [ ] Release-сборка, установка на эмулятор, открыть Сир 2:1 — снимки экрана; полный прогон тестов. Обновить Baseline Profile (сценарий: открыть стих).
