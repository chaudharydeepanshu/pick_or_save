package com.deepanshuchaudhary.pick_or_save

import android.app.Activity
import android.database.Cursor
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.deepanshuchaudhary.pick_or_save.PickOrSavePlugin.Companion.LOG_TAG
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun fileMetadataFromPathOrUri(
    resultCallback: MethodChannel.Result?, sourceFilePathOrUri: String, context: Activity
) {

    val utils = Utils()

    val contentResolver = context.contentResolver

    val fileMetaData: MutableList<String> = mutableListOf()

    val sourceFileUri = utils.getURI(sourceFilePathOrUri)

    val parsedScheme: String? = sourceFileUri.scheme

    fileMetaData.clear()

    if (parsedScheme == "file") {
        val f = File(sourceFileUri.path!!)
        if (f.exists()) {
            fileMetaData.add(f.name)
            fileMetaData.add(f.length().toString())
            fileMetaData.add(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH).format(
                    Date(f.lastModified())
                )
            )
            utils.finishSuccessfully(fileMetaData, resultCallback)
        } else {
            println("The File does not exist")
            utils.finishSuccessfully(null, resultCallback)
        }
    } else {
        // The query, because it only applies to a single document, returns only
        // one row. There's no need to filter, sort, or select fields,
        // because we want all fields for one document.
        val cursor: Cursor? = contentResolver.query(
            sourceFileUri, null, null, null, null, null
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

                val documentFile = DocumentFile.fromSingleUri(context, sourceFileUri)

                lastModified = if (documentFile != null) {
                    if (documentFile.lastModified() != 0.toLong()) {
                        SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH
                        ).format(
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
            utils.finishSuccessfully(null, resultCallback)
        } else {
            utils.finishSuccessfully(fileMetaData, resultCallback)
        }
    }
}