import 'package:pick_or_save/src/pick_or_save_method_channel.dart';

import 'package:pick_or_save/src/pick_or_save_platform_interface.dart';

class PickOrSave {
  /// Opens directory picker for picking a directory.
  ///
  /// Returns the uri of the picked directory or null if operation was cancelled.
  /// Throws exception on error.
  Future<String?> directoryPicker({DirectoryPickerParams? params}) {
    return PickOrSavePlatform.instance.directoryPicker(params: params);
  }

  /// Provides list of URIs of sub documents in a directory.
  ///
  /// Returns the uris of files of the directory or null if operation was cancelled.
  /// Throws exception on error.
  Future<List<DocumentFile>?> directoryDocumentsPicker(
      {DirectoryDocumentsPickerParams? params}) {
    return PickOrSavePlatform.instance.directoryDocumentsPicker(params: params);
  }

  /// Opens file picker for picking a files.
  ///
  /// Returns the path or uri of the picked file or null if operation was cancelled.
  /// Throws exception on error.
  Future<List<String>?> filePicker({FilePickerParams? params}) {
    return PickOrSavePlatform.instance.filePicker(params: params);
  }

  /// Displays a dialog for selecting a location where to save the file or files and
  /// saves the file or files to the selected location.
  ///
  /// Returns path of the saved file or folder if multiple files or null if operation was cancelled.
  /// Throws exception on error.
  Future<List<String>?> fileSaver({FileSaverParams? params}) {
    return PickOrSavePlatform.instance.fileSaver(params: params);
  }

  /// Get the display name and size of a file from uri.
  ///
  /// Returns [FileMetadata].
  /// Throws exception on error.
  Future<FileMetadata> fileMetaData({FileMetadataParams? params}) {
    return PickOrSavePlatform.instance.fileMetaData(params: params);
  }

  /// Get the cached file path from uri.
  ///
  /// Returns cached file path.
  /// Throws exception on error.
  Future<String?> cacheFilePathFromPath({CacheFilePathFromPathParams? params}) {
    return PickOrSavePlatform.instance.cacheFilePathFromPath(params: params);
  }

  /// Returns bool value for permissions status of a uri.
  ///
  /// If releasePermission set to true then it will return bool value after releasing permission.
  Future<bool?> uriPermissionStatus({UriPermissionStatusParams? params}) {
    return PickOrSavePlatform.instance.uriPermissionStatus(params: params);
  }

  /// Returns list of uris with persisted permissions.
  Future<List<String>?> urisWithPersistedPermission() {
    return PickOrSavePlatform.instance.urisWithPersistedPermission();
  }

  /// Cancels running action.
  /// Note it will take some time to cancel and then release the resources as
  /// it is not possible to cancel anything instantaneously.
  ///
  /// Returns the cancelling message. Doesn't mean that the resources are released.
  Future<String?> cancelActions({CancelActionsParams? params}) {
    return PickOrSavePlatform.instance.cancelActions(params: params);
  }
}
