import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'pick_or_save_platform_interface.dart';

/// An implementation of [PickOrSavePlatform] that uses method channels.
class MethodChannelPickOrSave extends PickOrSavePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('pick_or_save');

  @override
  Future<List<String>?> filePicker({FilePickerParams? params}) async {
    final List? picked =
        await methodChannel.invokeMethod<List?>('pickFiles', params?.toJson());
    return picked?.cast<String>();
  }

  @override
  Future<List<String>?> fileSaver({FileSaverParams? params}) async {
    final List? saved =
        await methodChannel.invokeMethod<List?>('saveFiles', params?.toJson());
    return saved?.cast<String>();
  }
}

/// File picking types for [filePicker].
enum FilePickingType { single, multiple }

/// Parameters for the [filePicker] method.
class FilePickerParams {
  /// Provide allowed extensions (null allows all extensions).
  ///
  /// If provided, returns path or uri for only allowed file extensions.
  final List<String>? allowedExtensions;

  /// Filter MIME types.
  /// File picker will be showing only provided MIME types.
  final List<String>? mimeTypeFilter;

  /// Show only device local files.
  ///
  /// If true, [filePicker] shows only local files.
  final bool localOnly;

  /// Copy file to cache directory.
  ///
  /// If true, [filePicker] returns path to the copied file.
  /// If false, [filePicker] returns uri of original picked file.
  final bool copyFileToCacheDir;

  /// File picking types (single, multiple).
  ///
  /// To pick multiple files set filePickingType to FilePickingType.multiple.
  /// To pick single file set filePickingType to FilePickingType.single.
  final FilePickingType filePickingType;

  /// Create parameters for the [filePicker] method.
  const FilePickerParams(
      {this.allowedExtensions,
      this.mimeTypeFilter,
      this.localOnly = false,
      this.copyFileToCacheDir = true,
      this.filePickingType = FilePickingType.single});

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'allowedExtensions': allowedExtensions,
      'mimeTypeFilter': mimeTypeFilter,
      'localOnly': localOnly,
      'copyFileToCacheDir': copyFileToCacheDir,
      'filePickingType': filePickingType.toString(),
    };
  }
}

/// File saving types for [fileSaver].
enum FileSavingType { single, multiple }

/// Parameters for the [fileSaver] method.
class FileSaverParams {
  /// Path of the files to save.
  /// Provide either [sourceFilesPaths] or [data] list.
  final List<String>? sourceFilesPaths;

  /// Files data.
  /// Provide either [sourceFilesPaths] or [data] list.
  final List<Uint8List>? data;

  /// The suggested files names to use when saving the files.
  /// Required if [data] is provided.
  final List<String>? filesNames;

  /// Filter MIME types.
  /// File picker will be showing only provided MIME types.
  final List<String>? mimeTypeFilter;

  /// Show only device local files.
  ///
  /// If true, [filePicker] shows only local files.
  final bool localOnly;

  /// Create parameters for the [saveFile] method.
  const FileSaverParams(
      {this.sourceFilesPaths,
      this.data,
      this.filesNames,
      this.mimeTypeFilter,
      this.localOnly = false})
      : assert(sourceFilesPaths == null || data == null,
            'sourceFilesPaths or data should be null'),
        assert(sourceFilesPaths != null || data != null,
            'Missing sourceFilesPaths or data'),
        assert(data == null || (filesNames != null && filesNames.length != 0),
            'Missing filesNames');

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'sourceFilesPaths': sourceFilesPaths,
      'data': data,
      'filesNames': filesNames,
      'mimeTypeFilter': mimeTypeFilter,
      'localOnly': localOnly
    };
  }
}
