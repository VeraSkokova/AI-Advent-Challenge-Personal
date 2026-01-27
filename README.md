# День 31: Голосовой AI-агент

Реализация голосового интерфейса для персонализированного AI-ассистента в рамках **AI Advent Challenge**.
Приложение записывает голос с микрофона, переводит его в текст с помощью локальной модели Whisper и отправляет запрос в YandexGPT, используя профиль пользователя.

## Возможности
- **Push-to-talk**: Управление записью через клавишу Enter (старт/стоп).
- **Speech-to-Text (STT)**: Локальное распознавание речи через [whisper.cpp](https://github.com/ggerganov/whisper.cpp) (бесплатно, приватно, без интернета).
- **Персонализация**: Агент использует конфиг `user_config.json` (стек, интересы, стиль общения) для генерации системного промпта (наследие Day 30).
- **LLM**: Интеграция с Yandex Cloud LLM API.
- **Audio Recording**: Запись WAV-файла через FFmpeg (16kHz, mono).

## Требования для Windows

### 1. Программное обеспечение
1.  **FFmpeg**: 
    - Скачать `ffmpeg-release-essentials.zip` с [gyan.dev](https://www.gyan.dev/ffmpeg/builds/).
    - Распаковать, путь к `ffmpeg.exe` прописать в конфиге.
2.  **Whisper.cpp**:
    - Скачать `whisper-bin-x64.zip` из [релизов whisper.cpp](https://github.com/ggerganov/whisper.cpp/releases).
    - Используется файл `whisper-cli.exe`.
3.  **Модель Whisper**:
    - Скачать файл модели `.bin` (рекомендуется `ggml-small.bin` для русского языка) с [Hugging Face](https://huggingface.co/ggerganov/whisper.cpp/tree/main).

### 2. Конфигурация (local.properties)
Создайте файл `local.properties` в корне проекта. Укажите ваши ключи и пути:

```properties
# Yandex Cloud (Day 30+)
YANDEX_API_KEY=ваш_api_ключ
YANDEX_FOLDER_ID=ваш_folder_id

# Пути к инструментам (используйте двойные слэши \\ для Windows путей)
ffmpeg.path=C:\\Tools\\ffmpeg\\bin\\ffmpeg.exe
whisper.main.path=C:\\Tools\\whisper\\whisper-cli.exe
whisper.model.path=C:\\Tools\\whisper\\ggml-small.bin

# Ваше устройство записи
# Чтобы узнать имя: ffmpeg -list_devices true -f dshow -i dummy
# Копируйте имя из секции "DirectShow audio devices"
audio.input.device=audio=Microphone (Realtek(R) Audio)
```

## Запуск

В терминале (PowerShell/CMD):
```bash
./gradlew run
```

### Инструкция пользователя:
1. Запустите приложение.
2. Когда появится приглашение, нажмите **Enter**.
3. Дождитесь сообщения `>> ЗАПИСЬ ИДЕТ` и говорите.
4. Нажмите **Enter** еще раз, чтобы остановить запись.
5. Программа распознает речь и выведет ответ ассистента.
6. Введите `exit` вместо нажатия Enter, чтобы выйти.

## Структура проекта
- `src/main/resources/user_config.json`: Настройки личности агента и интересов.
- `src/main/kotlin/config/`:
    - `VoiceConfig.kt`: Настройки путей к FFmpeg/Whisper.
    - `ApiConfig.kt`: Настройки API ключей.
- `src/main/kotlin/service/`:
    - `AudioRecorder.kt`: Обертка над FFmpeg.
    - `SpeechService.kt`: Обертка над Whisper.
- `src/main/kotlin/client/UniversalGptClient.kt`: Клиент к YandexGPT.
- `src/main/kotlin/Main.kt`: Основной цикл (Voice REPL).
