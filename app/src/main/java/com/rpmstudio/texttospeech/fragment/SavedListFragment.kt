package com.rpmstudio.texttospeech.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.appodeal.ads.Appodeal
import com.appodeal.ads.NativeAd
import com.rpmstudio.texttospeech.adapter.SavedAdapter
import com.rpmstudio.texttospeech.interfaces.SavedListItem
import com.rpmstudio.texttospeech.databinding.FragmentSavedListBinding
import com.rpmstudio.texttospeech.interfaces.OnFileDeletedListener

class SavedListFragment : Fragment(), OnFileDeletedListener {
    companion object {
        private const val TAG = "SavedListFragment"

        private const val STEPS = 5
    }

    private var _binding: FragmentSavedListBinding? = null
    private val binding get() = _binding!!

    private lateinit var savedAdapter: SavedAdapter
    private lateinit var savedTextFiles: MutableList<Pair<String, String>>

    private val getNativeAd: () -> NativeAd? = { Appodeal.getNativeAds(1).firstOrNull() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        savedAdapter = SavedAdapter(childFragmentManager)

        savedTextFiles = loadSavedTextFiles().filterIsInstance<SavedListItem.SavedTextItemSaved>()
            .map { Pair(it.filename, it.text) }.toMutableList()

        val items: MutableList<SavedListItem> =
            savedTextFiles.foldIndexed(mutableListOf()) { index, acc, item ->
                acc.apply {
                    add(SavedListItem.SavedTextItemSaved(item.first, item.second))
                    if (index % STEPS == 0 && index != 0) {
                        add(createAdItem())
                    }
                }
            }
        savedAdapter.submitList(items)

        binding.rvTtsStorageList.apply {
            layoutManager = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> GridLayoutManager(requireContext(), 2)
                else -> LinearLayoutManager(requireContext())
            }
            adapter = savedAdapter
        }

        if (savedTextFiles.isEmpty()) {
            binding.emptyListBlock.visibility = View.VISIBLE
        } else {
            binding.emptyListBlock.visibility = View.GONE
        }

    }

    private fun createAdItem(): SavedListItem.DynamicNativeAdItemSaved =
        SavedListItem.DynamicNativeAdItemSaved(getNativeAd = getNativeAd)


    private fun loadSavedTextFiles(): List<SavedListItem> {
        val textFiles = mutableListOf<Pair<String, String>>()
        val filesDir = requireContext().filesDir
        val files = filesDir.listFiles()

        files?.forEach { file ->
            if (file.isFile && file.name.startsWith("TTS_")) {
                try {
                    val text = file.readText()
                    textFiles.add(Pair(file.name, text)) // Store filename and content
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading file: ${e.message}")
                }
            }
        }
        Log.d(TAG, "Loaded files: $textFiles") // Add this log

        val itemsWithAds =
            textFiles.foldIndexed(mutableListOf<SavedListItem>()) { index, acc, item ->
                acc.apply {
                    add(SavedListItem.SavedTextItemSaved(item.first, item.second))
                    if (index % STEPS == 0 && index != 0) {
                        add(createAdItem())
                    }
                }
            }
        return itemsWithAds
    }

    override fun onFileDeleted(filename: String) {
        Handler(Looper.getMainLooper()).post {
            val positionToDelete = savedTextFiles.indexOfFirst { it.first == filename }
            if (positionToDelete != -1) {
                savedTextFiles.removeAt(positionToDelete)
                savedAdapter.notifyItemRemoved(positionToDelete)

                savedTextFiles =
                    loadSavedTextFiles().filterIsInstance<SavedListItem.SavedTextItemSaved>() // Filter for SavedTextItem
                        .map { Pair(it.filename, it.text) } // Map to Pair<String, String>
                        .toMutableList()

                binding.emptyListBlock.visibility =
                    if (savedTextFiles.isEmpty()) View.VISIBLE else View.GONE
                Log.d(TAG, "onFileDeleted: ${binding.emptyListBlock.visibility}")
            }
        }
    }
}