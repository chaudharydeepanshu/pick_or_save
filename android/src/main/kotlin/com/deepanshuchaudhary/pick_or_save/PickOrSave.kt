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

// For deciding what type of picker to open.
enum class PickerType { File, Photo }

var filePickingResult: MethodChannel.Result? = null

var fileSavingResult: MethodChannel.Result? = null

var fileMetadataResult: MethodChannel.Result? = null

var cacheFilePathFromUriResult: MethodChannel.Result? = null

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
        pickerType: PickerType,
        enableMultipleSelection: Boolean
    ) {
        try {
            Log.d(
                LOG_TAG,
                "pickFile - IN, allowedExtensions=$allowedExtensions, mimeTypesFilter=$mimeTypesFilter, localOnly=$localOnly, copyFileToCacheDir=$copyFileToCacheDir, pickerType=$pickerType, enableMultipleSelection=$enableMultipleSelection"
            )

            if (filePickingResult != null) {
                utils.finishWithAlreadyActiveError(resultCallback)
                return
            } else if (fileSavingResult != null) {
                utils.finishWithAlreadyActiveError(resultCallback)
                return
            } else {
                filePickingResult = resultCallback
            }

            if (pickerType == PickerType.File) {
                if (enableMultipleSelection) {
                    pickMultipleFiles(
                        allowedExtensions = allowedExtensions,
                        mimeTypesFilter = mimeTypesFilter,
                        localOnly = localOnly,
                        copyFileToCacheDir = copyFileToCacheDir,
                        context = activity
                    )
                } else {
                    pickSingleFile(
                        allowedExtensions = allowedExtensions,
                        mimeTypesFilter = mimeTypesFilter,
                        localOnly = localOnly,
                        copyFileToCacheDir = copyFileToCacheDir,
                        context = activity
                    )
                }
            } else if (pickerType == PickerType.Photo) {

                if (utils.isPhotoPickerAvailable()) {
                    val photoPickerMimeType: String = mimeTypesFilter.first().trim()

                    pickSingleOrMultiplePhoto(
                        allowedExtensions = allowedExtensions,
                        photoPickerMimeType = photoPickerMimeType,
                        copyFileToCacheDir = copyFileToCacheDir,
                        enableMultipleSelection = enableMultipleSelection,
                        context = activity
                    )

                } else {
                    val updatedMimeTypes: MutableList<String> = mutableListOf()

                    for (i in mimeTypesFilter.indices) {
                        if (i == 0 && mimeTypesFilter[i].trim() == "*/*") {
                            updatedMimeTypes.addAll(listOf("image/*", "video/*"))
                        } else {
                            updatedMimeTypes.add(mimeTypesFilter[i])
                        }
                    }

                    // Consider implementing fallback functionality so that users can still
                    // select images and videos if Photo Picker is not available.
                    if (enableMultipleSelection) {
                        pickMultipleFiles(
                            allowedExtensions = allowedExtensions,
                            mimeTypesFilter = updatedMimeTypes,
                            localOnly = localOnly,
                            copyFileToCacheDir = copyFileToCacheDir,
                            context = activity
                        )
                    } else {
                        pickSingleFile(
                            allowedExtensions = allowedExtensions,
                            mimeTypesFilter = updatedMimeTypes,
                            localOnly = localOnly,
                            copyFileToCacheDir = copyFileToCacheDir,
                            context = activity
                        )
                    }

                }
            }


        } catch (e: Exception) {
            utils.finishWithError(
                "pickFile_exception", e.stackTraceToString(), null, filePickingResult
            )
        } catch (e: Error) {
            utils.finishWithError(
                "pickFile_error", e.stackTraceToString(), null, filePickingResult
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

            if (filePickingResult != null) {
                utils.finishWithAlreadyActiveError(resultCallback)
                return
            } else if (fileSavingResult != null) {
                utils.finishWithAlreadyActiveError(resultCallback)
                return
            } else {
                fileSavingResult = resultCallback
//        utils.cancelSaving()
            }

            if (saveFiles == null) {
                utils.finishWithError(
                    "saveFiles_not_found",
                    "Save files list is null",
                    "Save files list is null",
                    filePickingResult
                )
            } else if (saveFiles.isEmpty()) {
                utils.finishWithError(
                    "saveFiles_not_found",
                    "Save files list is empty",
                    "Save files list is empty",
                    filePickingResult
                )
            } else if (saveFiles.size == 1) {
                saveSingleFile(
                    saveFileInfo = saveFiles.first(),
                    mimeTypesFilter = mimeTypesFilter,
                    localOnly = localOnly,
                    context = activity
                )
            } else {
                saveMultipleFiles(
                    saveFilesInfo = saveFiles,
                    mimeTypesFilter = mimeTypesFilter,
                    localOnly = localOnly,
                    context = activity
                )
            }


        } catch (e: Exception) {
            utils.finishWithError(
                "saveFile_exception", e.stackTraceToString(), null, filePickingResult
            )
        } catch (e: Error) {
            utils.finishWithError(
                "saveFile_error", e.stackTraceToString(), null, filePickingResult
            )
        }
    }

    // For picking single file or multiple files.
    fun fileMetaData(
        resultCallback: MethodChannel.Result,
        sourceFilePathOrUri: String?,
    ) {

        try {

            Log.d(
                LOG_TAG, "fileMetaData - IN, sourceFilePathOrUri=$sourceFilePathOrUri"
            )

            fileMetadataResult = resultCallback

            fileMetadataFromPathOrUri(
                resultCallback = fileMetadataResult,
                sourceFilePathOrUri = sourceFilePathOrUri!!,
                context = activity
            )

            Log.d(LOG_TAG, "fileMetaData - OUT")


        } catch (e: Exception) {
            utils.finishWithError(
                "pickFile_exception", e.stackTraceToString(), null, fileMetadataResult
            )
        } catch (e: Error) {
            utils.finishWithError(
                "pickFile_error", e.stackTraceToString(), null, fileMetadataResult
            )
        }
    }

    // For getting cached file path from file Uri.
    fun cacheFilePath(
        resultCallback: MethodChannel.Result,
        sourceFilePathOrUri: String?,
    ) {
        val uiScope = CoroutineScope(Dispatchers.Main)
        uiScope.launch {

            try {

                Log.d(
                    LOG_TAG, "cacheFileFromUri - IN, sourceFilePathOrUri=$sourceFilePathOrUri"
                )

                cacheFilePathFromUriResult = resultCallback

                val result = cacheFilePathFromPathOrUri(
                    sourceFilePathOrUri = sourceFilePathOrUri!!,
                    context = activity,
                    resultCallback = cacheFilePathFromUriResult
                )

                utils.finishSuccessfullyWithString(result, cacheFilePathFromUriResult)

                Log.d(LOG_TAG, "cacheFileFromUri - OUT")


            } catch (e: Exception) {
                utils.finishWithError(
                    "cacheFileFromUri_exception",
                    e.stackTraceToString(),
                    null,
                    cacheFilePathFromUriResult
                )
            } catch (e: Error) {
                utils.finishWithError(
                    "cacheFileFromUri_error",
                    e.stackTraceToString(),
                    null,
                    cacheFilePathFromUriResult
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
