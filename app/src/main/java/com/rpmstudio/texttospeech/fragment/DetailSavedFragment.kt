package com.rpmstudio.texttospeech.fragment

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rpmstudio.texttospeech.R
import com.rpmstudio.texttospeech.data.LanguageData
import com.rpmstudio.texttospeech.data.UserPreferences
import com.rpmstudio.texttospeech.databinding.FragmentDetailSavedBinding
import com.rpmstudio.texttospeech.interfaces.OnFileDeletedListener
import com.rpmstudio.texttospeech.utils.showErrorToast
import com.rpmstudio.texttospeech.utils.showSuccessToast
import java.io.File
import java.io.IOException

class DetailSavedFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "DetailSavedFragment"
        private const val ARG_FILENAME = "filename"
        private const val ARG_SAVED_TEXT = "text"
    }

    private var onFileDeletedListener: OnFileDeletedListener? = null

    fun setOnFileDeletedListener(listener: OnFileDeletedListener) {
        this.onFileDeletedListener = listener
    }

    private var _binding: FragmentDetailSavedBinding? = null
    private val binding get() = _binding!!

    private lateinit var textToSpeech: TextToSpeech

    private var filename: String = ""
    private var savedText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            filename = it.getString(ARG_FILENAME) ?: ""
            savedText = it.getString(ARG_SAVED_TEXT) ?: ""
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_FILENAME, filename)
        outState.putString(ARG_SAVED_TEXT, savedText)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvFileName.text = filename
        binding.tvSavedText.text = savedText

        setupTextToSpeech()
        setupPlayButton()
        setupDownloadButton()
        setupShareButton()
        setupDeleteButton()
    }

    private fun setupTextToSpeech() {
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                loadSavedPreferences()

                Handler(requireContext().mainLooper).postDelayed({
                    LanguageData.applyUserPreferences()
                }, 100)

                binding.ivPlay.isEnabled = true
            } else {
                binding.ivPlay.isEnabled = false
            }
        }
    }

    private fun loadSavedPreferences() {
        val (selectedLanguageTag, selectedVoiceId, selectedFlagUrl) = UserPreferences.getSelectedLanguageAndVoice(
            requireContext()
        )

        if (selectedLanguageTag != null && selectedVoiceId != null && selectedFlagUrl != null) {
            val selectedVoice = textToSpeech.voices.find { it.name == selectedVoiceId }

            selectedVoice?.let {
                textToSpeech.voice = it
                textToSpeech.setPitch(UserPreferences.getPitch(requireContext()))
                textToSpeech.setSpeechRate(UserPreferences.getSpeed(requireContext()))
            }
        }
    }

    private fun initTextToSpeechListener() {
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}

            override fun onDone(utteranceId: String) {
                Handler(Looper.getMainLooper()).post {
                    binding.animationPlay.visibility = View.INVISIBLE
                    binding.animationPlay.cancelAnimation()
                    binding.ivPlay.visibility = View.VISIBLE
                    binding.ivPlay.isEnabled = true
                    binding.tvPlay.text = getString(R.string.play)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
            }
        })
    }

    private fun setupPlayButton() {
        binding.playBlock.setOnClickListener {
            if (!textToSpeech.isSpeaking) {
                binding.ivPlay.visibility = View.INVISIBLE
                binding.ivPlay.isEnabled = false
                binding.animationPlay.visibility = View.VISIBLE
                binding.animationPlay.playAnimation()
                binding.tvPlay.text = getString(R.string.stop)

                Handler(requireContext().mainLooper).postDelayed({
                    textToSpeech.speak(savedText, TextToSpeech.QUEUE_FLUSH, null, savedText)
                }, 100)

                initTextToSpeechListener()

            } else if (textToSpeech.isSpeaking) {

                binding.ivPlay.isEnabled = false
                Handler(requireContext().mainLooper).postDelayed({
                    textToSpeech.stop()
                    binding.animationPlay.visibility = View.INVISIBLE
                    binding.animationPlay.cancelAnimation()
                    binding.ivPlay.visibility = View.VISIBLE
                    binding.ivPlay.isEnabled = true
                    binding.tvPlay.text = getString(R.string.play)
                }, 100)

                binding.ivPlay.isEnabled = true
            }
        }
    }

    private fun setupDownloadButton() {
        binding.downloadBlock.setOnClickListener {
            if (textToSpeech.isSpeaking) {
                textToSpeech.stop()
                binding.animationPlay.visibility = View.INVISIBLE
                binding.animationPlay.cancelAnimation()
                binding.ivPlay.visibility = View.VISIBLE
                binding.ivPlay.isEnabled = true
                binding.tvPlay.text = getString(R.string.play)
            }

            val subfolderName = "TTSFiles"
            val downloadDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val subfolderPath = File(downloadDir, subfolderName)
            val file = File(subfolderPath, "$filename.mp3")

            if (file.exists()) {
                // File already exists, show warning dialog
                showOverwriteDialog()
            } else {
                if (checkStoragePermission()) {
                    downloadTTSFile()
                } else {
                    requestStoragePermission()
                }
            }
        }
    }

    private fun showOverwriteDialog() {
        MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.file_already_exists))
            .setMessage(getString(R.string.continue_downloading_anyway))
            .setPositiveButton(getString(R.string.continue_)) { dialog, _ ->
                if (checkStoragePermission()) {
                    downloadTTSFile()
                } else {
                    requestStoragePermission()
                }
                dialog.dismiss()
            }.setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun downloadTTSFile(onDownloadComplete: () -> Unit = {}) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.ivDownload.visibility = View.INVISIBLE
            binding.ivDownload.isEnabled = false
            binding.animationDownload.visibility = View.VISIBLE
            binding.animationDownload.playAnimation()
            binding.tvDownload.text = getString(R.string.downloading)

            Handler(requireContext().mainLooper).postDelayed({
                downloadTTSFileMediaStore(onDownloadComplete)
            }, 100)

        } else {
            binding.ivDownload.visibility = View.INVISIBLE
            binding.ivDownload.isEnabled = false
            binding.animationDownload.visibility = View.VISIBLE
            binding.animationDownload.playAnimation()
            binding.tvDownload.text = getString(R.string.downloading)

            Handler(requireContext().mainLooper).postDelayed({
                downloadTTSFileLegacy(onDownloadComplete)
            }, 100)

        }
    }

    private fun downloadTTSFileMediaStore(onDownloadComplete: () -> Unit = {}) {
        val tempFile = File.createTempFile("tts_temp", ".mp3", requireContext().cacheDir)
        val utteranceId = "tts_download_${System.currentTimeMillis()}"

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                Log.d(TAG, "TTS download started")
            }

            override fun onDone(utteranceId: String) {
                Handler(Looper.getMainLooper()).post {
                    binding.animationDownload.visibility = View.INVISIBLE
                    binding.animationDownload.cancelAnimation()
                    binding.ivDownload.visibility = View.VISIBLE
                    binding.ivDownload.isEnabled = true
                    binding.tvDownload.text = getString(R.string.download)

                    addToMediaStore(tempFile, filename)
                    onDownloadComplete()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                Handler(Looper.getMainLooper()).post {
                    requireContext().showErrorToast(
                        TAG, getString(R.string.failed_to_download_file)
                    )
                }
            }
        })

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

        // Synthesize to temp file
        textToSpeech.synthesizeToFile(
            savedText, params, tempFile, utteranceId
        )
    }

    private fun addToMediaStore(tempFile: File, displayName: String) {
        val resolver = requireContext().contentResolver
        val subfolderName = "TTSFiles"

        val subfolderValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, subfolderName)
            put(MediaStore.MediaColumns.MIME_TYPE, "vnd.android.document/directory")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val subfolderUri = try {
            Log.d(TAG, "Attempting to insert subfolder")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, subfolderValues)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting subfolder", e)
            null
        }

        if (subfolderUri != null) {
            val fileValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$displayName.mp3")
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/$subfolderName"
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val audioUri = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, fileValues)
                } else {
                    resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, fileValues)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting file", e)
                null
            }

            if (audioUri != null) {
                try {
                    resolver.openOutputStream(audioUri)?.use { outputStream ->
                        tempFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        fileValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(audioUri, fileValues, null, null)

                        subfolderValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(subfolderUri, subfolderValues, null, null)
                    }

                    val filePathColumn = arrayOf(MediaStore.MediaColumns.DATA)
                    resolver.query(audioUri, filePathColumn, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val filePath =
                                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                            Log.d(TAG, "Filepath in MediaStore: $filePath")
                        }
                    }
                    requireContext().showSuccessToast(
                        TAG, getString(R.string.file_downloaded_to_downloads_folder, subfolderName)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding file to MediaStore", e)
                    requireContext().showErrorToast(
                        TAG, getString(R.string.failed_to_download_file)
                    )

                } finally {
                    tempFile.delete()
                }
            } else {
                requireContext().showErrorToast(TAG, getString(R.string.error_creating_file_entry))
            }
        } else {
            requireContext().showErrorToast(TAG, getString(R.string.error_creating_subfolder))
        }
    }

    private fun downloadTTSFileLegacy(onDownloadComplete: () -> Unit = {}) {
        val subfolderName = "TTSFiles"
        val downloadDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val subfolderPath = File(downloadDir, subfolderName)

        if (!subfolderPath.exists()) {
            subfolderPath.mkdirs()
        }

        val outputFile = File(subfolderPath, "$filename.mp3")
        val utteranceId = "tts_download_${System.currentTimeMillis()}"


        try {
            if (!outputFile.exists()) {
                outputFile.createNewFile()
            }

            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {
                    requireContext().showSuccessToast(
                        TAG, getString(R.string.file_download_started)
                    )
                }

                override fun onDone(utteranceId: String) {
                    requireActivity().runOnUiThread {
                        binding.animationDownload.visibility = View.INVISIBLE
                        binding.animationDownload.cancelAnimation()
                        binding.ivDownload.visibility = View.VISIBLE
                        binding.ivDownload.isEnabled = true
                        binding.tvDownload.text = getString(R.string.download)

                        requireContext().showSuccessToast(
                            TAG,
                            getString(R.string.file_downloaded_to_downloads_folder, subfolderName)
                        )
                        onDownloadComplete()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String) {
                    requireActivity().runOnUiThread {
                        requireContext().showErrorToast(
                            TAG, getString(R.string.failed_to_download_file)
                        )
                    }
                }
            })

            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

            textToSpeech.synthesizeToFile(
                savedText, params, outputFile, utteranceId
            )
        } catch (e: IOException) {
            Log.e(TAG, "Error creating or writing to file: ${e.message}")
            requireActivity().runOnUiThread {
                requireContext().showErrorToast(TAG, getString(R.string.failed_to_download_file))
            }
        }
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        requestPermissionLauncher.launch(permission)
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            downloadTTSFile()
        } else {
            requireContext().showErrorToast(
                TAG, getString(R.string.storage_permission_required_to_download)
            )
        }
    }

    private fun setupShareButton() {
        binding.sendBlock.setOnClickListener {
            if (textToSpeech.isSpeaking) {
                textToSpeech.stop()
                binding.animationPlay.visibility = View.INVISIBLE
                binding.animationPlay.cancelAnimation()
                binding.ivPlay.visibility = View.VISIBLE
                binding.ivPlay.isEnabled = true
                binding.tvPlay.text = getString(R.string.play)
            }

            val subfolderName = "TTSFiles"
            val downloadDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val subfolderPath = File(downloadDir, subfolderName)
            val file = File(subfolderPath, "$filename.mp3")

            if (file.exists()) {
                shareAudioFile(file)
            } else {
                showDownloadDialog()
            }
        }
    }

    private fun shareAudioFile(file: File) {
        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().applicationContext.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mpeg"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_audio_file)))
    }

    private fun showDownloadDialog() {
        MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.share_audio_file))
            .setMessage(getString(R.string.audio_file_not_found_download_it_first))
            .setPositiveButton(getString(R.string.download)) { dialog, _ ->
                // Start download process
                if (checkStoragePermission()) {
                    downloadTTSFile {
                        val subfolderName = "TTSFiles"
                        val downloadDir =
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val subfolderPath = File(downloadDir, subfolderName)
                        val file = File(subfolderPath, "$filename.mp3")
                        shareAudioFile(file)
                    }
                } else {
                    requestStoragePermission()
                }
                dialog.dismiss()
            }.setNegativeButton(getString(android.R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun setupDeleteButton() {
        binding.deleteBlock.setOnClickListener {
            if (textToSpeech.isSpeaking) {
                textToSpeech.stop()
                binding.animationPlay.visibility = View.INVISIBLE
                binding.animationPlay.cancelAnimation()
                binding.ivPlay.visibility = View.VISIBLE
                binding.ivPlay.isEnabled = true
                binding.tvPlay.text = getString(R.string.play)
            }

            MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.delete_title_dialog))
                .setIcon(R.drawable.trash_bin_trash_svgrepo_com)
                .setMessage(getString(R.string.message_dialog_delete))
                .setPositiveButton(getString(R.string.positive_button_dialog_delete)) { dialog, _ ->
                    val fileToDelete = File(requireContext().filesDir, filename)
                    if (fileToDelete.delete()) {
                        requireContext().showSuccessToast(
                            TAG, getString(R.string.file_deleted_successfully)
                        )
                        onFileDeletedListener?.onFileDeleted(filename)
                        dismiss()
                    } else {
                        requireContext().showErrorToast(
                            TAG, getString(R.string.failed_to_delete_file)
                        )
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.negative_button_dialog_delete)) { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
    }

    override fun onPause() {
        super.onPause()
        textToSpeech.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}