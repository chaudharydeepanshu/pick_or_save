package com.deepanshuchaudhary.pick_or_save

import android.app.Activity

suspend fun cacheFilePathFromUri(
    sourceFileUri: String, context: Activity
): String? {

    val utils = Utils()

    val begin = System.nanoTime()


    val destinationFileName =
        utils.getFileNameFromPickedDocumentUri(utils.getURI(sourceFileUri), context)

    val cachedFilePath: String? = utils.copyFileToCacheDirOnBackground(
        context = context,
        sourceFileUri = utils.getURI(sourceFileUri),
        destinationFileName = destinationFileName ?: "Unknown.ext"
    )

    val end = System.nanoTime()
    println("Elapsed time in nanoseconds: ${end - begin}")

    return cachedFilePath
}
