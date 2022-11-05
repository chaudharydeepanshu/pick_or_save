package com.deepanshuchaudhary.pick_or_save

import android.app.Activity
import android.content.Intent
import android.util.Log
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//https://developer.android.com/training/data-storage/shared/documents-files#open-file

private const val LOG_TAG = "PickOrSave"

// For deciding what type of file picking dialog to open.
enum class FilePickingType { SINGLE, MULTIPLE }

var filePickingResult: MethodChannel.Result? = null

var fileSavingResult: MethodChannel.Result? = null

class PickOrSave(
    private val activity: Activity
) : PluginRegistry.ActivityResultListener {

    private val utils = Utils()

    // For picking single file or multiple files.
    fun pickFile(
        resultCallback: MethodChannel.Result,
        allowedExtensions: List<String>,
        mimeTypesFilter: List<String>,
        localOnly: Boolean,
        copyFileToCacheDir: Boolean,
        filePickingType: FilePickingType
    ) {
        try {
            Log.d(
                LOG_TAG,
                "pickFile - IN, allowedExtensions=$allowedExtensions, mimeTypesFilter=$mimeTypesFilter, localOnly=$localOnly, copyFileToCacheDir=$copyFileToCacheDir, filePickingType=$filePickingType"
            )

            if (filePickingType == FilePickingType.MULTIPLE) {
                pickMultipleFiles(
                    resultCallback = resultCallback,
                    allowedExtensions = allowedExtensions,
                    mimeTypesFilter = mimeTypesFilter,
                    localOnly = localOnly,
                    copyFileToCacheDir = copyFileToCacheDir,
                    context = activity
                )
            } else {
                pickSingleFile(
                    resultCallback = resultCallback,
                    allowedExtensions = allowedExtensions,
                    mimeTypesFilter = mimeTypesFilter,
                    localOnly = localOnly,
                    copyFileToCacheDir = copyFileToCacheDir,
                    context = activity
                )
            }

        } catch (e: Exception) {
            utils.finishWithError(
                "pickFile_exception", e.stackTraceToString(), null, resultCallback
            )
        } catch (e: Error) {
            utils.finishWithError(
                "pickFile_error", e.stackTraceToString(), null, resultCallback
            )
        }
    }

    // For saving single file or multiple files.
    fun saveFile(
        resultCallback: MethodChannel.Result,
        saveFiles: List<SaveFileInfo>?,
        mimeTypesFilter: List<String>?,
        localOnly: Boolean
    ) {
        try {

            Log.d(
                LOG_TAG,
                "saveFile - IN, saveFiles=$saveFiles, mimeTypesFilter=$mimeTypesFilter, localOnly=$localOnly"
            )

            if (saveFiles == null) {
                utils.finishWithError(
                    "saveFiles_not_found",
                    "Save files list is null",
                    "Save files list is null",
                    resultCallback
                )
            } else if (saveFiles.isEmpty()) {
                utils.finishWithError(
                    "saveFiles_not_found",
                    "Save files list is empty",
                    "Save files list is empty",
                    resultCallback
                )
            } else if (saveFiles.size == 1) {
                saveSingleFile(
                    resultCallback = resultCallback,
                    saveFileInfo = saveFiles.first(),
                    mimeTypesFilter = mimeTypesFilter,
                    localOnly = localOnly,
                    context = activity
                )
            } else {
                saveMultipleFiles(
                    resultCallback = resultCallback,
                    saveFilesInfo = saveFiles,
                    mimeTypesFilter = mimeTypesFilter,
                    localOnly = localOnly,
                    context = activity
                )
            }


        } catch (e: Exception) {
            utils.finishWithError(
                "saveFile_exception", e.stackTraceToString(), null, resultCallback
            )
        } catch (e: Error) {
            utils.finishWithError(
                "saveFile_error", e.stackTraceToString(), null, resultCallback
            )
        }
    }

    // For picking single file or multiple files.
    fun fileMetaData(
        resultCallback: MethodChannel.Result,
        sourceFileUri: String?,
        sourceFilePath: String?,
    ) {

        try {

            Log.d(
                LOG_TAG,
                "fileMetaData - IN, sourceFileUri=$sourceFileUri, sourceFilePath=$sourceFilePath"
            )

            fileMetadata(
                resultCallback = resultCallback,
                sourceFileUri = sourceFileUri,
                sourceFilePath = sourceFilePath,
                context = activity
            )

            Log.d(LOG_TAG, "fileMetaData - OUT")


        } catch (e: Exception) {
            utils.finishWithError(
                "pickFile_exception", e.stackTraceToString(), null, resultCallback
            )
        } catch (e: Error) {
            utils.finishWithError(
                "pickFile_error", e.stackTraceToString(), null, resultCallback
            )
        }
    }

    // For getting cached file path from file Uri.
    fun cacheFilePathFromUri(
        resultCallback: MethodChannel.Result,
        sourceFileUri: String?,
    ) {
        val uiScope = CoroutineScope(Dispatchers.Main)
        uiScope.launch {

            try {

                Log.d(
                    LOG_TAG, "cacheFileFromUri - IN, sourceFileUri=$sourceFileUri"
                )

                val result = cacheFilePathFromUri(
                    sourceFileUri = sourceFileUri!!,
                    context = activity,
                    resultCallback = resultCallback
                )

                utils.finishSuccessfullyWithString(result, resultCallback)

                Log.d(LOG_TAG, "cacheFileFromUri - OUT")


            } catch (e: Exception) {
                utils.finishWithError(
                    "cacheFileFromUri_exception", e.stackTraceToString(), null, resultCallback
                )
            } catch (e: Error) {
                utils.finishWithError(
                    "cacheFileFromUri_error", e.stackTraceToString(), null, resultCallback
                )
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {

        return when (requestCode) {
            utils.REQUEST_CODE_PICK_FILE -> {
                processPickedFiles(resultCode = resultCode, data = data, context = activity)
            }
            utils.REQUEST_CODE_SAVE_FILE -> {
                processSingleSaveFile(resultCode = resultCode, data = data, context = activity)
            }
            utils.REQUEST_CODE_SAVE_MULTIPLE_FILES -> {
                processMultipleSaveFile(resultCode = resultCode, data = data, context = activity)
            }
            else -> false
        }
    }

    fun cancelFilesSaving(
    ) {
        utils.cancelSaving()
    }
}
