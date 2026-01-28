package core.domain.model

enum class ToolType {
    CLOUD_LLM, // Yandex GPT
    LOCAL_LLM, // Local Llama/Mistral
    RAG,       // Documentation Search
    MCP        // External Tools
}
