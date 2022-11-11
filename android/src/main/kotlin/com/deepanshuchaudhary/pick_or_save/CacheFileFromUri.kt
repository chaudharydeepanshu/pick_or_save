package com.deepanshuchaudhary.pick_or_save

import android.app.Activity
import io.flutter.plugin.common.MethodChannel

suspend fun cacheFilePathFromPathOrUri(
    sourceFilePathOrUri: String, context: Activity, resultCallback: MethodChannel.Result?,
): String? {

    val utils = Utils()

    val begin = System.nanoTime()


    val destinationFileName =
        utils.getFileNameFromPickedDocumentUri(utils.getURI(sourceFilePathOrUri), context)

    val cachedFilePath: String? = utils.copyFileToCacheDirOnBackground(
        context = context,
        sourceFileUri = utils.getURI(sourceFilePathOrUri),
        destinationFileName = destinationFileName ?: "Unknown.ext",
        resultCallback = resultCallback
    )

    val end = System.nanoTime()
    println("Elapsed time in nanoseconds: ${end - begin}")

    return cachedFilePath
}
