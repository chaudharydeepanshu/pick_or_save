package com.deepanshuchaudhary.pick_or_save

import android.app.Activity
import android.content.Intent
import android.util.Log
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.net.Uri

//https://developer.android.com/training/data-storage/shared/documents-files#open-file

private const val LOG_TAG = "PickOrSave"

// For deciding what type of picker to open.
enum class PickerType { File, Photo }

enum class CancelType {
    FilesSaving, DirectoryDocumentsPicker
}

var filePickingResult: MethodChannel.Result? = null

var directoryDocumentsPickingResult: MethodChannel.Result? = null

var fileSavingResult: MethodChannel.Result? = null

var fileMetadataResult: MethodChannel.Result? = null

var cacheFilePathFromUriResult: MethodChannel.Result? = null

class PickOrSave(
    private val activity: Activity
) : PluginRegistry.ActivityResultListener {

    private val utils = Utils()

    // For picking files under a directory.
    fun pickDirectoryDocuments(
        resultCallback: MethodChannel.Result,
        documentId: String?,
        directoryUri: String?,
        recurseDirectories: Boolean?,
        allowedExtensions: List<String>,
        mimeTypesFilter: List<String>,
    ) {
        try {
            Log.d(
                LOG_TAG,
                "pickFile - IN, documentId=$documentId, directoryUri=$directoryUri, recurseDirectories=$recurseDirectories, allowedExtensions=$allowedExtensions, mimeTypesFilter=$mimeTypesFilter"
            )

            directoryDocumentsPickingResult = resultCallback

            pickDocumentsFromDirectoryUri(
                docId = documentId,
                directoryUri = directoryUri!!,
                recurseDirectories = recurseDirectories!!,
                context = activity,
                allowedExtensions = allowedExtensions,
                mimeTypesFilter = mimeTypesFilter
            )

        } catch (e: Exception) {
            utils.finishWithError(
                "pickDirectoryDocuments_exception",
                e.stackTraceToString(),
                null,
                directoryDocumentsPickingResult
            )
        } catch (e: Error) {
            utils.finishWithError(
                "pickDirectoryDocuments_error",
                e.stackTraceToString(),
                null,
                directoryDocumentsPickingResult
            )
        }
    }

    // For picking directory.
    fun pickDirectory(
        resultCallback: MethodChannel.Result,
        initialDirectoryUri: String?,
    ) {
        try {
            Log.d(
                LOG_TAG, "pickFile - IN, initialDirectoryUri=$initialDirectoryUri"
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

            pickSingleDirectory(
                initialDirectoryUri = initialDirectoryUri, context = activity
            )

        } catch (e: Exception) {
            utils.finishWithError(
                "pickDirectory_exception", e.stackTraceToString(), null, filePickingResult
            )
        } catch (e: Error) {
            utils.finishWithError(
                "pickDirectory_error", e.stackTraceToString(), null, filePickingResult
            )
        }
    }

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
        localOnly: Boolean,
        directoryUri: String?,
    ) {
        try {

            Log.d(
                LOG_TAG,
                "saveFile - IN, mimeTypesFilter=$mimeTypesFilter, localOnly=$localOnly, directoryUri=$directoryUri, saveFiles=$saveFiles"
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
            } else if (saveFiles.size == 1 && directoryUri == null) {
                saveSingleFile(
                    saveFileInfo = saveFiles.first(),
                    mimeTypesFilter = mimeTypesFilter,
                    localOnly = localOnly,
                    context = activity,
                )
            } else {
                saveMultipleFiles(
                    saveFilesInfo = saveFiles,
                    mimeTypesFilter = mimeTypesFilter,
                    localOnly = localOnly,
                    context = activity,
                    destinationDirectoryUri = directoryUri
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

                utils.finishSuccessfully(result, cacheFilePathFromUriResult)

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
            utils.REQUEST_CODE_ACTION_OPEN_DOCUMENT -> {
                processActionOpenDocument(resultCode = resultCode, data = data, context = activity)
            }

            utils.REQUEST_CODE_ACTION_CREATE_DOCUMENT -> {
                processActionCreateDocument(
                    resultCode = resultCode, data = data, context = activity
                )
            }

            utils.REQUEST_CODE_ACTION_OPEN_DOCUMENT_TREE -> {
                processActionOpenDocumentTree(
                    resultCode = resultCode, data = data, context = activity
                )
            }

            else -> false
        }
    }

    fun cancelActions(
        cancelType: CancelType?,
    ) {
        Log.d(
            LOG_TAG, "cancelActions - IN, cancelType=$cancelType"
        )

        if (cancelType == CancelType.FilesSaving) {
            utils.cancelSaving()
        } else if (cancelType == CancelType.DirectoryDocumentsPicker) {
            utils.cancelDirectoryDocumentsPicker()
        }
    }

    fun uriPermissionStatus(
        resultCallback: MethodChannel.Result, uri: String?, releasePermission: Boolean?
    ) {
        Log.d(
            LOG_TAG, "uriPermissionStatus - IN, uri=$uri, releasePermission:$releasePermission"
        )

        var isPermissionGranted = false

        val contentResolver = activity.contentResolver

        val flags: Int =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        val grantedUris = mutableListOf<String>()

        // List of all persisted permissions for our app.
        val list = contentResolver.persistedUriPermissions
        for (i in list.indices) {
            val persistedUriString = list[i].uri.toString()

            if (list[i].isWritePermission && list[i].isReadPermission) {
                grantedUris.add(persistedUriString)
            }
        }

        if (releasePermission!! && grantedUris.contains(uri)) {
            contentResolver.releasePersistableUriPermission(utils.getURI(uri!!), flags)
            grantedUris.remove(uri)
        }

        if (grantedUris.contains(uri)) {
            isPermissionGranted = true
        }

        utils.finishSuccessfully(isPermissionGranted, resultCallback)

        Log.d(LOG_TAG, "uriPermissionStatus - OUT")
    }

    fun urisWithPersistedPermission(resultCallback: MethodChannel.Result) {
        Log.d(
            LOG_TAG, "urisWithPersistedPermission - IN"
        )

        val contentResolver = activity.contentResolver

        val grantedUris = mutableListOf<String>()

        // list of all persisted permissions for our app
        val list = contentResolver.persistedUriPermissions
        for (i in list.indices) {
            val persistedUriString = list[i].uri.toString()

            if (list[i].isWritePermission && list[i].isReadPermission) {
                grantedUris.add(persistedUriString)
            }
        }
        utils.finishSuccessfully(grantedUris, resultCallback)

        Log.d(LOG_TAG, "urisWithPersistedPermission - OUT")
    }
}

