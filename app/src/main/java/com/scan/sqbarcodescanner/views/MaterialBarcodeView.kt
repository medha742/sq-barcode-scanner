package com.scan.sqbarcodescanner.views

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.scan.sqbarcodescanner.R
import com.scan.sqbarcodescanner.utilities.Util

class MaterialBarcodeView : CoordinatorLayout {
    private var view: View?=null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initializePrimaryTask(context,attrs,null)
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context,attrs,defStyle) {
        initializePrimaryTask(context,attrs,defStyle)
    }

    constructor(context: Context):super(context){
        initializePrimaryTask(context,null,null)
    }

    fun initializePrimaryTask(context: Context, attrs: AttributeSet?,defStyle: Int?){
        view = LayoutInflater.from(context).inflate(R.layout.barcode_material_view,this)
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it,
                R.styleable.MaterialBarcodeView, 0, 0)
            val rectangleWidth = typedArray.getInt(R.styleable
                .MaterialBarcodeView_rectangle_width,80)

            val laserColor = typedArray.getColor(R.styleable.MaterialBarcodeView_center_laser,
                ContextCompat.getColor(context,R.color.white))
            val borderColorRectangle = typedArray.getColor(R.styleable.MaterialBarcodeView_rectangle_border,
                ContextCompat.getColor(context,R.color.white))
            Util.BORDER_COLOR = borderColorRectangle
            Util.LASER_COLOR = laserColor
            Util.BARCODE_RECT_WIDTH = rectangleWidth
            val rectangleHeight = typedArray
                .getInt(R.styleable
                    .MaterialBarcodeView_rectangle_height,
                    35)

            Util.BARCODE_RECT_HEIGHT = rectangleHeight

            typedArray.recycle()
        }

    }
}