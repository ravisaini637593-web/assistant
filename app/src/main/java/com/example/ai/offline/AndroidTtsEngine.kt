package com.example.ai.offline

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AndroidTtsEngine(
    context: Context,
    private val onInitResult: (Boolean, Int) -> Unit = { _, _ -> }
) {

    private var tts: TextToSpeech? = null
    var isInitialized: Boolean = false
        private set

    var speechRate: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    var pitch: Float = 1.0f
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    var isSpeaking: Boolean = false
        private set

    private var onSpeakingStateChanged: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(speechRate)
                tts?.setPitch(pitch)
                isInitialized = true
                val voiceCount = tts?.voices?.size ?: 0

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                        onSpeakingStateChanged?.invoke(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        onSpeakingStateChanged?.invoke(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        onSpeakingStateChanged?.invoke(false)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        isSpeaking = false
                        onSpeakingStateChanged?.invoke(false)
                    }
                })

                onInitResult(true, voiceCount)
            } else {
                isInitialized = false
                onInitResult(false, 0)
            }
        }
    }

    fun setSpeakingListener(listener: (Boolean) -> Unit) {
        onSpeakingStateChanged = listener
    }

    fun speak(text: String, utteranceId: String = System.currentTimeMillis().toString()) {
        if (!isInitialized || text.isBlank()) return
        stop() // Barge-in / stop prior utterance
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        if (isSpeaking) {
            tts?.stop()
            isSpeaking = false
            onSpeakingStateChanged?.invoke(false)
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    fun getInstalledVoicesCount(): Int {
        return tts?.voices?.size ?: 0
    }
}
