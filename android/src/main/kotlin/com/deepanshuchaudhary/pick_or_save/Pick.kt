package com.deepanshuchaudhary.pick_or_save

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.DocumentsContract.EXTRA_INITIAL_URI
import android.provider.MediaStore.*
import android.util.Log
import android.webkit.MimeTypeMap
import com.deepanshuchaudhary.pick_or_save.PickOrSavePlugin.Companion.LOG_TAG
import kotlinx.coroutines.*

private var validFileExtensions: List<String>? = null
private var copyPickedFileToCacheDir: Boolean = true

var directoryDocumentsPickerJob: Job? = null

// For picking files under a directory.
fun pickDocumentsFromDirectoryUri(
    docId: String?,
    directoryUri: String,
    recurseDirectories: Boolean,
    allowedExtensions: List<String>,
    mimeTypesFilter: List<String>,
    context: Activity,
) {

    val utils = Utils()

    val begin = System.nanoTime()

    val uiScope = CoroutineScope(Dispatchers.IO)
    directoryDocumentsPickerJob = uiScope.launch {

        val updatedMimeTypeFilter = mimeTypesFilter.toMutableList()

        allowedExtensions.forEach {
            yield()
            val mimeTypeFromExtension: String? =
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
            if (mimeTypeFromExtension != null) {
                updatedMimeTypeFilter.add(mimeTypeFromExtension)
            }
        }

        val contentResolver = context.contentResolver

        val directoryFilesUris = mutableListOf<List<Any?>>()

        val treeUri = utils.getURI(directoryUri.trim())

// Not using DocumentFile method as it is very slow in compare with DocumentsContract.
// Get directory tree.
//    val documentsTree: DocumentFile? = DocumentFile.fromTreeUri(context, treeUri)
//
//    val childDocuments = documentsTree!!.listFiles()
//
//    for (childDocument in childDocuments) {
//        directoryFilesUris.add(childDocument.uri.toString())
//    }

        // get children uri from the tree uri
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri, docId ?: DocumentsContract.getTreeDocumentId(treeUri)
        )

        // Keep track of our directory hierarchy.
        val dirNodes: MutableList<Uri> = mutableListOf()
        dirNodes.add(childrenUri)

        while (dirNodes.isNotEmpty()) {
            yield()
            // get the item from top
            val childrenUriFromDirNodes = dirNodes.first()
            dirNodes.removeAt(0)
            var c: Cursor? = null
            try {
                c = contentResolver.query(
                    childrenUriFromDirNodes, arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
//                        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
//                        DocumentsContract.Document.COLUMN_SIZE,
                    ), null, null, null
                )
                if (c != null) {
                    while (c.moveToNext()) {
                        yield()
                        val documentId: String = c.getString(0)
                        val name: String = c.getString(1)
                        val mime: String = c.getString(2)
//                        val lastModified: String = c.getString(3)
//                        val size: String = c.getString(4)
                        val isDirectory: Boolean = DocumentsContract.Document.MIME_TYPE_DIR == mime
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                            treeUri, documentId
                        )
                        if (isDirectory && recurseDirectories) {
                            val newNode: Uri = DocumentsContract.buildChildDocumentsUriUsingTree(
                                documentUri, documentId
                            )
                            dirNodes.add(newNode)
                        }
                        var isMimeSupported = false

                        for (it in updatedMimeTypeFilter) {
                            if (it == mime) {
                                isMimeSupported = true
                                break
                            }

                            val splitParts = it.split("/").toList()

                            if (splitParts.size > 1 && (splitParts[1].trim() == "*" || splitParts[1].trim() == "")) {
                                if (mime.startsWith(splitParts[0])) {
                                    isMimeSupported = true
                                    break
                                }
                            }
                        }

                        if (updatedMimeTypeFilter.isEmpty() || isMimeSupported) {
                            directoryFilesUris.add(
                                listOf(
                                    documentId,
                                    documentUri.toString(),
                                    mime,
                                    name,
                                    isDirectory,
                                    !isDirectory,
                                )
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                Log.w(LOG_TAG, "Failed query: $e")
            } finally {
                c?.close()
            }
        }

        utils.finishSuccessfully(
            directoryFilesUris, directoryDocumentsPickingResult
        )

        Log.d(LOG_TAG, "pickDocumentsFromDirectoryUri - OUT")

        val end = System.nanoTime()
        println("Elapsed time in nanoseconds: ${end - begin}")
    }

}

// For picking single directory.
fun pickSingleDirectory(
    initialDirectoryUri: String?,
    context: Activity,
) {

    val utils = Utils()

    val begin = System.nanoTime()

    // Choose a directory using the system's file picker.
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

    // Optionally, specify a URI for the directory that should be opened in
    // the system file picker when it loads.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (initialDirectoryUri != null) {
            val initialUri = utils.getURI(initialDirectoryUri.trim())

            intent.putExtra(EXTRA_INITIAL_URI, initialUri)
        }
    }

    context.startActivityForResult(intent, utils.REQUEST_CODE_ACTION_OPEN_DOCUMENT_TREE)

    Log.d(LOG_TAG, "pickSingleDirectory - OUT")

    val end = System.nanoTime()
    println("Elapsed time in nanoseconds: ${end - begin}")

}

