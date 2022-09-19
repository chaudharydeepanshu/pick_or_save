import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'package:pick_or_save/src/pick_or_save_method_channel.dart';

abstract class PickOrSavePlatform extends PlatformInterface {
  /// Constructs a PickOrSavePlatform.
  PickOrSavePlatform() : super(token: _token);

  static final Object _token = Object();

  static PickOrSavePlatform _instance = MethodChannelPickOrSave();

  /// The default instance of [PickOrSavePlatform] to use.
  ///
  /// Defaults to [MethodChannelPickOrSave].
  static PickOrSavePlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [PickOrSavePlatform] when
  /// they register themselves.
  static set instance(PickOrSavePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<List<String>?> filePicker({FilePickerParams? params}) {
    throw UnimplementedError('pickFile() has not been implemented.');
  }

  Future<List<String>?> fileSaver({FileSaverParams? params}) {
    throw UnimplementedError('fileSaver() has not been implemented.');
  }
}
