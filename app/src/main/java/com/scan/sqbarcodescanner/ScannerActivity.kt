package com.scan.sqbarcodescanner

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.hardware.Camera
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.common.internal.Objects
import com.google.android.material.chip.Chip
import com.scan.sqbarcodescanner.barcode.BarcodeProcessor
import com.scan.sqbarcodescanner.camera.CameraSource
import com.scan.sqbarcodescanner.camera.CameraSourcePreview
import com.scan.sqbarcodescanner.camera.GraphicOverlay
import com.scan.sqbarcodescanner.viewmodel.WorkflowModel
import kotlinx.android.synthetic.main.camera_preview_overlay_kotlin.*
import java.io.IOException

class ScannerActivity : AppCompatActivity(),View.OnClickListener {

    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var flashButton: View? = null
    private var promptChip: Chip? = null
    private var promptChipAnimator: AnimatorSet? = null
    private var workflowModel: WorkflowModel? = null
    private var currentWorkflowState: WorkflowModel.WorkflowState? = null
    private val TAG = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scanner_layout)
        preview = findViewById(R.id.camera_preview)
        graphicOverlay = findViewById<GraphicOverlay>(R.id.camera_preview_graphic_overlay).apply {
            setOnClickListener(this@ScannerActivity)
            cameraSource = CameraSource(this)
        }

        promptChip = findViewById(R.id.bottom_prompt_chip)
        promptChipAnimator =
            (AnimatorInflater.loadAnimator(this, R.animator.bottom_prompt_chip_enter) as AnimatorSet).apply {
                setTarget(promptChip)
            }

        findViewById<View>(R.id.close_button).setOnClickListener(this)
        flashButton = findViewById<View>(R.id.flash_button).apply {
            setOnClickListener(this@ScannerActivity)
        }

        setUpWorkflowModel()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.close_button -> onBackPressed()
            R.id.flash_button -> {
                flashButton?.let {
                    if (it.isSelected) {
                        it.isSelected = false
                        cameraSource?.updateFlashMode(Camera.Parameters.FLASH_MODE_OFF)
                    } else {
                        it.isSelected = true
                        cameraSource!!.updateFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
                    }
                }
            }
        }
    }

    private fun setUpWorkflowModel() {
        workflowModel = ViewModelProviders.of(this).get(WorkflowModel::class.java)

        // Observes the workflow state changes, if happens, update the overlay view indicators and
        // camera preview state.
        workflowModel!!.workflowState.observe(this, Observer { workflowState ->
            if (workflowState == null || Objects.equal(currentWorkflowState, workflowState)) {
                return@Observer
            }

            currentWorkflowState = workflowState
            Log.d(TAG, "Current workflow state: ${currentWorkflowState!!.name}")

            val wasPromptChipGone = promptChip?.visibility == View.GONE

            when (workflowState) {
                WorkflowModel.WorkflowState.DETECTING -> {
                    promptChip?.visibility = View.VISIBLE
                    promptChip?.setText(R.string.prompt_point_at_a_barcode)
                    startCameraPreview()
                }
                WorkflowModel.WorkflowState.CONFIRMING -> {
                    promptChip?.visibility = View.VISIBLE
                    promptChip?.setText(R.string.prompt_move_camera_closer)
                    startCameraPreview()
                }
                WorkflowModel.WorkflowState.SEARCHING -> {
                    promptChip?.visibility = View.VISIBLE
                    promptChip?.setText(R.string.prompt_searching)
                    stopCameraPreview()
                }
                WorkflowModel.WorkflowState.DETECTED, WorkflowModel.WorkflowState.SEARCHED -> {
                    promptChip?.visibility = View.GONE
                    stopCameraPreview()
                }
                else -> promptChip?.visibility = View.GONE
            }

            val shouldPlayPromptChipEnteringAnimation =
                wasPromptChipGone && promptChip?.visibility == View.VISIBLE
            promptChipAnimator?.let {
                if (shouldPlayPromptChipEnteringAnimation && !it.isRunning) it.start()
            }
        })

        workflowModel?.detectedBarcode?.observe(this, Observer { barcode ->
            if (barcode != null) {
                Toast.makeText(this,"Barcode detected successfully", Toast.LENGTH_SHORT).show()
                // val barcodeFieldList = ArrayList<BarcodeField>()
                //barcodeFieldList.add(BarcodeField("Raw Value", barcode.rawValue ?: ""))
                //BarcodeResultFragment.show(supportFragmentManager, barcodeFieldList)
            }
        })
    }

    private fun startCameraPreview() {
        val workflowModel = this.workflowModel ?: return
        val cameraSource = this.cameraSource ?: return
        if (!workflowModel.isCameraLive) {
            try {
                workflowModel.markCameraLive()
                preview?.start(cameraSource)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start camera preview!", e)
                cameraSource.release()
                this.cameraSource = null
            }
        }
    }

    private fun stopCameraPreview() {
        val workflowModel = this.workflowModel ?: return
        if (workflowModel.isCameraLive) {
            workflowModel.markCameraFrozen()
            flashButton?.isSelected = false
            preview?.stop()
        }
    }

    override fun onResume() {
        super.onResume()

        workflowModel?.markCameraFrozen()
        currentWorkflowState = WorkflowModel.WorkflowState.NOT_STARTED
        cameraSource?.setFrameProcessor(BarcodeProcessor(graphicOverlay!!, workflowModel!!))
        workflowModel?.setWorkflowState(WorkflowModel.WorkflowState.DETECTING)
    }

    override fun onPostResume() {
        super.onPostResume()
        //BarcodeResultFragment.dismiss(supportFragmentManager)
    }

    override fun onPause() {
        super.onPause()
        currentWorkflowState = WorkflowModel.WorkflowState.NOT_STARTED
        stopCameraPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource?.release()
        cameraSource = null
    }
}