// Process picking directory.
fun processPickedDirectory(
    resultCode: Int, data: Intent?, context: Activity,
): Boolean {

    val uiScope = CoroutineScope(Dispatchers.IO)
    uiScope.launch {

        val utils = Utils()

        val begin = System.nanoTime()

        if (resultCode == Activity.RESULT_OK && data?.data != null) {
            val sourceFileUri = data.data!!
            Log.d(LOG_TAG, "Picked directory: $sourceFileUri")

            // https://developer.android.com/training/data-storage/shared/documents-files
            // Persist read and write permissions.
            val contentResolver = context.contentResolver
            contentResolver.takePersistableUriPermission(
                sourceFileUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            utils.finishSuccessfully(
                listOf(sourceFileUri.toString()), filePickingResult
            )
        } else {
            Log.d(LOG_TAG, "Cancelled")
            utils.finishSuccessfully(null, filePickingResult)
        }

        val end = System.nanoTime()
        println("Elapsed time in nanoseconds: ${end - begin}")

    }
    return true
}

// For picking photo.
@SuppressLint("NewApi")
fun pickSingleOrMultiplePhoto(
    allowedExtensions: List<String>,
    photoPickerMimeType: String,
    copyFileToCacheDir: Boolean,
    enableMultipleSelection: Boolean,
    context: Activity,
) {

    val utils = Utils()

    val begin = System.nanoTime()

    validFileExtensions = allowedExtensions
    copyPickedFileToCacheDir = copyFileToCacheDir

    val intent = Intent(ACTION_PICK_IMAGES)

    if (enableMultipleSelection) {
        val maxSelectionAllowed = getPickImagesMaxLimit()
        intent.putExtra(EXTRA_PICK_IMAGES_MAX, maxSelectionAllowed)
    }

    intent.type = photoPickerMimeType

    context.startActivityForResult(intent, utils.REQUEST_CODE_ACTION_OPEN_DOCUMENT)

    Log.d(LOG_TAG, "pickSingleOrMultiplePhoto - OUT")

    val end = System.nanoTime()
    println("Elapsed time in nanoseconds: ${end - begin}")

}

// For picking single file.
fun pickSingleFile(
    allowedExtensions: List<String>,
    mimeTypesFilter: List<String>,
    localOnly: Boolean,
    copyFileToCacheDir: Boolean,
    context: Activity,
) {

    val updatedMimeTypeFilter = mimeTypesFilter.toMutableList()

    allowedExtensions.forEach {
        val mimeTypeFromExtension: String? = MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
        if (mimeTypeFromExtension != null) {
            updatedMimeTypeFilter.add(mimeTypeFromExtension)
        }
    }

    val utils = Utils()

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

    context.startActivityForResult(intent, utils.REQUEST_CODE_ACTION_OPEN_DOCUMENT)

    Log.d(LOG_TAG, "pickFile - OUT")

    val end = System.nanoTime()
    println("Elapsed time in nanoseconds: ${end - begin}")

}

// For picking multiple file.
fun pickMultipleFiles(
    allowedExtensions: List<String>,
    mimeTypesFilter: List<String>,
    localOnly: Boolean,
    copyFileToCacheDir: Boolean,
    context: Activity,
) {

    val updatedMimeTypeFilter = mimeTypesFilter.toMutableList()

    allowedExtensions.forEach {
        val mimeTypeFromExtension: String? = MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
        if (mimeTypeFromExtension != null) {
            updatedMimeTypeFilter.add(mimeTypeFromExtension)
        }
    }

    val utils = Utils()

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

    context.startActivityForResult(intent, utils.REQUEST_CODE_ACTION_OPEN_DOCUMENT)

    Log.d(LOG_TAG, "pickFile - OUT")

    val end = System.nanoTime()
    println("Elapsed time in nanoseconds: ${end - begin}")

}

// Process picking file.
fun processPickedFiles(
    resultCode: Int, data: Intent?, context: Activity
): Boolean {

    val uiScope = CoroutineScope(Dispatchers.Main)
    uiScope.launch {

        val utils = Utils()

        val begin = System.nanoTime()

        if (resultCode == Activity.RESULT_OK && data?.data != null) {
            val sourceFileUri = data.data!!
            Log.d(LOG_TAG, "Picked file: $sourceFileUri")
            val destinationFileName = utils.getFileNameFromPickedDocumentUri(sourceFileUri, context)
            if ((destinationFileName != null) && utils.validateFileExtension(
                    destinationFileName, validFileExtensions?.toTypedArray()
                )
            ) {
                if (copyPickedFileToCacheDir) {
                    val cachedFilePath: String? = utils.copyFileToCacheDirOnBackground(
                        context = context,
                        sourceFileUri = sourceFileUri,
                        destinationFileName = destinationFileName,
                        resultCallback = filePickingResult,
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
                        listOf(sourceFileUri.toString()), filePickingResult
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
                    resultCallback = filePickingResult,
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

    }
    return true
}