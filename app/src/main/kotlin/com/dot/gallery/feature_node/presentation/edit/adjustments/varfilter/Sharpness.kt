package com.dot.gallery.feature_node.presentation.edit.adjustments.varfilter

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorMatrix
import com.dot.gallery.feature_node.domain.model.editor.TileBehavior
import com.dot.gallery.feature_node.domain.model.editor.VariableFilter
import kotlin.math.roundToInt

data class Sharpness(
    override val value: Float = 5f
) : VariableFilter {
    override val maxValue = 9f
    override val minValue = 3f
    override val defaultValue = 5f

    override fun apply(bitmap: Bitmap): Bitmap {
        // The sharpen library was removed; sharpening is a no-op so the editor keeps working.
        return bitmap
    }

    override fun revert(bitmap: Bitmap): Bitmap {
        return bitmap
    }

    override fun colorMatrix(): ColorMatrix? = null

    // Legacy sharpen convolved with an odd kernel up to 9x9 → 4px halo. The adjustment is a no-op
    // now, so this halo is never consumed by the bake engine.
    override val tileBehavior: TileBehavior get() = TileBehavior.Kernel(radius = 4)

    fun floatToOddKernelSize(value: Float, min: Int = 3, max: Int = 9): Int {
        var intValue = value.roundToInt()
        if (intValue < min) intValue = min
        if (intValue > max) intValue = max
        // Ensure odd
        if (intValue % 2 == 0) intValue += 1
        if (intValue > max) intValue -= 2 // avoid exceeding max
        return intValue
    }

}