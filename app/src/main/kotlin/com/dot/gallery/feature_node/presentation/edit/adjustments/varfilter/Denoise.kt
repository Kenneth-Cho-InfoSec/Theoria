package com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter

import android.graphics.Bitmap
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature_node.domain.model.editor.TileBehavior
import com.dot.gallery.feature_node.domain.model.editor.VariableFilter

data class Denoise(
    @param:FloatRange(from = 0.0, to = 1.0)
    override val value: Float = 0f
) : VariableFilter {
    override val maxValue = 1f
    override val minValue = 0f
    override val defaultValue = 0f

    override fun apply(bitmap: Bitmap): Bitmap {
        // The denoise library was removed; denoise is a no-op so the editor keeps working.
        return bitmap
    }

    override fun revert(bitmap: Bitmap): Bitmap = bitmap

    override fun colorMatrix(): ColorMatrix? = null

    // Legacy stackBlur radius went up to 10px. The adjustment is a no-op now, so this halo is
    // never consumed by the bake engine.
    override val tileBehavior: TileBehavior get() = TileBehavior.Kernel(radius = 10)
}
