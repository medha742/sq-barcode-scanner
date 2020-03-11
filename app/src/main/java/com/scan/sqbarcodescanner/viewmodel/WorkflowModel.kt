package com.scan.sqbarcodescanner.viewmodel

import android.app.Application
import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode

class WorkflowModel(application: Application) : AndroidViewModel(application) {

    val workflowState = MutableLiveData<WorkflowState>()
    val detectedBarcode = MutableLiveData<FirebaseVisionBarcode>()

    var isCameraLive = false
        private set


    private val context: Context
        get() = getApplication<Application>().applicationContext

    /**
     * State set of the application workflow.
     */
    enum class WorkflowState {
        NOT_STARTED,
        DETECTING,
        DETECTED,
        CONFIRMING,
        CONFIRMED,
        SEARCHING,
        SEARCHED
    }

    @MainThread
    fun setWorkflowState(workflowState: WorkflowState) {
        if (workflowState != WorkflowState.CONFIRMED &&
            workflowState != WorkflowState.SEARCHING &&
            workflowState != WorkflowState.SEARCHED) {
        }
        this.workflowState.value = workflowState
    }

    @MainThread
    fun onSearchButtonClicked() {
    }


    fun markCameraLive() {
        isCameraLive = true
    }

    fun markCameraFrozen() {
        isCameraLive = false
    }
}