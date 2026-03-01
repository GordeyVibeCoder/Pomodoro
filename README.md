# Pomodoro + Lissajous (Android)

Приложение Pomodoro на Jetpack Compose с визуализацией прогресса через фигуры Лиссажу.

## Важно про Gradle Wrapper

В этом репозитории **не хранится** `gradle/wrapper/gradle-wrapper.jar`, потому что целевая платформа PR не поддерживает бинарные файлы.

После клонирования сгенерируйте wrapper локально:

```bash
gradle wrapper --gradle-version 8.14.3
```

Либо откройте проект в Android Studio и выполните синхронизацию — IDE предложит восстановить wrapper.
