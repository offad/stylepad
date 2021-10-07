package com.davidfadare.notes.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.AdapterView
import android.widget.GridView
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.davidfadare.notes.R
import com.davidfadare.notes.recycler.ImageAdapter
import com.davidfadare.notes.util.Utility.Companion.addBorder

class DrawingPreviewPagerFragment : Fragment() {

    var data: Array<String>? = emptyArray()
    var noteColor: Int = 0
    private var gridView: GridView? = null

    private var currentAnimator: Animator? = null
    private var shortAnimationDuration = 0
    private var animationReady = true

    private var expandedImageView: ImageView? = null
    private var noteListContainter: RelativeLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        data = arguments?.getStringArray("someData")
        noteColor = arguments?.getInt("noteColor") ?: 0
        if (noteColor == 0 && context != null) {
            noteColor = ContextCompat.getColor(context!!, R.color.blue_note)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(
                R.layout.fragment_preview_drawing, container, false) as ViewGroup

        val emptyView = rootView.findViewById<RelativeLayout>(R.id.empty_view)
        noteListContainter = rootView.findViewById(R.id.noteListContainer)
        val emptyViewImage = rootView.findViewById<ImageView>(R.id.empty_note_image)
        emptyViewImage.drawable.setColorFilter(noteColor, PorterDuff.Mode.SRC_IN)
        expandedImageView = rootView.findViewById(R.id.expanded_image)

        gridView = rootView.findViewById(R.id.drawing_grid)
        gridView?.emptyView = emptyView

        onSetupGridView()

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)
        return rootView
    }

    private fun onSetupGridView() {
        if (data != null) {
            val imageAdapter = ImageAdapter(context!!, noteColor, *data!!)
            gridView?.adapter = imageAdapter
        }
        gridView?.choiceMode = GridView.CHOICE_MODE_SINGLE
        gridView?.onItemClickListener = AdapterView.OnItemClickListener { _, view, i, _ ->
            if (animationReady) {
                val imageLocation = data?.get(i)
                zoomImageFromThumb(view, imageLocation)
            }
        }
    }

    private fun zoomImageFromThumb(thumbView: View, pictureFile: String?) {
        animationReady = false
        currentAnimator?.cancel()
        if (pictureFile.isNullOrEmpty() || expandedImageView == null) return

        val option = BitmapFactory.Options()
        option.inMutable = true
        option.inPreferredConfig = Bitmap.Config.ARGB_8888
        val imageBitmap = BitmapFactory.decodeFile(pictureFile, option)
        val borderBitmap = addBorder(imageBitmap, noteColor)

        expandedImageView?.setImageBitmap(borderBitmap)

        val startBoundsInt = Rect()
        val finalBoundsInt = Rect()
        val globalOffset = Point()

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBoundsInt)
        noteListContainter?.getGlobalVisibleRect(finalBoundsInt, globalOffset)
        startBoundsInt.offset(-globalOffset.x, -globalOffset.y)
        finalBoundsInt.offset(-globalOffset.x, -globalOffset.y)

        val startBounds = RectF(startBoundsInt)
        val finalBounds = RectF(finalBoundsInt)

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        val startScale: Float
        if ((finalBounds.width() / finalBounds.height() > startBounds.width() / startBounds.height())) {
            // Extend start bounds horizontally
            startScale = startBounds.height() / finalBounds.height()
            val startWidth: Float = startScale * finalBounds.width()
            val deltaWidth: Float = (startWidth - startBounds.width()) / 2
            startBounds.left -= deltaWidth.toInt()
            startBounds.right += deltaWidth.toInt()
        } else {
            // Extend start bounds vertically
            startScale = startBounds.width() / finalBounds.width()
            val startHeight: Float = startScale * finalBounds.height()
            val deltaHeight: Float = (startHeight - startBounds.height()) / 2f
            startBounds.top -= deltaHeight.toInt()
            startBounds.bottom += deltaHeight.toInt()
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.alpha = 0f
        expandedImageView?.visibility = View.VISIBLE

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView?.pivotX = 0f
        expandedImageView?.pivotY = 0f

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        currentAnimator = AnimatorSet().apply {
            play(ObjectAnimator.ofFloat(
                    expandedImageView,
                    View.X,
                    startBounds.left,
                    finalBounds.left)
            ).apply {
                with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top, finalBounds.top))
                with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f))
            }
            duration = shortAnimationDuration.toLong()
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {

                override fun onAnimationEnd(animation: Animator) {
                    currentAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    currentAnimator = null
                }
            })
            start()
        }

        expandedImageView?.setOnClickListener {
            currentAnimator?.cancel()

            currentAnimator = AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left)).apply {
                    with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                    with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale))
                    with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale))
                }
                duration = shortAnimationDuration.toLong()
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation: Animator) {
                        thumbView.alpha = 1f
                        expandedImageView?.visibility = View.GONE
                        currentAnimator = null
                        animationReady = true
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        thumbView.alpha = 1f
                        expandedImageView?.visibility = View.GONE
                        currentAnimator = null
                        animationReady = true
                    }
                })
                start()
            }
        }
    }

    companion object {
        fun newInstance(text: Array<String>, color: Int): DrawingPreviewPagerFragment {
            val fragment = DrawingPreviewPagerFragment()
            val args = Bundle()
            args.putStringArray("someData", text)
            args.putInt("noteColor", color)
            fragment.arguments = args
            return fragment
        }
    }
}
