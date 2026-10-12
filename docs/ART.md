# Арты

Стиль: светлая тёплая акварель, много неба и света, золото, беж, небесно-голубой; фигуры мелкие или со спины; без текста.
Формат: 4:5, 1080×1350, WebP качество 80 (~100–150 КБ).

Промпт-шаблон:

```
soft luminous watercolor, {сцена}, warm golden light, pale sky with lots of empty space in the upper half,
palette of ivory, sand, soft gold and sky blue, delicate wet-on-wet washes, small figure seen from behind or no figures,
no text, serene and contemplative, vertical 4:5
```

Куда класть:
- арт группы: `app/src/main/res/drawable-nodpi/art_group_<group>.webp` (и удалить одноимённый `.xml`-градиент в `drawable/`);
- арт отдельной книги (позже): `app/src/main/res/drawable-nodpi/art_book_<id>.webp` — подхватится автоматически (id: Бытие 1 … Матфей 51 … Откровение 77);
- карусель главной: список `homeCarouselArts` в `app/src/main/kotlin/bible/app/ui/art/Art.kt`.

Группы и сцены:

| Файл | Группа | Сцена |
|---|---|---|
| law | Пятикнижие | гора Синай на рассвете |
| history | Исторические | Иерусалим на холмах |
| wisdom | Учительные | арфа и свиток у оливы |
| prophets | Пророческие | фигура на скале перед небом |
| history_late | Маккавейские и 3 Ездры | стены и храм на закате |
| gospels | Евангелия | Галилейское море, лодка |
| acts | Деяния | дорога и паруса у побережья |
| catholic_epistles | Соборные послания | свеча и раскрытое письмо |
| paul | Послания Павла | римская дорога к городу |
| revelation | Откровение | небесный град в сиянии над облаками |

Картинки из Pinterest в `docs/mockups/art/` — только для макетов, в приложение их не класть.
