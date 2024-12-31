package com.example.transcriptionapp.model

data class TranscriptionResponse(val text: String)

data class SummarizationRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>
) {
    data class Message(val role: String, val content: String)
}

data class SummarizationResponse(
    val choices: List<Choice>
) {
    data class Choice(val message: Message) {
        data class Message(val content: String)
    }
}