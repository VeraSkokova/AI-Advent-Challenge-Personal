# AI Advent Challenge - Day 31: Voice Agent

Этот день посвящен созданию голосового интерфейса (Voice UI) для вашего AI-агента.
Мы используем **FFmpeg** для записи звука с микрофона и **Whisper.cpp** для локального распознавания речи (STT).

## Требования для Windows

### 1. FFmpeg
Утилита для работы с мультимедиа. Используется для захвата аудиопотока.
1. Скачайте сборку для Windows: [gyan.dev/ffmpeg/builds](https://www.gyan.dev/ffmpeg/builds/) (например, `ffmpeg-release-essentials.zip`).
2. Распакуйте архив.
3. Добавьте путь к папке `bin` (где лежит `ffmpeg.exe`) в переменную среды PATH **ИЛИ** пропишите полный путь в `local.properties`.
4. Проверьте установку: `ffmpeg -version`.

### 2. Whisper.cpp
Легковесная C++ реализация модели Whisper от OpenAI.
1. Скачайте релиз для Windows: [ggerganov/whisper.cpp/releases](https://github.com/ggerganov/whisper.cpp/releases) (архив `whisper-bin-x64.zip`).
2. Распакуйте архив. Внутри должен быть файл `whisper-cli.exe` (в старых версиях `main.exe`, но он deprecated).
3. Скачайте файл модели (например, `ggml-small.bin` или `ggml-base.bin`) отсюда: [Hugging Face](https://huggingface.co/ggerganov/whisper.cpp/tree/main).
4. Положите модель в удобное место.

### 3. Настройка local.properties
Создайте файл `local.properties` в корне проекта (если нет).
Добавьте туда настройки путей и вашего микрофона:

```properties
# Yandex Cloud (из предыдущих дней)
YANDEX_API_KEY=ваш_api_key
YANDEX_FOLDER_ID=ваш_folder_id

# Пути к инструментам (используйте двойные слэши для Windows путей)
ffmpeg.path=C:\\path\\to\\ffmpeg\\bin\\ffmpeg.exe
whisper.main.path=C:\\path\\to\\whisper\\whisper-cli.exe
whisper.model.path=C:\\path\\to\\whisper\\ggml-small.bin

# Ваше аудио-устройство
# Чтобы узнать имя, выполните: ffmpeg -list_devices true -f dshow -i dummy
# Скопируйте имя из секции "DirectShow audio devices"
audio.input.device=audio=Microphone Array (Технология Intel® Smart Sound для цифровых микрофонов)
# Или используйте alternative name, если есть проблемы с кодировкой:
# audio.input.device=audio=@device_cm_{...}
```

## Запуск

```powershell
./gradlew run
```

Приложение предложит нажать Enter для старта записи.
1. Нажмите Enter -> говорите.
2. Нажмите Enter еще раз -> запись остановится.
3. Подождите распознавания и ответа AI.
