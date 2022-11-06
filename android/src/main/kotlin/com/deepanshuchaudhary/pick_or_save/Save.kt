package com.deepanshuchaudhary.pick_or_save

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.webkit.MimeTypeMap
import com.deepanshuchaudhary.pick_or_save.PickOrSavePlugin.Companion.LOG_TAG
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

//private var isSourceFileTemp : Boolean = false
private var destinationSaveFileInfo: DestinationSaveFileInfo? = null
private var destinationSaveFilesInfo: MutableList<DestinationSaveFileInfo> = mutableListOf()

var fileSaveJob: Job? = null

data class DestinationSaveFileInfo(
    val file: File,
    val fileName: String,
    val saveFileNamePrefix: String,
    val saveFileNameSuffix: String,
    val isTempFile: Boolean = false
)

data class SaveFileInfo(val filePath: String?, val fileData: ByteArray?, val fileName: String?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SaveFileInfo

        if (filePath != other.filePath) return false
        if (!fileData.contentEquals(other.fileData)) return false
        if (fileName != other.fileName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = filePath.hashCode()
        result = 31 * result + fileData.contentHashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }
}

// For saving single file.
fun saveSingleFile(
    resultCallback: MethodChannel.Result,
    saveFileInfo: SaveFileInfo,
    mimeTypesFilter: List<String>?,
    localOnly: Boolean,
    context: Activity,
) {

    val utils = Utils()

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

    val begin = System.nanoTime()

    val saveFile: File

    val saveFileName: String
    val saveFileNameSuffix: String
    val saveFileNamePrefix: String

    if (saveFileInfo.filePath != null) {
        // Getting source file.
        saveFile = File(saveFileInfo.filePath)
        if (!saveFile.exists()) {
            utils.finishWithError(
                "saveFile_not_found", "Save file is missing", saveFileInfo.filePath, resultCallback
            )
            return
        }
        saveFileName = saveFileInfo.fileName ?: saveFile.name ?: "Unknown.ext"
        saveFileNameSuffix = "." + utils.getFileExtension(saveFileName)
        saveFileNamePrefix = saveFileName.dropLast(saveFileNameSuffix.length)

        destinationSaveFileInfo = DestinationSaveFileInfo(
            file = saveFile,
            fileName = saveFileName,
            saveFileNamePrefix = saveFileNamePrefix,
            saveFileNameSuffix = saveFileNameSuffix,
            isTempFile = false
        )

    } else {
        // Writing data to temporary file.
        saveFileName = saveFileInfo.fileName ?: "Unknown.ext"
        saveFileNameSuffix = "." + utils.getFileExtension(saveFileName)
        saveFileNamePrefix = saveFileName.dropLast(saveFileNameSuffix.length)
        saveFile = File.createTempFile(saveFileNamePrefix, saveFileNameSuffix)
        saveFile.writeBytes(saveFileInfo.fileData!!)

        destinationSaveFileInfo = DestinationSaveFileInfo(
            file = saveFile,
            fileName = saveFileName,
            saveFileNamePrefix = saveFileNamePrefix,
            saveFileNameSuffix = saveFileNameSuffix,
            isTempFile = false
        )
    }

    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
    intent.putExtra(Intent.EXTRA_TITLE, saveFileNamePrefix)
    if (localOnly) {
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
    }

    // Setting mimeType for file saving dialog to avoid adding extension manually when saving.
    val sourceFileMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(saveFile.extension)
    intent.type = sourceFileMimeType ?: "*/*"

    utils.applyMimeTypesFilterToIntent(mimeTypesFilter, intent)

    context.startActivityForResult(intent, utils.REQUEST_CODE_SAVE_FILE)

    Log.d(LOG_TAG, "saveFile - OUT")

    val end = System.nanoTime()
    println("Elapsed time in nanoseconds: ${end - begin}")

}

// For saving multiple file.
fun saveMultipleFiles(
    resultCallback: MethodChannel.Result,
    saveFilesInfo: List<SaveFileInfo>,
    mimeTypesFilter: List<String>?,
    localOnly: Boolean,
    context: Activity,
) {

    val utils = Utils()

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

    val begin = System.nanoTime()

    saveFilesInfo.indices.map { index ->

        val saveFileInfo = saveFilesInfo.elementAt(index)
        val saveFile: File

        val saveFileName: String
        val saveFileNameSuffix: String
        val saveFileNamePrefix: String

        if (saveFileInfo.filePath != null) {
            // Getting source file.
            saveFile = File(saveFileInfo.filePath)
            if (!saveFile.exists()) {
                Log.d(LOG_TAG, "saveFile_not_found ${saveFileInfo.filePath}")
//            utils.finishWithError(
//                "saveFile_not_found", "Save file is missing", saveFileInfo.filePath, resultCallback
//            )
//            return
            }
            saveFileName = saveFile.name ?: "Unknown.ext"
            saveFileNameSuffix = "." + utils.getFileExtension(saveFileName)
            saveFileNamePrefix = saveFileName.dropLast(saveFileNameSuffix.length)
            destinationSaveFilesInfo.add(
                DestinationSaveFileInfo(
                    file = saveFile,
                    fileName = saveFileName,
                    saveFileNamePrefix = saveFileNamePrefix,
                    saveFileNameSuffix = saveFileNameSuffix,
                    isTempFile = false
                )
            )

        } else {
            // Writing data to temporary file.
            saveFileName = saveFileInfo.fileName ?: "Unknown.ext"
            saveFileNameSuffix = "." + utils.getFileExtension(saveFileName)
            saveFileNamePrefix = saveFileName.dropLast(saveFileNameSuffix.length)
            saveFile = File.createTempFile(saveFileNamePrefix, saveFileNameSuffix)
            saveFile.writeBytes(saveFileInfo.fileData!!)
            destinationSaveFilesInfo.add(
                DestinationSaveFileInfo(
                    file = saveFile,
                    fileName = saveFileName,
                    saveFileNamePrefix = saveFileNamePrefix,
                    saveFileNameSuffix = saveFileNameSuffix,
                    isTempFile = false
                )
            )
        }
    }

    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    if (localOnly) {
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
    }

    utils.applyMimeTypesFilterToIntent(mimeTypesFilter, intent)

    context.startActivityForResult(intent, utils.REQUEST_CODE_SAVE_MULTIPLE_FILES)

    Log.d(LOG_TAG, "saveFile - OUT")

    val end = System.nanoTime()
    println("Elapsed time in nanoseconds: ${end - begin}")

}

