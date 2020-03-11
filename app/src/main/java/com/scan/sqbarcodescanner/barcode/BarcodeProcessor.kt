package com.scan.sqbarcodescanner.barcode

import android.animation.ValueAnimator
import android.util.Log
import androidx.annotation.MainThread
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.scan.sqbarcodescanner.camera.FrameProcessorBase
import com.scan.sqbarcodescanner.camera.GraphicOverlay
import com.scan.sqbarcodescanner.utilities.Util
import com.scan.sqbarcodescanner.viewmodel.WorkflowModel
import java.io.IOException

/** A processor to run the barcode detector.  */
class BarcodeProcessor(graphicOverlay: GraphicOverlay, private val workflowModel: WorkflowModel) :
    FrameProcessorBase<List<FirebaseVisionBarcode>>() {

    private val detector = FirebaseVision.getInstance().visionBarcodeDetector
    private val barcodeScanningGraphic: BarcodeScanningGraphic = BarcodeScanningGraphic(graphicOverlay)

    override fun detectInImage(image: FirebaseVisionImage): Task<List<FirebaseVisionBarcode>> =
        detector.detectInImage(image)

    @MainThread
    override fun onSuccess(
        image: FirebaseVisionImage,
        results: List<FirebaseVisionBarcode>,
        graphicOverlay: GraphicOverlay
    ) {

        if (!workflowModel.isCameraLive) return

        Log.d(TAG, "Barcode result size: ${results.size}")

        // Picks the barcode, if exists, that covers the center of graphic overlay.

        val barcodeInCenter = results.firstOrNull { barcode ->
            val boundingBox = barcode.boundingBox ?: return@firstOrNull false
            val box = graphicOverlay.translateRect(boundingBox)
            box.contains(graphicOverlay.width / 2f, graphicOverlay.height / 2f)
        }

        graphicOverlay.clear()
        if (barcodeInCenter == null) {
            //cameraReticleAnimator.start()
            graphicOverlay.add(BarcodeScanningGraphic(graphicOverlay))
            //graphicOverlay.add(BarcodeReticleGraphic(graphicOverlay, cameraReticleAnimator))
            workflowModel.setWorkflowState(WorkflowModel.WorkflowState.DETECTING)
        } else {
            //cameraReticleAnimator.cancel()
            val sizeProgress = Util.getProgressToMeetBarcodeSizeRequirement(graphicOverlay, barcodeInCenter)
            if (sizeProgress < 1) {
                graphicOverlay.add(BarcodeScanningGraphic(graphicOverlay))
                // Barcode in the camera view is too small, so prompt user to move camera closer.
                //graphicOverlay.add(BarcodeConfirmingGraphic(graphicOverlay, barcodeInCenter))
                workflowModel.setWorkflowState(WorkflowModel.WorkflowState.CONFIRMING)
            } else {
                // Barcode size in the camera view is sufficient.
                // if (PreferenceUtils.shouldDelayLoadingBarcodeResult(graphicOverlay.context)) {
                //val loadingAnimator = createLoadingAnimator(graphicOverlay, barcodeInCenter)
                //loadingAnimator.start()
                graphicOverlay.add(BarcodeScanningGraphic(graphicOverlay))
                //graphicOverlay.add(BarcodeLoadingGraphic(graphicOverlay, loadingAnimator))
                workflowModel.setWorkflowState(WorkflowModel.WorkflowState.SEARCHING)
                workflowModel.setWorkflowState(WorkflowModel.WorkflowState.DETECTED)
                workflowModel.detectedBarcode.setValue(barcodeInCenter)
                /*} else {
                    workflowModel.setWorkflowState(WorkflowModel.WorkflowState.DETECTED)
                    workflowModel.detectedBarcode.setValue(barcodeInCenter)
                }*/
            }
        }
        graphicOverlay.invalidate()
    }

    private fun createLoadingAnimator(graphicOverlay: GraphicOverlay, barcode: FirebaseVisionBarcode): ValueAnimator {
        val endProgress = 1.1f
        return ValueAnimator.ofFloat(0f, endProgress).apply {
            duration = 2000
            addUpdateListener {
                if ((animatedValue as Float).compareTo(endProgress) >= 0) {
                    graphicOverlay.clear()
                    workflowModel.setWorkflowState(WorkflowModel.WorkflowState.SEARCHED)
                    workflowModel.detectedBarcode.setValue(barcode)
                } else {
                    graphicOverlay.invalidate()
                }
            }
        }
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Barcode detection failed!", e)
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to close barcode detector!", e)
        }
    }

    companion object {
        private const val TAG = "BarcodeProcessor"
    }
}
