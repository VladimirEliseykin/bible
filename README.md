# Библия

Офлайн-приложение для чтения Библии на Android: Синодальный перевод, 77 книг в православном порядке, акварельные арты, удобное чтение без интернета.

## Возможности версии 0.1

- Главная с каруселью артов и «Продолжить чтение».
- Оглавление «полки с обложками», выбор главы.
- Чтение: арт главы, номера стихов, разделители, свайп между главами, размер шрифта и интервал.
- Место чтения сохраняется и восстанавливается.

## Сборка

Нужны JDK 21 и Android SDK (compileSdk 36).

```bash
./gradlew :app:assembleDebug
```

База `bible.db` собирается автоматически из `tools/dbgen/source/rst78` (USFM) при каждой сборке.

Тесты: `./gradlew :tools:dbgen:test :app:testDebugUnitTest :app:connectedDebugAndroidTest`.

## Источники и лицензии

- Текст: Синодальный перевод, общественное достояние, источник — [bibleonline/rst](https://github.com/bibleonline/rst).
- Шрифты Literata и Golos Text — SIL Open Font License (`app/src/main/res/raw/ofl_*.txt`).

Документация: спецификация и план в `docs/superpowers/`, арты — `docs/ART.md`.
