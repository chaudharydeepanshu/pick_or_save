package com.deepanshuchaudhary.pick_or_save

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

//https://developer.android.com/training/data-storage/shared/documents-files#open-file

private const val LOG_TAG = "PickOrSave"

// Request code for ACTION_OPEN_DOCUMENT.
private const val REQUEST_CODE_PICK_FILE = 1

// Request code for ACTION_CREATE_DOCUMENT.
private const val REQUEST_CODE_SAVE_FILE = 2

// Request code for ACTION_OPEN_DOCUMENT_TREE.
private const val REQUEST_CODE_SAVE_MULTIPLE_FILES = 3

// For deciding what type of file picking dialog to open.
enum class FilePickingType { SINGLE, MULTIPLE }

// For deciding what type of file saving dialog to open.
// FileSavingType.SINGLE allows user to select a location and modify its name before saving.
// FileSavingType.MULTIPLE allows user to select a location where all the files will be saved.
enum class FileSavingType { SINGLE, MULTIPLE }

class PickOrSave(
    private val activity: Activity
) : PluginRegistry.ActivityResultListener {

    private var pendingResult: MethodChannel.Result? = null
    private var fileExtensionsFilter: Array<String>? = null

    // For deciding whether to copy picked files after picking. Defaults to true.
    private var copyPickedFileToCacheDir: Boolean = true

    // Holds provided source files.
    private var sourceFiles: MutableList<File> = mutableListOf()

    // Holds provided file names prefixes. Is empty if no file names are provided.
    private var sourceFilesNamesPrefixes: MutableList<String> = mutableListOf()

    // For deciding whether the source file is temp or not. It is true if source is provided in form of ByteArray and false if provided as file.
    private var isSourceFileTemp: Boolean = false
    private var filePickingType: FilePickingType = FilePickingType.SINGLE
    private var fileSavingType: FileSavingType = FileSavingType.SINGLE

    // For picking single file or multiple files.
    fun pickFile(
        result: MethodChannel.Result,
        fileExtensionsFilter: Array<String>?,
        mimeTypeFilter: Array<String>?,
        localOnly: Boolean,
        copyFileToCacheDir: Boolean,
        filePickingType: FilePickingType
    ) {
        Log.d(
            LOG_TAG,
            "pickFile - IN, fileExtensionsFilter=$fileExtensionsFilter, mimeTypesFilter=$mimeTypeFilter, localOnly=$localOnly, copyFileToCacheDir=$copyFileToCacheDir, filePickingType=$filePickingType"
        )

        if (!setPendingResult(result)) {
            finishWithAlreadyActiveError(result)
            return
        }

        this.fileExtensionsFilter = fileExtensionsFilter
        this.copyPickedFileToCacheDir = copyFileToCacheDir
        this.filePickingType = filePickingType

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        if (localOnly) {
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        }
        if (filePickingType == FilePickingType.MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        intent.type = "*/*"
        applyMimeTypesFilterToIntent(mimeTypeFilter, intent)

        activity.startActivityForResult(intent, REQUEST_CODE_PICK_FILE)

        Log.d(LOG_TAG, "pickFile - OUT")
    }

    // For saving single file or multiple files.
    fun saveFile(
        result: MethodChannel.Result,
        sourceFilesPaths: Array<String>?,
        data: Array<ByteArray>?,
        filesNames: Array<String>?,
        mimeTypesFilter: Array<String>?,
        localOnly: Boolean
    ) {
        Log.d(
            LOG_TAG, "saveFile - IN, sourceFilesPaths=$sourceFilesPaths, " +
                    "data=${data?.size} bytes, filesNames=$filesNames, " +
                    "mimeTypesFilter=$mimeTypesFilter, localOnly=$localOnly"
        )

        if (!setPendingResult(result)) {
            finishWithAlreadyActiveError(result)
            return
        }
        sourceFilesNamesPrefixes.clear()
        sourceFiles.clear()
        fileSavingType =
            if (sourceFilesPaths != null && sourceFilesPaths.isNotEmpty()) (if (sourceFilesPaths.size > 1) FileSavingType.MULTIPLE else FileSavingType.SINGLE) else if (data != null && data.isNotEmpty()) (if (data.size > 1) FileSavingType.MULTIPLE else FileSavingType.SINGLE) else FileSavingType.SINGLE

        if (filesNames != null && filesNames.isNotEmpty()) {
            filesNames.indices.map { index ->
                val fileName = filesNames.elementAt(index)
                val fileNameSuffix = "." + getFileExtension(fileName)
                val fileNamePrefix = fileName.dropLast(fileNameSuffix.length)
                sourceFilesNamesPrefixes.add(fileNamePrefix)
            }
        }

        if (fileSavingType == FileSavingType.MULTIPLE) {
            if (sourceFilesPaths != null && sourceFilesPaths.isNotEmpty()) {
                isSourceFileTemp = false
                // Getting source files.
                sourceFilesPaths.indices.map { index ->
                    val sourceFile = File(sourceFilesPaths.elementAt(index))
                    sourceFiles.add(sourceFile)
                }
                val isSourceFilesExists: Boolean = sourceFiles.all { file -> file.exists() }
                if (!isSourceFilesExists) {
                    finishWithError(
                        "file_not_found",
                        "Source file is missing",
                        sourceFilesPaths.toString()
                    )
                    return
                }
            } else {
                // Writing data to temporary files.
                isSourceFileTemp = true
                0.until(data!!.size).map { index ->
                    val fileName = filesNames!!.elementAt(index)
                    val fileNameSuffix = "." + getFileExtension(fileName)
                    val fileNamePrefix = fileName.dropLast(fileNameSuffix.length)
                    val sourceFile: File = File.createTempFile(fileNamePrefix, fileNameSuffix)
                    sourceFile.writeBytes(data.elementAt(index))
                    sourceFiles.add(sourceFile)
                    Log.d(LOG_TAG, sourceFile.name)
                }
            }

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            if (localOnly) {
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }

            activity.startActivityForResult(intent, REQUEST_CODE_SAVE_MULTIPLE_FILES)
        } else {
            if (sourceFilesPaths != null && sourceFilesPaths.isNotEmpty()) {
                isSourceFileTemp = false
                // Getting source file.
                sourceFiles.add(File(sourceFilesPaths[0]))
                if (!sourceFiles[0].exists()) {
                    finishWithError(
                        "file_not_found",
                        "Source file is missing",
                        sourceFilesPaths[0]
                    )
                    return
                }
            } else {
                // Writing data to temporary file.
                isSourceFileTemp = true
                val fileName = filesNames?.get(0)!!
                val fileNameSuffix = "." + getFileExtension(fileName)
                val fileNamePrefix = fileName.dropLast(fileNameSuffix.length)
                val sourceFile: File = File.createTempFile(fileNamePrefix, fileNameSuffix)
                sourceFile.writeBytes(data?.get(0)!!)
                sourceFiles.add(sourceFile)
            }

            val sourceFileNamePrefix = if (sourceFilesNamesPrefixes.isNotEmpty()) {
                sourceFilesNamesPrefixes[0]
            } else {
                val sourceFileName = sourceFiles[0].name
                val fileNameSuffix = "." + getFileExtension(sourceFileName)
                sourceFileName.dropLast(fileNameSuffix.length)
            }

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(Intent.EXTRA_TITLE, sourceFileNamePrefix)
            if (localOnly) {
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }

            // Setting mimeType for file saving dialog to avoid adding extension manually when saving.
            val sourceFileMimeType =
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(sourceFiles[0].extension)
            intent.type = sourceFileMimeType ?: "*/*"

            applyMimeTypesFilterToIntent(mimeTypesFilter, intent)

            activity.startActivityForResult(intent, REQUEST_CODE_SAVE_FILE)
        }
        Log.d(LOG_TAG, "saveFile - OUT")
    }

    // For picking single file or multiple files.
    fun fileMetaData(
        result: MethodChannel.Result,
        sourceFilePath: String?,
    ) {
        Log.d(
            LOG_TAG,
            "fileMetaData - IN, sourceFilePath=$sourceFilePath"
        )

        if (!setPendingResult(result)) {
            finishWithAlreadyActiveError(result)
            return
        }

        val contentResolver = activity.contentResolver

        val fileMetaData: MutableList<String> = mutableListOf()


        // The query, because it only applies to a single document, returns only
        // one row. There's no need to filter, sort, or select fields,
        // because we want all fields for one document.
        val cursor: Cursor? = contentResolver.query(
            Uri.parse(sourceFilePath), null, null, null, null, null
        )

        cursor?.use {
            // moveToFirst() returns false if the cursor has 0 rows. Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (it.moveToFirst()) {

                // Note it's called "Display Name". This is
                // provider-specific, and might not necessarily be the file name.
                val displayNameIndex: Int = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val displayName: String = if (displayNameIndex >= 0) {
                    it.getString(displayNameIndex)
                } else {
                    "Unknown"
                }
                Log.i(LOG_TAG, "Display Name: $displayName")

                fileMetaData.add(displayName)

                val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)

                // If the size is unknown, the value stored is null. But because an
                // int can't be null, the behavior is implementation-specific,
                // and unpredictable. So as
                // a rule, check if it's null before assigning to an int. This will
                // happen often: The storage API allows for remote files, whose
                // size might not be locally known.
                val size: String = if (!it.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    it.getString(sizeIndex)
                } else {
                    "Unknown"
                }
                Log.i(LOG_TAG, "Size: $size")

                fileMetaData.add(size)

                val lastModified: String

                val documentFile = DocumentFile.fromSingleUri(activity, Uri.parse(sourceFilePath))

                lastModified = if (documentFile != null) {
                    if (documentFile.lastModified() != 0.toLong()) {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH).format(
                            Date(documentFile.lastModified())
                        )
                    } else {
                        "Unknown"
                    }
                } else {
                    "Unknown"
                }

                Log.i(LOG_TAG, "LastModified: $lastModified")

                fileMetaData.add(lastModified)

            }
        }

        if (fileMetaData.size != 3) {
            finishSuccessfully(null)
        } else {
            finishSuccessfully(fileMetaData)
        }

        Log.d(LOG_TAG, "fileMetaData - OUT")
    }

    private fun applyMimeTypesFilterToIntent(mimeTypesFilter: Array<String>?, intent: Intent) {
        if (mimeTypesFilter != null) {
//            if (mimeTypesFilter.size == 1) {
//                intent.type = mimeTypesFilter.first()
//            } else {
//                intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypesFilter)
//            }
        }
//        else {
//            intent.type = "*/*"
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        when (requestCode) {
            REQUEST_CODE_PICK_FILE -> {
                if (resultCode == Activity.RESULT_OK && data?.data != null) {
                    val sourceFileUri = data.data
                    Log.d(LOG_TAG, "Picked file: $sourceFileUri")
                    val destinationFileName = getFileNameFromPickedDocumentUri(sourceFileUri)
                    if (destinationFileName != null && validateFileExtension(destinationFileName)) {
                        if (copyPickedFileToCacheDir) {
                            copyFileToCacheDirOnBackground(
                                context = activity,
                                sourceFileUri = sourceFileUri!!,
                                destinationFileName = destinationFileName
                            )
                        } else {
                            finishSuccessfully(listOf(sourceFileUri!!.toString()))
                        }
                    } else {
                        finishWithError(
                            "invalid_file_extension",
                            "Invalid file type was picked",
                            getFileExtension(destinationFileName)
                        )
                    }
                } else if (resultCode == Activity.RESULT_OK && data?.clipData != null) {
                    val sourceFileUris = 0.until(data.clipData!!.itemCount).map { index ->
                        data.clipData!!.getItemAt(index).uri
                    }
                    Log.d(LOG_TAG, "Picked files: $sourceFileUris")
                    val destinationFilesNames = 0.until(sourceFileUris.size).map { index ->
                        getFileNameFromPickedDocumentUri(sourceFileUris.elementAt(index))
                    }
                    val isFilesTypeValid: Boolean = destinationFilesNames.all { fileName ->
                        fileName != null && validateFileExtension(fileName)
                    }
                    if (isFilesTypeValid) {
                        if (copyPickedFileToCacheDir) {
                            copyMultipleFilesToCacheDirOnBackground(
                                context = activity,
                                sourceFileUris = sourceFileUris,
                                destinationFilesNames = destinationFilesNames.filterNotNull()
                            )
                        } else {
                            finishSuccessfully(sourceFileUris.map { uri -> uri.toString() })
                        }
                    } else {
                        val invalidFilesTypes: MutableList<String?> = mutableListOf()

                        0.until(destinationFilesNames.size).map { index ->
                            val fileName = destinationFilesNames.elementAt(index)
                            if (fileName == null || !validateFileExtension(fileName)) {
                                invalidFilesTypes.add(
                                    getFileExtension(
                                        destinationFilesNames.elementAt(
                                            index
                                        )
                                    )
                                )
                            }
                        }
                        finishWithError(
                            "invalid_file_extension",
                            "Invalid file type was picked",
                            invalidFilesTypes.toString()
                        )
                    }
                } else {
                    Log.d(LOG_TAG, "Cancelled")
                    finishSuccessfully(null)
                }
                return true
            }
            REQUEST_CODE_SAVE_FILE -> {
                if (resultCode == Activity.RESULT_OK && data?.data != null) {
                    val destinationFileUri = data.data
                    saveFileOnBackground(sourceFiles[0], destinationFileUri!!)
                } else {
                    Log.d(LOG_TAG, "Cancelled")
                    if (isSourceFileTemp) {
                        Log.d(LOG_TAG, "Deleting source file: ${sourceFiles[0].path}")
                        sourceFiles[0].delete()
                    }
                    finishSuccessfully(null)
                }
                return true
            }
            REQUEST_CODE_SAVE_MULTIPLE_FILES -> {
                if (resultCode == Activity.RESULT_OK && data?.data != null) {
                    val destinationDirectoryUri = data.data
                    saveMultipleFilesOnBackground(
                        sourceFiles,
                        sourceFilesNamesPrefixes,
                        destinationDirectoryUri!!
                    )
                } else {
                    Log.d(LOG_TAG, "Cancelled")
                    if (isSourceFileTemp) {
                        Log.d(LOG_TAG, "Deleting source file: $sourceFiles")
                        0.until(sourceFiles.size).map { index ->
                            val sourceFile = sourceFiles.elementAt(index)
                            sourceFile.delete()
                        }
                    }
                    finishSuccessfully(null)
                }
                return true
            }
            else -> return false
        }
    }

    private fun copyMultipleFilesToCacheDirOnBackground(
        context: Context,
        sourceFileUris: List<Uri>,
        destinationFilesNames: List<String>
    ) {
        val uiScope = CoroutineScope(Dispatchers.Main)
        uiScope.launch {
            try {
                Log.d(LOG_TAG, "Launch...")
                Log.d(LOG_TAG, "Copy on background...")
                val filesPaths: MutableList<String> = mutableListOf()
                0.until(destinationFilesNames.size).map { index ->
                    val destinationFileName = destinationFilesNames.elementAt(index)
                    val sourceFileUri = sourceFileUris.elementAt(index)
                    filesPaths.add(withContext(Dispatchers.IO) {
                        copyFileToCacheDir(context, sourceFileUri, destinationFileName)
                    })
                }
                Log.d(LOG_TAG, "...copied on background, result: $filesPaths")
                finishSuccessfully(filesPaths)
                Log.d(LOG_TAG, "...launch")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "copyFileToCacheDirOnBackground failed", e)
                finishWithError("file_copy_failed", e.localizedMessage, e.toString())
            }
        }
    }

    private fun copyFileToCacheDirOnBackground(
        context: Context,
        sourceFileUri: Uri,
        destinationFileName: String
    ) {
        val uiScope = CoroutineScope(Dispatchers.Main)
        uiScope.launch {
            try {
                Log.d(LOG_TAG, "Launch...")
                Log.d(LOG_TAG, "Copy on background...")
                val filePath = withContext(Dispatchers.IO) {
                    copyFileToCacheDir(context, sourceFileUri, destinationFileName)
                }
                Log.d(LOG_TAG, "...copied on background, result: $filePath")
                finishSuccessfully(listOf(filePath))
                Log.d(LOG_TAG, "...launch")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "copyFileToCacheDirOnBackground failed", e)
                finishWithError("file_copy_failed", e.localizedMessage, e.toString())
            }
        }
    }

    private fun copyFileToCacheDir(
        context: Context,
        sourceFileUri: Uri,
        destinationFileName: String
    ): String {
        // Getting destination file on cache directory.
        val destinationFile = File(context.cacheDir.path, destinationFileName)

        // Deleting existing destination file.
        if (destinationFile.exists()) {
            Log.d(LOG_TAG, "Deleting existing destination file '${destinationFile.path}'")
            destinationFile.delete()
        }

        // Copying file to cache directory.
        Log.d(LOG_TAG, "Copying '$sourceFileUri' to '${destinationFile.path}'")
        var copiedBytes: Long
        context.contentResolver.openInputStream(sourceFileUri).use { inputStream ->
            destinationFile.outputStream().use { outputStream ->
                copiedBytes = inputStream!!.copyTo(outputStream)
            }
        }

        Log.d(
            LOG_TAG,
            "Successfully copied file to '${destinationFile.absolutePath}, bytes=$copiedBytes'"
        )

        return destinationFile.absolutePath
    }

    private fun getFileNameFromPickedDocumentUri(uri: Uri?): String? {
        if (uri == null) {
            return null
        }
        var fileName: String? = null
        activity.contentResolver.query(uri, null, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                fileName =
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return cleanupFileName(fileName)
    }

    private fun cleanupFileName(fileName: String?): String? {
        // https://stackoverflow.com/questions/2679699/what-characters-allowed-in-file-names-on-android
        return fileName?.replace(Regex("[\\\\/:*?\"<>|\\[\\]]"), "_")
    }

    private fun getFileExtension(fileName: String?): String {
        return fileName?.substringAfterLast('.', "") ?: ""
    }

    private fun validateFileExtension(filePath: String): Boolean {
        val validFileExtensions = fileExtensionsFilter
        if (validFileExtensions == null || validFileExtensions.isEmpty()) {
            return true
        }
        val fileExtension = getFileExtension(filePath)
        for (extension in validFileExtensions) {
            if (fileExtension.equals(extension, true)) {
                return true
            }
        }
        return false
    }

    private fun saveMultipleFilesOnBackground(
        sourceFiles: List<File>,
        sourceFilesNamesPrefixes: List<String>,
        destinationDirectoryUri: Uri
    ) {
        val uiScope = CoroutineScope(Dispatchers.Main)
        uiScope.launch {
            try {
                Log.d(LOG_TAG, "Saving file on background...")
                val outputFolder: DocumentFile? = DocumentFile.fromTreeUri(
                    activity,
                    destinationDirectoryUri
                )
                val filesPaths: MutableList<String> = mutableListOf()
                0.until(sourceFiles.size).map { index ->
                    val sourceFile = sourceFiles.elementAt(index)
                    val sourceFileMimeType =
                        MimeTypeMap.getSingleton().getMimeTypeFromExtension(sourceFile.extension)
                    val sourceFileNamePrefix: String = if (sourceFilesNamesPrefixes.isEmpty()) {
                        val sourceFileExtension = "." + sourceFiles.elementAt(index).extension
                        sourceFiles.elementAt(index).name.toString()
                            .dropLast(sourceFileExtension.length)
                    } else {
                        sourceFilesNamesPrefixes.elementAt(index)
                    }
                    val documentFileNewFile = outputFolder!!.createFile(
                        sourceFileMimeType ?: "application/random",
                        sourceFileNamePrefix
                    )
                    val destinationFileUri: Uri = documentFileNewFile!!.uri
                    filesPaths.add(withContext(Dispatchers.IO) {
                        saveFile(sourceFile, destinationFileUri)
                    })
                }
                Log.d(LOG_TAG, "...saved file on background, result: $filesPaths")
                finishSuccessfully(filesPaths)
            } catch (e: SecurityException) {
                Log.e(LOG_TAG, "saveFileOnBackground", e)
                finishWithError("security_exception", e.localizedMessage, e.toString())
            } catch (e: Exception) {
                Log.e(LOG_TAG, "saveFileOnBackground failed", e)
                finishWithError("save_file_failed", e.localizedMessage, e.toString())
            } finally {
                if (isSourceFileTemp) {
                    Log.d(LOG_TAG, "Deleting source files: $sourceFiles")
                    0.until(sourceFiles.size).map { index ->
                        val sourceFile = sourceFiles.elementAt(index)
                        sourceFile.delete()
                    }
                }
            }
        }
    }

    private fun saveFileOnBackground(
        sourceFile: File,
        destinationFileUri: Uri
    ) {
        val uiScope = CoroutineScope(Dispatchers.Main)
        uiScope.launch {
            try {
                Log.d(LOG_TAG, "Saving file on background...")
                val filePath = withContext(Dispatchers.IO) {
                    saveFile(sourceFile, destinationFileUri)
                }
                Log.d(LOG_TAG, "...saved file on background, result: $filePath")
                finishSuccessfully(listOf(filePath))
            } catch (e: SecurityException) {
                Log.e(LOG_TAG, "saveFileOnBackground", e)
                finishWithError("security_exception", e.localizedMessage, e.toString())
            } catch (e: Exception) {
                Log.e(LOG_TAG, "saveFileOnBackground failed", e)
                finishWithError("save_file_failed", e.localizedMessage, e.toString())
            } finally {
                if (isSourceFileTemp) {
                    Log.d(LOG_TAG, "Deleting source file: ${sourceFile.path}")
                    sourceFile.delete()
                }
            }
        }
    }

    private fun saveFile(
        sourceFile: File,
        destinationFileUri: Uri
    ): String {
        Log.d(LOG_TAG, "Saving file '${sourceFile.path}' to '${destinationFileUri.path}'")
        sourceFile.inputStream().use { inputStream ->
            activity.contentResolver.openOutputStream(destinationFileUri).use { outputStream ->
                outputStream as java.io.FileOutputStream
                outputStream.channel.truncate(0)
                inputStream.copyTo(outputStream)
            }
        }
        Log.d(LOG_TAG, "Saved file to '${destinationFileUri.path}'")
        return destinationFileUri.path!!
    }

    private fun setPendingResult(
        result: MethodChannel.Result
    ): Boolean {
        if (pendingResult != null) {
            return false
        }
        pendingResult = result
        return true
    }

    private fun clearPendingResult() {
        pendingResult = null
    }

    private fun finishWithAlreadyActiveError(result: MethodChannel.Result) {
        result.error("already_active", "File dialog is already active", null)
    }

    private fun finishSuccessfully(filesPaths: List<String>?) {
        pendingResult?.success(filesPaths)
        clearPendingResult()
    }

    private fun finishWithError(errorCode: String, errorMessage: String?, errorDetails: String?) {
        pendingResult?.error(errorCode, errorMessage, errorDetails)
        clearPendingResult()
    }
}
