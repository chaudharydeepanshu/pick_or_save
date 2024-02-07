package com.deepanshuchaudhary.pick_or_save

import android.util.Log

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** PickOrSavePlugin */
class PickOrSavePlugin : FlutterPlugin, ActivityAware, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    private var pickOrSave: PickOrSave? = null
    private var pluginBinding: FlutterPlugin.FlutterPluginBinding? = null
    private var activityBinding: ActivityPluginBinding? = null

    companion object {
        const val LOG_TAG = "PickOrSavePlugin"
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(LOG_TAG, "onAttachedToEngine - IN")

        if (pluginBinding != null) {
            Log.w(LOG_TAG, "onAttachedToEngine - already attached")
        }

        pluginBinding = flutterPluginBinding

        val messenger = pluginBinding?.binaryMessenger
        doOnAttachedToEngine(messenger!!)

        Log.d(LOG_TAG, "onAttachedToEngine - OUT")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        Log.d(LOG_TAG, "onDetachedFromEngine")
        doOnDetachedFromEngine()
    }

    // note: this may be called multiple times on app startup
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.d(LOG_TAG, "onAttachedToActivity")
        doOnAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        Log.d(LOG_TAG, "onDetachedFromActivity")
        doOnDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Log.d(LOG_TAG, "onReattachedToActivityForConfigChanges")
        doOnAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Log.d(LOG_TAG, "onDetachedFromActivityForConfigChanges")
        doOnDetachedFromActivity()
    }

    private fun doOnAttachedToEngine(messenger: BinaryMessenger) {
        Log.d(LOG_TAG, "doOnAttachedToEngine - IN")

        this.channel = MethodChannel(messenger, "pick_or_save")
        this.channel.setMethodCallHandler(this)

        Log.d(LOG_TAG, "doOnAttachedToEngine - OUT")
    }

    private fun doOnDetachedFromEngine() {
        Log.d(LOG_TAG, "doOnDetachedFromEngine - IN")

        if (pluginBinding == null) {
            Log.w(LOG_TAG, "doOnDetachedFromEngine - already detached")
        }
        pluginBinding = null

        this.channel.setMethodCallHandler(null)

        Log.d(LOG_TAG, "doOnDetachedFromEngine - OUT")
    }

    private fun doOnAttachedToActivity(activityBinding: ActivityPluginBinding?) {
        Log.d(LOG_TAG, "doOnAttachedToActivity - IN")

        this.activityBinding = activityBinding

        Log.d(LOG_TAG, "doOnAttachedToActivity - OUT")
    }

    private fun doOnDetachedFromActivity() {
        Log.d(LOG_TAG, "doOnDetachedFromActivity - IN")

        if (pickOrSave != null) {
            activityBinding?.removeActivityResultListener(pickOrSave!!)
            pickOrSave = null
        }
        activityBinding = null

        Log.d(LOG_TAG, "doOnDetachedFromActivity - OUT")
    }


    override fun onMethodCall(call: MethodCall, result: Result) {
        Log.d(LOG_TAG, "onMethodCall - IN , method=${call.method}")
        if (pickOrSave == null) {
            if (!createPickOrSave()) {
                result.error("init_failed", "Not attached", null)
                return
            }
        }
        when (call.method) {
            "pickDirectory" -> pickOrSave!!.pickDirectory(
                result,
                initialDirectoryUri = call.argument("initialDirectoryUri"),
            )

            "directoryDocumentsPicker" -> pickOrSave!!.pickDirectoryDocuments(
                result,
                documentId = call.argument("documentId"),
                directoryUri = call.argument("directoryUri"),
                recurseDirectories = call.argument("recurseDirectories"),
                allowedExtensions = parseMethodCallListOfStringArgument(
                    call, "allowedExtensions"
                ) ?: listOf(),
                mimeTypesFilter = parseMethodCallListOfStringArgument(call, "mimeTypesFilter")
                    ?: listOf(),
            )

            "pickFiles" -> pickOrSave!!.pickFile(
                result,
                allowedExtensions = parseMethodCallListOfStringArgument(
                    call, "allowedExtensions"
                ) ?: listOf(),
                mimeTypesFilter = parseMethodCallListOfStringArgument(call, "mimeTypesFilter")
                    ?: listOf(),
                localOnly = call.argument("localOnly") ?: false,
                copyFileToCacheDir = call.argument("getCachedFilePath") ?: false,
                pickerType = parseMethodCallPickerTypeArgument(call) ?: PickerType.File,
                enableMultipleSelection = call.argument("enableMultipleSelection") ?: true,
            )

            "saveFiles" -> pickOrSave!!.saveFile(
                result,
                saveFiles = parseMethodCallListOfSaveFileInfoArgument(call, "saveFiles"),
                mimeTypesFilter = parseMethodCallListOfStringArgument(call, "mimeTypesFilter")
                    ?: listOf(),
                localOnly = call.argument("localOnly") ?: false,
                directoryUri = call.argument("directoryUri"),
            )

            "fileMetaData" -> pickOrSave!!.fileMetaData(
                result, sourceFilePathOrUri = call.argument("filePath")
            )

            "cacheFilePathFromPath" -> pickOrSave!!.cacheFilePath(
                result,
                sourceFilePathOrUri = call.argument("filePath"),
            )

            "uriPermissionStatus" -> pickOrSave!!.uriPermissionStatus(
                result,
                uri = call.argument("uri"),
                releasePermission = call.argument("releasePermission"),
            )

            "urisWithPersistedPermission" -> pickOrSave!!.urisWithPersistedPermission(result)
            "cancelActions" -> pickOrSave!!.cancelActions(
                cancelType = parseMethodCallCancelTypeArgument(
                    call, "cancelType"
                )
            )

            else -> result.notImplemented()
        }
    }

    private fun createPickOrSave(): Boolean {
        Log.d(LOG_TAG, "createPickOrSave - IN")

        var pickOrSave: PickOrSave? = null
        if (activityBinding != null) {
            pickOrSave = PickOrSave(
                activity = activityBinding!!.activity
            )
            activityBinding!!.addActivityResultListener(pickOrSave)
        }
        this.pickOrSave = pickOrSave

        Log.d(LOG_TAG, "createPickOrSave - OUT")

        return pickOrSave != null
    }

    private fun parseMethodCallListOfStringArgument(
        call: MethodCall, arg: String
    ): List<String>? {
        if (call.hasArgument(arg)) {
            return call.argument<ArrayList<String>>(arg)?.toList()
        }
        return null
    }

