package com.rpmstudio.texttospeech.activity

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.rpmstudio.texttospeech.R
import com.rpmstudio.texttospeech.databinding.DialogFeedbackBinding
import com.rpmstudio.texttospeech.databinding.RatingDialogLayoutBinding
import com.rpmstudio.texttospeech.utils.AppStoreHelper
import com.rpmstudio.texttospeech.utils.AppStoreHelper.GOOGLE_PLAY_PACKAGE_NAME

class RateDialog(private val activity: Activity) : Dialog(activity), View.OnClickListener {
    private var binding: RatingDialogLayoutBinding

    private val imageViewStars = arrayOfNulls<ImageView>(5)
    private var starNumber: Int

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = RatingDialogLayoutBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        setCancelable(false)
        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        initView()
        binding.linearLayoutRatingBar.startAnimation(
            AnimationUtils.loadAnimation(
                activity, com.codemybrainsout.ratingdialog.R.anim.shake
            )
        )
        starNumber = 0
    }

    private fun initView() {
        binding.dialogRatingIcon.setImageResource(R.drawable.hand_stars_svgrepo_com)
        binding.textViewRateTitle.visibility = View.GONE
        binding.btnSelectStar.setOnClickListener(this)
        binding.buttonCancel.setOnClickListener(this)

        binding.imageViewStar1.setOnClickListener(this)
        binding.imageViewStar2.setOnClickListener(this)
        binding.imageViewStar3.setOnClickListener(this)
        binding.imageViewStar4.setOnClickListener(this)
        binding.imageViewStar5.setOnClickListener(this)
        imageViewStars[0] = binding.imageViewStar1
        imageViewStars[1] = binding.imageViewStar2
        imageViewStars[2] = binding.imageViewStar3
        imageViewStars[3] = binding.imageViewStar4
        imageViewStars[4] = binding.imageViewStar5
    }

    override fun onClick(view: View) {
        val id = view.id
        if (id != R.id.btn_select_star) {
            when (id) {
                R.id.image_view_star_1 -> {
                    starNumber = 1
                    binding.dialogRatingIcon.setImageResource(R.drawable.emoji_surprise_svgrepo_com)
                    binding.textViewRateTitle.text =
                        context.resources.getString(R.string.rating_title_1)
                    binding.textViewRateTitle.visibility = View.VISIBLE
                    binding.textViewRate.text = context.resources.getString(R.string.rating_text_1)
                    setStarBar()
                }

                R.id.image_view_star_2 -> {
                    starNumber = 2
                    binding.dialogRatingIcon.setImageResource(R.drawable.emoji_sad_svgrepo_com)
                    binding.textViewRateTitle.text =
                        context.resources.getString(R.string.rating_title_1)
                    binding.textViewRateTitle.visibility = View.VISIBLE
                    binding.textViewRate.text = context.resources.getString(R.string.rating_text_1)
                    setStarBar()
                }

                R.id.image_view_star_3 -> {
                    starNumber = 3
                    binding.dialogRatingIcon.setImageResource(R.drawable.emoji_meh_svgrepo_com)
                    binding.textViewRateTitle.text =
                        context.resources.getString(R.string.rating_title_1)
                    binding.textViewRateTitle.visibility = View.VISIBLE
                    binding.textViewRate.text = context.resources.getString(R.string.rating_text_1)
                    setStarBar()
                }

                R.id.image_view_star_4 -> {
                    starNumber = 4
                    binding.dialogRatingIcon.setImageResource(R.drawable.emoji_smile_svgrepo_com)
                    binding.textViewRateTitle.text =
                        context.resources.getString(R.string.rating_title_2)
                    binding.textViewRateTitle.visibility = View.VISIBLE
                    binding.textViewRate.text = context.resources.getString(R.string.rating_text_4)
                    setStarBar()
                }

                R.id.image_view_star_5 -> {
                    starNumber = 5
                    binding.dialogRatingIcon.setImageResource(R.drawable.emoji_happy_svgrepo_com)
                    binding.textViewRateTitle.text =
                        context.resources.getString(R.string.rating_title_2)
                    binding.textViewRateTitle.visibility = View.VISIBLE
                    binding.textViewRate.text = context.resources.getString(R.string.rating_text_4)
                    setStarBar()
                }

                R.id.button_cancel -> {
                    dismiss()
                }
            }
        } else if (starNumber >= 4) {
            when {
                AppStoreHelper.isGooglePlayStore(context) || AppStoreHelper.isRunningInEmulator() -> {
                    redirectToPlayStoreForGooglePlay()
                }

                AppStoreHelper.isSamsungGalaxyStore(context) -> {
                    redirectToPlayStoreForSamsungGalaxy()
                }

                else -> {
                    Log.d("RateDialog", context.getString(R.string.unknown_installer))
                    Toast.makeText(
                        context, context.getString(R.string.unknown_installer), Toast.LENGTH_SHORT
                    ).show()
                }
            }

            dismiss()
        } else if (starNumber > 0) {
            dismiss()
            showFeedbackDialog()
        } else {
            binding.linearLayoutRatingBar.startAnimation(
                AnimationUtils.loadAnimation(
                    activity, com.codemybrainsout.ratingdialog.R.anim.shake
                )
            )
        }
    }

    private fun redirectToPlayStoreForGooglePlay() {
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri())
            )
            activity.intent.setPackage(GOOGLE_PLAY_PACKAGE_NAME)
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                )
            )
        }
    }

    private fun redirectToPlayStoreForSamsungGalaxy() {
        val packageName = context.packageName
        val galaxyStoreAppRatingUri = "samsungapps://AppRating/$packageName".toUri()
        val intent = Intent(Intent.ACTION_VIEW, galaxyStoreAppRatingUri)

        // Check if the Galaxy Store app is installed
        if (intent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                activity.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://apps.samsung.com/appquery/AppRating.as?appId=${context.packageName}".toUri()
                    )
                )
            }
        } else {
            // Handle the case where the Galaxy Store is not installed
            Log.e("RateDialog", context.getString(R.string.galaxy_store_not_found))
            Toast.makeText(
                context, context.getString(R.string.galaxy_store_not_found), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showFeedbackDialog() {
        val dialog = Dialog(context)
        val binding = DialogFeedbackBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        binding.btnSubmit.setOnClickListener {

            if (binding.dialogRatingFeedback.text.toString()
                    .isEmpty() || binding.dialogRatingFeedback.text.toString().length < 10
            ) {
                Toast.makeText(
                    activity,
                    context.getString(R.string.please_explain_in_more_detail),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // go to email
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = "mailto:dev.rpmstudio@gmail.com".toUri()
                intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_feedback))
                intent.putExtra(Intent.EXTRA_TEXT, binding.dialogRatingFeedback.text.toString())
                try {
                    context.startActivity(
                        Intent.createChooser(
                            intent, context.getString(R.string.send_via_email)
                        )
                    )
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(
                        activity,
                        context.getString(R.string.could_not_find_an_application_to_send_email),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (_: Exception) {
                    Toast.makeText(
                        activity, context.getString(R.string.network_problem), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.ivFeedbackCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun setStarBar() {
        for (i in imageViewStars.indices) {
            if (i < starNumber) {
                imageViewStars[i]?.setImageResource(R.drawable.ic_round_star_on)
            } else {
                imageViewStars[i]?.setImageResource(R.drawable.ic_round_star)
            }
        }
        if (starNumber < 4) {
            binding.btnSelectStar.setText(R.string.feedback_button)
        } else {
            binding.btnSelectStar.setText(R.string.submit_button)
        }
    }

}