import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'pick_or_save_platform_interface.dart';

/// An implementation of [PickOrSavePlatform] that uses method channels.
class MethodChannelPickOrSave extends PickOrSavePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('pick_or_save');

  @override
  Future<String?> directoryPicker({DirectoryPickerParams? params}) async {
    final List? picked = await methodChannel.invokeMethod<List?>(
        'pickDirectory', params?.toJson());
    return picked?.cast<String>().first;
  }

  @override
  Future<List<DocumentFile>?> directoryDocumentsPicker(
      {DirectoryDocumentsPickerParams? params}) async {
    final List? pickedFiles = await methodChannel.invokeMethod<List?>(
        'directoryDocumentsPicker', params?.toJson());
    List<DocumentFile>? directoryFiles;
    if (pickedFiles != null) {
      directoryFiles = [];
    }
    pickedFiles?.forEach((pickedFile) {
      directoryFiles?.add(
        DocumentFile(
          id: pickedFile[0] as String,
          uri: pickedFile[1] as String,
          mimeType: pickedFile[2] as String?,
          name: pickedFile[3] as String,
          isDirectory: pickedFile[4] as bool,
          isFile: pickedFile[5] as bool,
        ),
      );
    });
    return directoryFiles?.cast<DocumentFile>();
  }

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
  Future<String?> cacheFilePathFromPath(
      {CacheFilePathFromPathParams? params}) async {
    final String? result = await methodChannel.invokeMethod<String?>(
        'cacheFilePathFromPath', params?.toJson());
    return result;
  }

  @override
  Future<bool?> uriPermissionStatus({UriPermissionStatusParams? params}) async {
    final bool? result = await methodChannel.invokeMethod<bool?>(
        'uriPermissionStatus', params?.toJson());
    return result;
  }

  @override
  Future<List<String>?> urisWithPersistedPermission() async {
    final List? result =
        await methodChannel.invokeMethod<List?>('urisWithPersistedPermission');
    return result?.cast<String>();
  }

  @override
  Future<String?> cancelActions({CancelActionsParams? params}) async {
    final String? result = await methodChannel.invokeMethod<String?>(
        'cancelActions', params?.toJson());
    return result;
  }
}

/// Parameters for the [uriPermissionStatus] method.
class UriPermissionStatusParams {
  /// Provide permission status of a uri.
  final String uri;

  /// Set true to release.
  final bool releasePermission;

  /// Create parameters for the [uriPermissionStatus] method.
  const UriPermissionStatusParams(
      {required this.uri, this.releasePermission = false});

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'uri': uri,
      'releasePermission': releasePermission
    };
  }

  // Implement toString to make it easier to see information
  // when using the print statement.
  @override
  String toString() {
    return 'PermissionStatusParams{uri: $uri}';
  }
}

/// Parameters for the [directoryPicker] method.
class DirectoryPickerParams {
  /// Provide initial directory uri if you have one from previous calls.
  ///
  /// If provided, opens this uri location on opening picker.
  final String? initialDirectoryUri;

  /// Create parameters for the [directoryPicker] method.
  const DirectoryPickerParams({this.initialDirectoryUri});

  Map<String, dynamic> toJson() {
    return <String, dynamic>{'initialDirectoryUri': initialDirectoryUri};
  }

  // Implement toString to make it easier to see information
  // when using the print statement.
  @override
  String toString() {
    return 'DirectoryPickerParams{initialDirectoryUri: $initialDirectoryUri}';
  }
}

enum CancelType { filesSaving, directoryDocumentsPicker }

/// Parameters for the [cancelActions] method.
class CancelActionsParams {
  /// Provide which action you want to cancel.
  final CancelType cancelType;

  /// Create parameters for the [cancelActions] method.
  const CancelActionsParams({required this.cancelType});

  Map<String, dynamic> toJson() {
    return <String, dynamic>{'cancelType': cancelType.toString()};
  }

  // Implement toString to make it easier to see information
  // when using the print statement.
  @override
  String toString() {
    return 'CancelActionsParams{cancelType: $cancelType}';
  }
}

class DocumentFile {
  final String id;
  final String uri;
  final String? mimeType;
  final String name;
  final bool isDirectory;
  final bool isFile;

  DocumentFile({
    required this.id,
    required this.uri,
    required this.mimeType,
    required this.name,
    required this.isDirectory,
    required this.isFile,
  });

  // Implement toString to make it easier to see information
  // when using the print statement.
  @override
  String toString() {
    return 'DocumentFile{id: $id, uri: $uri, type: $mimeType, name: $name, isDirectory: $isDirectory, isFile: $isFile}';
  }
}

/// Parameters for the [directoryDocumentsPicker] method.
class DirectoryDocumentsPickerParams {
  /// DocumentID is useful if you have saved the DocumentFile object from previous run
  /// for a specific Document and now you want to fetch files under that Document only
  /// without going through all the files of the before.
  ///
  /// To properly use it you need to save uri and id from DocumentFile object that you obtained from
  /// previous run. And then provide the uri as directoryUri and id as documentId to DirectoryDocumentsPickerParams.
  final String? documentId;

  /// Provide uri of directory of which you want sub documents uris.
  final String directoryUri;

  /// Optionally recurse into sub-directories.
  ///
  /// If true then it will provide uri of all sub documents in subdirectories.
  final bool recurseDirectories;

  /// Provide allowed extensions (null or empty allows all extensions).
  ///
  /// If provided, returns DocumentFile for only allowed file extensions.
  final List<String>? allowedExtensions;

  /// Filter MIME types.
  final List<String>? mimeTypesFilter;

