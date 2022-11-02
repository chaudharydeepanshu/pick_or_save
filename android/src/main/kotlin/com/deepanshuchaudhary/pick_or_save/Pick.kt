package com.deepanshuchaudhary.pick_or_save

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.deepanshuchaudhary.pick_or_save.PickOrSavePlugin.Companion.LOG_TAG
import io.flutter.plugin.common.MethodChannel

private var validFileExtensions: List<String>? = null
private var copyPickedFileToCacheDir: Boolean = true

// For picking single file.
fun pickSingleFile(
    resultCallback: MethodChannel.Result,
    allowedExtensions: List<String>,
    mimeTypeFilter: List<String>,
    localOnly: Boolean,
    copyFileToCacheDir: Boolean,
    context: Activity,
) {

    val updatedMimeTypeFilter = mimeTypeFilter.toMutableList()

    allowedExtensions.forEach {
        val mimeTypeFromExtension: String? = MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
        if (mimeTypeFromExtension != null) {
            updatedMimeTypeFilter.add(mimeTypeFromExtension)
        }
    }

    val utils = Utils()

    if (filePickingResult != null) {
        utils.finishWithAlreadyActiveError(resultCallback)
        return
    } else if (fileSavingResult != null) {
        utils.finishWithAlreadyActiveError(resultCallback)
        return
    } else {
        filePickingResult = resultCallback
    }

    val begin = System.nanoTime()

    validFileExtensions = allowedExtensions
    copyPickedFileToCacheDir = copyFileToCacheDir

    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    if (localOnly) {
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
    }

    intent.type = "*/*"

    if (updatedMimeTypeFilter.isNotEmpty()) {
        utils.applyMimeTypesFilterToIntent(updatedMimeTypeFilter, intent)
    }

    context.startActivityForResult(intent, utils.REQUEST_CODE_PICK_FILE)

    Log.d(LOG_TAG, "pickFile - OUT")

    val end = System.nanoTime()
    println("Elapsed time in nanoseconds: ${end - begin}")

}

// For picking multiple file.
fun pickMultipleFiles(
    resultCallback: MethodChannel.Result,
    allowedExtensions: List<String>,
    mimeTypeFilter: List<String>,
    localOnly: Boolean,
    copyFileToCacheDir: Boolean,
    context: Activity,
) {

    val updatedMimeTypeFilter = mimeTypeFilter.toMutableList()

    allowedExtensions.forEach {
        val mimeTypeFromExtension: String? = MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
        if (mimeTypeFromExtension != null) {
            updatedMimeTypeFilter.add(mimeTypeFromExtension)
        }
    }

    val utils = Utils()

    if (filePickingResult != null) {
        utils.finishWithAlreadyActiveError(resultCallback)
        return
    } else if (fileSavingResult != null) {
        utils.finishWithAlreadyActiveError(resultCallback)
        return
    } else {
        filePickingResult = resultCallback
    }

    val begin = System.nanoTime()

    validFileExtensions = allowedExtensions
    copyPickedFileToCacheDir = copyFileToCacheDir

    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    if (localOnly) {
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
    }

    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

    intent.type = "*/*"

    if (updatedMimeTypeFilter.isNotEmpty()) {
        utils.applyMimeTypesFilterToIntent(updatedMimeTypeFilter, intent)
    }

    context.startActivityForResult(intent, utils.REQUEST_CODE_PICK_FILE)

    Log.d(LOG_TAG, "pickFile - OUT")

    val end = System.nanoTime()
    println("Elapsed time in nanoseconds: ${end - begin}")

}

