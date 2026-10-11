# Underlay

[![Maven Central](https://img.shields.io/maven-central/v/com.timkrest/underlay?label=Maven%20Central)](https://central.sonatype.com/artifact/com.timkrest/underlay)
[![CI](https://github.com/timkrest/Underlay/actions/workflows/ci.yml/badge.svg)](https://github.com/timkrest/Underlay/actions/workflows/ci.yml)

[English version](README.md)

Размытая подложка для диалогов и оверлеев, которые живут **в своём окне**, начиная с API 23.

| `Snapshot` под `Dialog` | `Fallback` в прозрачной активити |
|---|---|
| <img src="docs/snapshot.png" width="300" alt="Диалог поверх сетки плиток, подложка размыта по снимку окна активити"> | <img src="docs/fallback.png" width="300" alt="Прозрачная активити рисует сплошной цвет: захватывать нечего"> |

```kotlin
Dialog(onDismissRequest = ::dismiss) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .blurredUnderlay(
                blurRadius = 16.dp,
                tint = Color.Black.copy(alpha = 0.3f),
                fallback = MaterialTheme.colorScheme.surface,
            ),
    ) {
        // содержимое диалога
    }
}
```

## Зачем

`Dialog` — отдельное окно, и `Modifier.blur()` внутри него не достаёт до активити под ним ([задача в
трекере](https://issuetracker.google.com/issues/296272625)). Underlay размывает окно под ним:
`FLAG_BLUR_BEHIND` на API 31+, размытый снимок в остальных случаях. В отличие от haze, ему не нужно
помеченное Compose-дерево: под оверлеем могут быть View или чужой код, а сам оверлей может быть
View-`Dialog` или `PopupWindow`.

Подробности: [как это устроено](docs/how-it-works.ru.md),
[статья](docs/article-window-boundary.ru.md).

## Установка

```kotlin
dependencies {
    implementation("com.timkrest:underlay:0.4.0")
}
```

`minSdk 23`, байткод Java 11. Compose runtime и UI подключаются как `api`.

## Источники

| Источник | Когда | Что рисуется |
|----------|-------|--------------|
| `SystemBlur` | API 31+, своё окно, межоконное размытие включено | Система размывает всё за окном, сверху тинт |
| `Snapshot` | Есть host-окно, которое можно снять | Снимок host-окна, уменьшенный ×4 и размытый, под тинтом |
| `Pending` | Снимок готовится | Только тинт |
| `Fallback` | Нет своего окна, нет host-окна или снять не удалось | Цвет `fallback` |

Энергосбережение выключает межоконное размытие, и открытые оверлеи переходят на `Snapshot`. На
Samsung Galaxy и некоторых других устройствах его нет, там всегда `Snapshot`.

## Состояние и обновление

```kotlin
val underlay = rememberUnderlayState()

Box(
    modifier = Modifier.blurredUnderlay(
        blurRadius = 16.dp,
        tint = Color.Black.copy(alpha = 0.3f),
        fallback = MaterialTheme.colorScheme.surface,
        state = underlay,
        onSourceChange = { source -> Log.d("underlay", "$source") },
    ),
) {
    Text("${underlay.source}")
    Button(onClick = { underlay.refresh() }) { Text("Обновить") }
}
```

`Snapshot` переснимается, когда host-окно меняет размер, по `refresh()` и в Compose при смене
конфигурации (с View вызывайте `refresh()`). С `liveSnapshot = true` он переснимается на каждом
кадре host-окна с отставанием в пару кадров, ценой захвата и размытия на кадр.

## View

```kotlin
class FrostedDialogFragment : DialogFragment(R.layout.frosted_dialog) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.setDimAmount(0f)
        view.blurredUnderlay(
            blurRadius = 16f * resources.displayMetrics.density,
            tint = 0x4D000000,
            fallback = requireContext().getColor(R.color.surface),
        )
    }
}
```

Подложка становится фоном View, радиус в пикселях. У `ViewUnderlay` есть `source`, `refresh()`,
сеттеры и `remove()`, который возвращает старый фон. Вызывайте на своей View, а не на `decorView`
или шторке `BottomSheetDialog`.

## Ограничения

- Вешайте модификатор один раз на окно, на композабл во всё окно: `SystemBlur` покрывает всё окно,
  `Snapshot` — только композабл.
- Снимок всегда делается с окна активити, даже под диалогом, открытым поверх другого диалога.
- У прозрачной активити нет host-окна: без `SystemBlur` она рисует `fallback`. Оверлей внутри окна
  активити рисует его всегда; для него нужна библиотека размытия внутри окна.
- Host-окно с `FLAG_SECURE` снимается только в защищённое окно оверлея, иначе вместо `Snapshot`
  будет `Fallback`.
  Compose-`Dialog` и `Popup` по умолчанию наследуют этот флаг.

## Совместимость

Каждая сборка сверяет публичный API с [`underlay/api/underlay.api`](underlay/api/underlay.api).
Патч-релизы его не ломают; до 1.0.0 минорный релиз может, и changelog об этом пишет. Новый
параметр у функции с `Dp` или `Color` меняет её манглированное JVM-имя и требует пересборки.

## Сэмпл

По оверлею на каждый вид окна, с активным источником и слайдером радиуса:

```bash
./gradlew :sample:installDebug
```

## Как поучаствовать

[CONTRIBUTING.md](CONTRIBUTING.md). Самый полезный отчёт — какой источник выбрался на вашем
устройстве. Вклад покрывается [CLA](CLA.md).

## Лицензия

Apache 2.0, см. [LICENSE](LICENSE). Уязвимости — через [SECURITY.md](SECURITY.md).
