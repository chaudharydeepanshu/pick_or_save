package com.deepanshuchaudhary.pick_or_save

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Process ACTION_OPEN_DOCUMENT intent.
fun processActionOpenDocument(
    resultCode: Int, data: Intent?, context: Activity
): Boolean {

    val uiScope = CoroutineScope(Dispatchers.Main)
    uiScope.launch {

        val begin = System.nanoTime()

        if (filePickingResult != null) {
            processPickedFiles(resultCode, data, context)
        }

        val end = System.nanoTime()
        println("Elapsed time in nanoseconds: ${end - begin}")

    }
    return true
}

// Process ACTION_OPEN_DOCUMENT_TREE intent.
fun processActionOpenDocumentTree(
    resultCode: Int, data: Intent?, context: Activity
): Boolean {

    val uiScope = CoroutineScope(Dispatchers.Main)

    uiScope.launch {

        val begin = System.nanoTime()

        if (fileSavingResult != null) {
            processMultipleSaveFile(resultCode, data, context)
        } else if (filePickingResult != null) {
            processPickedDirectory(resultCode, data, context)
        }

        val end = System.nanoTime()
        println("Elapsed time in nanoseconds: ${end - begin}")

    }

    return true
}

// Process ACTION_CREATE_DOCUMENT intent.
fun processActionCreateDocument(
    resultCode: Int, data: Intent?, context: Activity
): Boolean {

    val uiScope = CoroutineScope(Dispatchers.Main)

    uiScope.launch {

        val begin = System.nanoTime()

        if (fileSavingResult != null) {
            processSingleSaveFile(resultCode, data, context)
        }

        val end = System.nanoTime()
        println("Elapsed time in nanoseconds: ${end - begin}")

    }

    return true
}