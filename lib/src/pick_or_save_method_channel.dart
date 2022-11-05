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

  @override
  Future<FileMetadata> fileMetaData({FileMetadataParams? params}) async {
    final List? fileMetaData = await methodChannel.invokeMethod<List?>(
        'fileMetaData', params?.toJson());
    fileMetaData?.cast<String>();
    if (fileMetaData != null) {
      return FileMetadata(
          displayName: fileMetaData[0],
          size: fileMetaData[1],
          lastModified: fileMetaData[2]);
    } else {
      return FileMetadata(displayName: null, size: null, lastModified: null);
    }
  }

  @override
  Future<String?> cacheFilePathFromUri(
      {CacheFilePathFromUriParams? params}) async {
    final String? result = await methodChannel.invokeMethod<String?>(
        'cacheFilePathFromUri', params?.toJson());
    return result;
  }

  @override
  Future<String?> cancelFilesSaving() async {
    final String? result =
        await methodChannel.invokeMethod<String?>('cancelFilesSaving');
    return result;
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
  final List<String>? mimeTypesFilter;

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
      this.mimeTypesFilter,
      this.localOnly = false,
      this.copyFileToCacheDir = true,
      this.filePickingType = FilePickingType.single});

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'allowedExtensions': allowedExtensions
          ?.map((e) => e.toLowerCase().replaceAll(".", ""))
          .toList(),
      'mimeTypeFilter': mimeTypesFilter,
      'localOnly': localOnly,
      'copyFileToCacheDir': copyFileToCacheDir,
      'filePickingType': filePickingType.toString(),
    };
  }
}

/// File saving types for [fileSaver].
enum FileSavingType { single, multiple }

/// Parameters for the [fileMetaData] method.
class SaveFileInfo {
  /// Path of the file to save.
  /// Provide either [filePath] or [fileData].
  final String? filePath;

  /// File data.
  /// Provide either [filePath] or [fileData].
  final Uint8List? fileData;

  /// The file name to use when saving the file.
  /// Required if [fileData] is provided.
  final String? fileName;

  /// Create parameters for the [fileMetaData] method.
  const SaveFileInfo({
    this.filePath,
    this.fileData,
    this.fileName,
  })  : assert(filePath != null || fileData != null,
            'provide anyone out of filePath or fileData'),
        assert(filePath == null || fileData == null,
            'provide only anyone out of filePath or fileData'),
        assert(fileData != null ? fileName != null : true,
            'provide file name when fileData was provided');

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'filePath': filePath,
      'fileData': fileData,
      'fileName': fileName,
    };
  }
}

/// Parameters for the [fileSaver] method.
class FileSaverParams {
  /// SaveFileInfo for files to save.
  final List<SaveFileInfo>? saveFiles;

  /// Filter MIME types.
  /// File picker will be showing only provided MIME types.
  final List<String>? mimeTypesFilter;

  /// Show only device local files.
  ///
  /// If true, [filePicker] shows only local files.
  final bool localOnly;

  /// Create parameters for the [saveFile] method.
  const FileSaverParams(
      {this.saveFiles, this.mimeTypesFilter, this.localOnly = false})
      : assert(saveFiles != null && saveFiles.length != 0,
            'provide saveFiles with non null and non empty list');

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'saveFiles': saveFiles?.map((e) => e.toJson()).toList(),
      'mimeTypesFilter': mimeTypesFilter,
      'localOnly': localOnly
    };
  }
}

/// Parameters for the [fileMetaData] method.
class FileMetadataParams {
  /// Path of the file.
  final String? sourceFilePath;

  /// URI of the file.
  final String? sourceFileUri;

  /// Create parameters for the [fileMetaData] method.
  const FileMetadataParams({
    this.sourceFilePath,
    this.sourceFileUri,
  })  : assert(sourceFileUri != null || sourceFilePath != null,
            'provide anyone out of sourceFilePath or sourceFileURI'),
        assert(sourceFileUri == null || sourceFilePath == null,
            'sourceFilePath or sourceFileURI should be null');

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'sourceFilePath': sourceFilePath,
      'sourceFileUri': sourceFileUri,
    };
  }
}

class FileMetadata {
  final String? displayName;
  final String? size;
  final String? lastModified;

  FileMetadata({
    required this.displayName,
    required this.size,
    required this.lastModified,
  });

  // Implement toString to make it easier to see information
  // when using the print statement.
  @override
  String toString() {
    return 'FileMetadata{displayName: $displayName, size: $size, lastModified: $lastModified}';
  }
}

/// Parameters for the [cacheFilePathFromUri] method.
class CacheFilePathFromUriParams {
  /// URI of the file.
  final String fileUri;

  /// Create parameters for the [cacheFilePathFromUri] method.
  const CacheFilePathFromUriParams({
    required this.fileUri,
  });

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'fileUri': fileUri,
    };
  }

  // Implement toString to make it easier to see information
  // when using the print statement.
  @override
  String toString() {
    return 'CacheFilePathFromUriParams{fileUri: $fileUri}';
  }
}