// Process single save file.
fun processSingleSaveFile(
    resultCode: Int, data: Intent?, context: Activity
): Boolean {

    val coroutineScope = CoroutineScope(Dispatchers.Main)
    coroutineScope.launch {

        val utils = Utils()

        val begin = System.nanoTime()

        if (resultCode == Activity.RESULT_OK && data?.data != null) {
            val destinationFileUri = data.data
            if (destinationSaveFileInfo != null) {

                val savedFilePath: String? = utils.saveFileOnBackground(
                    destinationSaveFileInfo!!, destinationFileUri!!, fileSavingResult, context
                )

                if (savedFilePath != null) {
                    utils.finishSavingSuccessfully(
                        listOf(savedFilePath), fileSavingResult
                    )
                } else {
                    utils.finishWithError(
                        "file_saving_failed",
                        "saved file path was null",
                        "saved file path was null",
                        fileSavingResult
                    )
                }

            } else {
                utils.finishWithError(
                    "destinationSaveFileInfo_not_found",
                    "destinationSaveFileInfo is null",
                    "destinationSaveFileInfo is null",
                    fileSavingResult
                )
            }
        } else {
            if (destinationSaveFileInfo != null) {
                Log.d(LOG_TAG, "Cancelled")
                if (destinationSaveFileInfo!!.isTempFile) {
                    Log.d(LOG_TAG, "Deleting source file: ${destinationSaveFileInfo!!.file.path}")
                    destinationSaveFileInfo!!.file.delete()
                }
                utils.finishSavingSuccessfully(null, fileSavingResult)
            } else {
                utils.finishWithError(
                    "destinationSaveFileInfo_not_found",
                    "destinationSaveFileInfo is null",
                    "destinationSaveFileInfo is null",
                    fileSavingResult
                )
            }
        }

        val end = System.nanoTime()
        println("Elapsed time in nanoseconds: ${end - begin}")

    }

    return true
}

// Process multiple save file.
fun processMultipleSaveFile(
    resultCode: Int, data: Intent?, context: Activity
): Boolean {

    val coroutineScope = CoroutineScope(Dispatchers.Main)
    fileSaveJob = coroutineScope.launch {

        val utils = Utils()

        val begin = System.nanoTime()

        if (resultCode == Activity.RESULT_OK && data?.data != null) {
            val destinationDirectoryUri = data.data
            if (destinationSaveFilesInfo.isNotEmpty()) {

                val savedFilesPaths: List<String> = utils.saveMultipleFilesOnBackground(
                    destinationSaveFilesInfo, destinationDirectoryUri!!, fileSavingResult, context
                )

                if (savedFilesPaths.isNotEmpty()) {
                    utils.finishSavingSuccessfully(
                        savedFilesPaths, fileSavingResult
                    )
                } else {
                    utils.finishWithError(
                        "files_saving_failed",
                        "saved files paths list was empty",
                        "saved files paths list was empty",
                        fileSavingResult
                    )
                }

            } else {
                utils.finishWithError(
                    "destinationSaveFilesInfo_not_found",
                    "destinationSaveFilesInfo is empty",
                    "destinationSaveFilesInfo is empty",
                    fileSavingResult
                )
            }

        } else {
            if (destinationSaveFilesInfo.isNotEmpty()) {
                Log.d(LOG_TAG, "Cancelled")
                0.until(destinationSaveFilesInfo.size).map { index ->
                    if (destinationSaveFilesInfo.elementAt(index).isTempFile) {
                        val saveFile = destinationSaveFilesInfo.elementAt(index).file
                        Log.d(LOG_TAG, "Deleting temp source file: ${saveFile.path}")
                        saveFile.delete()
                    }
                }
                utils.finishSavingSuccessfully(null, fileSavingResult)
            } else {
                utils.finishWithError(
                    "destinationSaveFilesInfo_not_found",
                    "destinationSaveFilesInfo is empty",
                    "destinationSaveFilesInfo is empty",
                    fileSavingResult
                )
            }
        }

        val end = System.nanoTime()
        println("Elapsed time in nanoseconds: ${end - begin}")

    }

    return true
}