package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Stable
class ArabicVoiceController internal constructor(
    val isSupported: Boolean,
    private val onStartRequested: () -> Unit,
    private val onStopRequested: () -> Unit
) {
    var isListening by mutableStateOf(false)
        internal set
    var partialTranscript by mutableStateOf("")
        internal set
    var statusMessage by mutableStateOf<String?>(null)
        internal set

    fun toggleListening() {
        if (isListening) {
            onStopRequested()
        } else {
            onStartRequested()
        }
    }

    fun stopListening() {
        if (isListening) {
            onStopRequested()
        }
    }

    fun clearMessage() {
        statusMessage = null
    }
}

@Composable
fun rememberArabicVoiceController(
    onPartialText: (String) -> Unit,
    onFinalResult: (String) -> Unit,
    onVoiceError: (String) -> Unit = {}
): ArabicVoiceController {
    val context = LocalContext.current
    val latestOnPartial by rememberUpdatedState(onPartialText)
    val latestOnFinal by rememberUpdatedState(onFinalResult)
    val latestOnError by rememberUpdatedState(onVoiceError)

    val recognitionAvailable = remember(context) {
        runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)
    }

    val recognizer = remember(context, recognitionAvailable) {
        if (recognitionAvailable) {
            runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        } else {
            null
        }
    }

    lateinit var controller: ArabicVoiceController

    fun beginSpeechCapture() {
        val sr = recognizer
        if (sr == null) {
            val msg = "التعرف الصوتي غير متوفر على هذا الجهاز حالياً — يمكنك الكتابة يدوياً."
            controller.statusMessage = msg
            latestOnError(msg)
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
        }
        controller.partialTranscript = ""
        controller.statusMessage = "جاري تجهيز المايك للاستماع..."
        controller.isListening = true
        runCatching {
            sr.startListening(intent)
        }.onFailure {
            controller.isListening = false
            val msg = "تعذر تشغيل المايك للتعرف الصوتي."
            controller.statusMessage = msg
            latestOnError(msg)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginSpeechCapture()
        } else {
            val msg = "يرجى السماح بإذن المايكروفون لتتمكن من التسميع الصوتي."
            controller.statusMessage = msg
            latestOnError(msg)
        }
    }

    controller = remember(recognizer) {
        ArabicVoiceController(
            isSupported = recognitionAvailable && recognizer != null,
            onStartRequested = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    beginSpeechCapture()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onStopRequested = {
                controller.isListening = false
                runCatching { recognizer?.stopListening() }
            }
        )
    }

    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                controller.isListening = true
                controller.statusMessage = "تحدث الآن بالآية المطلوبة..."
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                controller.isListening = false
                controller.statusMessage = null
            }

            override fun onError(error: Int) {
                controller.isListening = false
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "تعذر التقاط الصوت من المايك."
                    SpeechRecognizer.ERROR_CLIENT -> "تم إيقاف التسجيل الصوتي."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يلزم تفعيل إذن المايكروفون أولاً."
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "التعرف الصوتي يحتاج اتصالاً بالإنترنت."
                    SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التقاط كلمات واضحة — حاول القراءة بصوت أوضح."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "خدمة الصوت مشغولة حالياً — أعد المحاولة."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يُسمع صوت خلال المهلة — اضغط المايك وجرب ثانية."
                    else -> "تعذر التعرف على الصوت (رمز $error)."
                }
                controller.statusMessage = msg
                latestOnError(msg)
            }

            override fun onResults(results: Bundle?) {
                controller.isListening = false
                controller.statusMessage = null
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val best = matches.firstOrNull { it.isNotBlank() }.orEmpty()
                if (best.isNotBlank()) {
                    controller.partialTranscript = best
                    latestOnFinal(best)
                } else {
                    val msg = "لم يتم التقاط صوت واضح — حاول مرة أخرى."
                    controller.statusMessage = msg
                    latestOnError(msg)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull { it.isNotBlank() }
                    .orEmpty()
                if (partial.isNotBlank()) {
                    controller.partialTranscript = partial
                    latestOnPartial(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        onDispose {
            runCatching { recognizer?.destroy() }
        }
    }

    return controller
}
