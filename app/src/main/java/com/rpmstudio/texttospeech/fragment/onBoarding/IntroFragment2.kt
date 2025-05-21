package com.rpmstudio.texttospeech.fragment.onBoarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rpmstudio.texttospeech.databinding.FragmentIntro2Binding

class IntroFragment2 : Fragment() {
    private var _binding: FragmentIntro2Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntro2Binding.inflate(inflater, container, false)
        return binding.root
    }
}