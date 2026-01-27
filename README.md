# Day 30: Personal AI Agent

This project implements a personalized AI assistant as part of the **AI Advent Challenge**. The agent runs as a Kotlin console application and uses the **Yandex Cloud LLM API**.

## Features

- **Personalized Context**: The agent loads a user configuration from `user_config.json`, which includes:
    - **User Profile**: Role, experience level, and tone of voice.
    - **Technical Stack**: Preferred languages (Kotlin, Java), frameworks (Android ViewModel, Ktor), and tools.
    - **Interests**: Specific professional and personal interests to tailor conversations.
- **Dynamic System Prompt**: The system prompt is generated at runtime based on the loaded configuration, ensuring the AI behaves as a "Personal Mentor".
- **Secure Configuration**: API keys and Folder IDs are loaded from `local.properties` (not committed) or environment variables.
- **Universal Client Architecture**: Uses a flexible `UniversalGptClient` (adapted from Day 10) built with Ktor (CIO engine) and Kotlinx Serialization.

## Setup

1.  **Clone the repository** and switch to the `day30` branch.
2.  **Configure API Credentials**:
    Create a `local.properties` file in the root directory:
    ```properties
    YANDEX_API_KEY=your_api_key
    YANDEX_FOLDER_ID=your_folder_id
    ```
    Alternatively, set environment variables: `YANDEX_API_KEY` and `YANDEX_FOLDER_ID`.
3.  **Run the Application**:
    ```bash
    ./gradlew run
    ```

## Project Structure

- `src/main/resources/user_config.json`: The source of truth for the agent's personality.
- `src/main/kotlin/config/ApiConfig.kt`: Handles secure credential loading.
- `src/main/kotlin/client/UniversalGptClient.kt`: Network layer for interacting with YandexGPT.
- `src/main/kotlin/model/`: Data classes for configuration and API models.
- `src/main/kotlin/Main.kt`: Entry point; initializes the config, builds the prompt, and runs the REPL loop.

## Example Interaction

```
Loaded configuration for user: Vera

--- Personal AI Agent Started ---
System Prompt initialized with user profile context.
Type 'exit' to quit.

> What are my main interests?
AI: Based on your profile, your interests include:
- Kotlin and Android Development
- AI/ML Engineering (RAG, MCP, Local Models)
- Aerial silks training
- Music visualizations
```

## Learnings
- **Personalization via Configuration**: Moving the "identity" of the agent into a JSON file makes the system modular and easy to update without code changes.
- **Secure Secrets Management**: Adopted a `local.properties` approach standard in Android development for keeping keys safe.
- **Prompt Engineering**: Injecting structured data (stack, interests) directly into the system prompt significantly improves the relevance of the AI's responses.