// Process picking file.
fun processPickedFiles(
    resultCode: Int, data: Intent?, context: Activity
): Boolean {

    val utils = Utils()

    val begin = System.nanoTime()

    if (resultCode == Activity.RESULT_OK && data?.data != null) {
        val sourceFileUri = data.data
        Log.d(LOG_TAG, "Picked file: $sourceFileUri")
        val destinationFileName = utils.getFileNameFromPickedDocumentUri(sourceFileUri, context)
        if ((destinationFileName != null) && utils.validateFileExtension(
                destinationFileName, validFileExtensions?.toTypedArray()
            )
        ) {
            if (copyPickedFileToCacheDir) {
                val cachedFilePath: String? = utils.copyFileToCacheDirOnBackground(
                    context = context,
                    sourceFileUri = sourceFileUri!!,
                    destinationFileName = destinationFileName,
                )
                if (cachedFilePath != null) {
                    utils.finishSuccessfully(
                        listOf(cachedFilePath), filePickingResult
                    )
                } else {
                    utils.finishWithError(
                        "file_caching_failed",
                        "cached file path was null",
                        "cached file path was null",
                        filePickingResult
                    )
                }
            } else {
                utils.finishSuccessfully(
                    listOf(sourceFileUri!!.toString()), filePickingResult
                )
            }
        } else {
            utils.finishWithError(
                "invalid_file_extension",
                "Invalid file type was picked",
                utils.getFileExtension(destinationFileName),
                filePickingResult
            )
        }
    } else if (resultCode == Activity.RESULT_OK && data?.clipData != null) {
        val sourceFileUris = 0.until(data.clipData!!.itemCount).map { index ->
            data.clipData!!.getItemAt(index).uri
        }
        Log.d(LOG_TAG, "Picked files: $sourceFileUris")
        val invalidFilesUris = mutableListOf<Uri>()
        val validFilesUris = mutableListOf<Uri>()
        val destinationFilesNames = sourceFileUris.indices.map { index ->

            val destinationFileName =
                utils.getFileNameFromPickedDocumentUri(sourceFileUris.elementAt(index), context)

            val isFileTypeValid = destinationFileName != null && utils.validateFileExtension(
                destinationFileName, validFileExtensions?.toTypedArray()
            )

            if (isFileTypeValid) {
                validFilesUris.add(sourceFileUris.elementAt(index))
            } else {
                invalidFilesUris.add(sourceFileUris.elementAt(index))
            }

            destinationFileName

        }

        if (copyPickedFileToCacheDir) {

            val cachedFilesPaths: List<String>? = utils.copyMultipleFilesToCacheDirOnBackground(
                context = context,
                sourceFileUris = validFilesUris,
                destinationFilesNames = validFilesUris.indices.mapNotNull { index ->
                    utils.getFileNameFromPickedDocumentUri(
                        validFilesUris.elementAt(index), context
                    )
                },
            )
            if (cachedFilesPaths != null) {
                utils.finishSuccessfully(
                    cachedFilesPaths, filePickingResult
                )
            } else {
                utils.finishWithError(
                    "files_caching_failed",
                    "cached files paths list was null",
                    "cached files paths list was null",
                    filePickingResult
                )
            }


        } else {
            utils.finishSuccessfully(
                validFilesUris.map { uri -> uri.toString() }, filePickingResult
            )
        }

        val invalidFilesTypes: MutableList<String?> = mutableListOf()

        destinationFilesNames.indices.map { index ->
            val fileName = destinationFilesNames.elementAt(index)
            if (fileName == null || !utils.validateFileExtension(
                    fileName, validFileExtensions?.toTypedArray()
                )
            ) {
                invalidFilesTypes.add(
                    utils.getFileExtension(
                        destinationFilesNames.elementAt(
                            index
                        )
                    )
                )
            }
        }
        Log.d(LOG_TAG, "Invalid file type was picked $invalidFilesTypes")
//                        finishWithError(
//                            "invalid_file_extension",
//                            "Invalid file type was picked",
//                            invalidFilesTypes.toString()
//                        )

    } else {
        Log.d(LOG_TAG, "Cancelled")
        utils.finishSuccessfully(null, filePickingResult)
    }

    val end = System.nanoTime()
    println("Elapsed time in nanoseconds: ${end - begin}")

    return true
}