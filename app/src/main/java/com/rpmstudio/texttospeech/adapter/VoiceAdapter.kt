package com.rpmstudio.texttospeech.adapter

import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.rpmstudio.texttospeech.data.Voice
import com.rpmstudio.texttospeech.databinding.ItemVoiceBinding
import com.rpmstudio.texttospeech.interfaces.OnVoiceSelectedListener

class VoiceAdapter(
    private var voices: List<Voice>,
    private val listener: OnVoiceSelectedListener,
    private val textToSpeech: TextToSpeech,
    private val displayNames: List<String>
) : RecyclerView.Adapter<VoiceAdapter.VoiceViewHolder>() {

    class VoiceViewHolder(binding: ItemVoiceBinding) : RecyclerView.ViewHolder(binding.root) {
        val chipVoice: Chip = binding.chipVoice
    }

    var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoiceViewHolder {
        val binding = ItemVoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VoiceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoiceViewHolder, position: Int) {
        holder.chipVoice.text = displayNames[position]
        holder.chipVoice.isChecked = position == selectedPosition

        holder.chipVoice.setOnClickListener {
            speakTrialPhraseWithVoice(position)

            val previousPosition = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            Log.d("VoiceAdapter", "chipVoice selected: $selectedPosition")

            if (previousPosition != -1) {
                notifyItemChanged(previousPosition)
            }
            listener.onVoiceSelected(voices[selectedPosition])
        }
    }

    override fun getItemCount(): Int = voices.size

    fun speakTrialPhraseWithVoice(position: Int) {
        Log.d("VoiceAdapter", "Available voices: ${textToSpeech.voices.joinToString { it.name }}")
        textToSpeech.stop()
        val voice = voices[position]
        val selectedVoice = textToSpeech.voices.find { it.name == voice.id }

        selectedVoice?.let {
            textToSpeech.voice = it
            textToSpeech.speak("4321", TextToSpeech.QUEUE_FLUSH, null, null)
        } ?: run {
            Log.w("VoiceAdapter", "No matching voice found for ${voice.id}")
            // Consider displaying a message to the user or trying a fallback voice
        }
    }
}


