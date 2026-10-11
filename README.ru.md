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

`Dialog` — отдельное окно. `Modifier.blur()` внутри него размывает сам диалог, а не активити под
ним. Библиотеки вроде haze умеют переносить через эту границу помеченное Compose-дерево; Underlay
нужен, когда под оверлеем View или чужой код, или сам оверлей — View-`Dialog` или `PopupWindow`.
Он работает с окнами: `FLAG_BLUR_BEHIND` на API 31+, снимок через `PixelCopy` ниже. Задача в
трекере платформы: [Support window blur in compose dialogs](https://issuetracker.google.com/issues/296272625).

Подробности и компромиссы: [статья](docs/article-window-boundary.ru.md) и
[как это устроено](docs/how-it-works.ru.md).

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

Энергосбережение выключает межоконное размытие; открытый оверлей тогда переходит на `Snapshot`.
На некоторых устройствах, например на Samsung Galaxy, его нет вовсе, и все оверлеи используют `Snapshot`.

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

`Snapshot` снимается при открытии оверлея, при смене размера host-окна, при смене конфигурации (только
в Compose; с View вызывайте `refresh()`) и по `refresh()`. `liveSnapshot = true` снимает host-окно при каждой его отрисовке, с отставанием в пару
кадров, ценой `PixelCopy` и размытия на каждый кадр.

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

Подложка становится фоном View, радиус задаётся в пикселях. Возвращается `ViewUnderlay` с `source`,
`refresh()`, сеттерами и `remove()`, который возвращает прежний фон. Вызывайте на своей View, а не на
`decorView` или шторке `BottomSheetDialog`.

## Совместимость

Публичный API записан в [`underlay/api/underlay.api`](underlay/api/underlay.api) и проверяется при
каждой сборке. Патч-релиз ничего не ломает; до 1.0.0 минорный релиз может, и changelog об этом
пишет. Kotlin манглит сигнатуры с `Dp` и `Color`, поэтому новый параметр может потребовать
пересборки, даже если код компилируется.

## Сэмпл

По оверлею на каждый вид окна поверх сетки hardware bitmap, с активным источником и слайдером
радиуса:

```bash
./gradlew :sample:installDebug
```

## Как поучаствовать

[CONTRIBUTING.md](CONTRIBUTING.md). Самый полезный отчёт — какой источник выбрался на вашем
устройстве. Вклад покрывается [CLA](CLA.md).

## Лицензия

Apache 2.0, см. [LICENSE](LICENSE). Уязвимости — через [SECURITY.md](SECURITY.md).