  /// Create parameters for the [directoryDocumentsPicker] method.
  const DirectoryDocumentsPickerParams(
      {this.documentId,
      required this.directoryUri,
      this.recurseDirectories = false,
      this.allowedExtensions,
      this.mimeTypesFilter});

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'documentId': documentId,
      'directoryUri': directoryUri,
      "recurseDirectories": recurseDirectories,
      'allowedExtensions': allowedExtensions
          ?.map((e) => e.toLowerCase().replaceAll(".", ""))
          .toList(),
      "mimeTypesFilter": mimeTypesFilter
    };
  }

  // Implement toString to make it easier to see information
  // when using the print statement.
  @override
  String toString() {
    return 'DirectoryFilesPickerParams{directoryUri: $directoryUri, recurseDirectories: $recurseDirectories, allowedExtensions: $allowedExtensions, mimeTypesFilter: $mimeTypesFilter}';
  }
}

/// Picker types for [filePicker].
enum PickerType { file, photo }

/// Parameters for the [filePicker] method.
class FilePickerParams {
  /// Provide allowed extensions (null allows all extensions).
  ///
  /// If provided, returns path or uri for only allowed file extensions.
  final List<String>? allowedExtensions;

  /// Filter MIME types.
  /// File picker will be showing only provided MIME types.
  ///
  /// If pickerType is PickerType.photo then the mimeTypesFilter is necessary and the first mime type will only be effective.
  /// Supported mimeTypesFilter when pickerType is PickerType.photo is [video/*] for videos or [image/*] for images or [*/*] for images and videos both.
  final List<String>? mimeTypesFilter;

  /// Show only device local files.
  ///
  /// If true, [filePicker] shows only local files.
  final bool localOnly;

  /// Copy file to cache directory.
  ///
  /// If true, [filePicker] returns path to the copied file.
  /// If false, [filePicker] returns uri of original picked file.
  final bool getCachedFilePath;

  /// Picker types for (file, photo).
  ///
  /// Defaults to PickerType.file.
  /// If pickerType is PickerType.photo then the mimeTypesFilter is necessary and the first mime type will only be effective.
  /// Supported mimeTypesFilter when pickerType is PickerType.photo is [video/*] for videos or [image/*] for images or [*/*] for images and videos both.
  final PickerType pickerType;

  /// To pick multiple files set this to true.
  ///
  /// Defaults to false.
  final bool enableMultipleSelection;

  /// Create parameters for the [filePicker] method.
  const FilePickerParams({
    this.allowedExtensions,
    this.mimeTypesFilter,
    this.localOnly = false,
    this.getCachedFilePath = true,
    this.pickerType = PickerType.file,
    this.enableMultipleSelection = false,
  }) : assert(
            pickerType == PickerType.photo
                ? mimeTypesFilter != null && mimeTypesFilter.length >= 1
                : true,
            'If pickerType is PickerType.photo then mimeTypesFilter should not be null and should not be empty');

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'allowedExtensions': allowedExtensions
          ?.map((e) => e.toLowerCase().replaceAll(".", ""))
          .toList(),
      'mimeTypesFilter': mimeTypesFilter,
      'localOnly': localOnly,
      'getCachedFilePath': getCachedFilePath,
      'pickerType': pickerType.toString(),
      'enableMultipleSelection': enableMultipleSelection,
    };
  }

  // Implement toString to make it easier to see information
  // when using the print statement.
  @override
  String toString() {
    return 'FilePickerParams{allowedExtensions: $allowedExtensions, mimeTypesFilter: $mimeTypesFilter, localOnly: $localOnly, getCachedFilePath: $getCachedFilePath, pickerType: $pickerType, enableMultipleSelection: $enableMultipleSelection}';
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

  // Implement toString to make it easier to see information
  // when using the print statement.
  @override
  String toString() {
    return 'SaveFileInfo{filePath: $filePath, fileData: $fileData, fileName: $fileName}';
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

  /// Provide uri of directory where you want save the files.
  ///
  /// If not provided then it will open the system location picker.
  final String? directoryUri;

  /// Create parameters for the [saveFile] method.
  const FileSaverParams(
      {this.saveFiles,
      this.mimeTypesFilter,
      this.localOnly = false,
      this.directoryUri})
      : assert(saveFiles != null && saveFiles.length != 0,
            'provide saveFiles with non null and non empty list');

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'saveFiles': saveFiles?.map((e) => e.toJson()).toList(),
      'mimeTypesFilter': mimeTypesFilter,
      'localOnly': localOnly,
      'directoryUri': directoryUri
    };
  }

  // Implement toString to make it easier to see information
  // when using the print statement.
  @override
  String toString() {
    return 'FileSaverParams{saveFiles: $saveFiles, mimeTypesFilter: $mimeTypesFilter, localOnly: $localOnly}';
  }
}

/// Parameters for the [fileMetaData] method.
class FileMetadataParams {
  /// Path of the file.
  final String filePath;

  /// Create parameters for the [fileMetaData] method.
  const FileMetadataParams({
    required this.filePath,
  });

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'filePath': filePath,
    };
  }

  // Implement toString to make it easier to see information
  // when using the print statement.
  @override
  String toString() {
    return 'FileMetadataParams{filePath: $filePath}';
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

/// Parameters for the [cacheFilePathFromPath] method.
class CacheFilePathFromPathParams {
  /// Path of the file.
  final String filePath;

  /// Create parameters for the [cacheFilePathFromPath] method.
  const CacheFilePathFromPathParams({
    required this.filePath,
  });

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'filePath': filePath,
    };
  }

  // Implement toString to make it easier to see information
  // when using the print statement.
  @override
  String toString() {
    return 'CacheFilePathFromUriParams{filePath: $filePath}';
  }
}