//    private fun parseMethodCallArrayOfByteArgument(
//        call: MethodCall, arg: String
//    ): Array<ByteArray>? {
//        if (call.hasArgument(arg)) {
//            return call.argument<ArrayList<ByteArray>>(arg)?.toTypedArray()
//        }
//        return null
//    }

    private fun parseMethodCallPickerTypeArgument(call: MethodCall): PickerType? {
        val arg = "pickerType"
        if (call.hasArgument(arg)) {
            return if (call.argument<String>(arg)?.toString() == "PickerType.file") {
                PickerType.File
            } else if (call.argument<String>(arg)?.toString() == "PickerType.photo") {
                PickerType.Photo
            } else {
                null
            }
        }
        return null
    }

    private fun parseMethodCallCancelTypeArgument(call: MethodCall, arg: String): CancelType? {
        if (call.hasArgument(arg)) {
            return if (call.argument<String>(arg)?.toString() == "CancelType.filesSaving") {
                CancelType.FilesSaving
            } else if (call.argument<String>(arg)
                    ?.toString() == "CancelType.directoryDocumentsPicker"
            ) {
                CancelType.DirectoryDocumentsPicker
            } else {
                null
            }
        }
        return null
    }

    private fun parseMethodCallListOfSaveFileInfoArgument(
        call: MethodCall, arg: String
    ): List<SaveFileInfo>? {
        if (call.hasArgument(arg)) {
            val saveFilesMapsList = call.argument<ArrayList<Map<String, Any>>>(arg)?.toList()
            val saveFilesList: MutableList<SaveFileInfo> = mutableListOf()
            saveFilesMapsList?.forEach { it ->
                val saveFile = SaveFileInfo(
                    filePath = it["filePath"] as String?,
                    fileData = it["fileData"] as ByteArray?,
                    fileName = it["fileName"] as String?
                )
                saveFilesList.add(saveFile)
            }
            return saveFilesList
        }
        return null
    }

}
