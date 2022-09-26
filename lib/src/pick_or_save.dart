import 'package:pick_or_save/src/pick_or_save_method_channel.dart';

import 'package:pick_or_save/src/pick_or_save_platform_interface.dart';

class PickOrSave {
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
}
