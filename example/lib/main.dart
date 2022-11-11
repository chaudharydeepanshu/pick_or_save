import 'dart:convert';
import 'dart:developer';
import 'dart:io';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pick_or_save/pick_or_save.dart';

void main() => runApp(const MyApp());

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Pick Or Save example',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(useMaterial3: true),
      darkTheme: ThemeData.dark(useMaterial3: true),
      themeMode: ThemeMode.system,
      home: const MyHomePage(),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key});

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  final _pickOrSavePlugin = PickOrSave();

  List<bool> isSelected = [true, false];

  bool _isBusy = false;
  final bool _localOnly = false;
  List<String>? _pickedFilePath;

  @override
  void initState() {
    super.initState();
  }

  Future<List<String>?> _fileSaver(FileSaverParams params) async {
    List<String>? result;
    try {
      setState(() {
        _isBusy = true;
      });
      result = await _pickOrSavePlugin.fileSaver(params: params);
    } on PlatformException catch (e) {
      log(e.toString());
    } catch (e) {
      log(e.toString());
    }
    if (!mounted) return result;
    setState(() {
      _isBusy = false;
    });
    return result;
  }

  Future<List<String>?> _filePicker(FilePickerParams params) async {
    List<String>? result;
    try {
      setState(() {
        _isBusy = true;
      });
      result = await _pickOrSavePlugin.filePicker(params: params);
    } on PlatformException catch (e) {
      log(e.toString());
    } catch (e) {
      log(e.toString());
    }
    if (!mounted) return result;
    setState(() {
      _isBusy = false;
    });
    return result;
  }

  Future<FileMetadata?> _fileMetadata(FileMetadataParams params) async {
    FileMetadata? result;
    try {
      result = await _pickOrSavePlugin.fileMetaData(params: params);
      log(result.toString());
    } on PlatformException catch (e) {
      log(e.toString());
    } catch (e) {
      log(e.toString());
    }
    return result;
  }

  Future<String?> _cacheFilePathFromUri(
      CacheFilePathFromPathParams params) async {
    String? result;
    try {
      result = await _pickOrSavePlugin.cacheFilePathFromUri(params: params);
      log(result.toString());
    } on PlatformException catch (e) {
      log(e.toString());
    } catch (e) {
      log(e.toString());
    }
    return result;
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        ScaffoldMessenger.of(context).hideCurrentSnackBar();
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Pick Or Save example'),
        ),
        body: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                "Picking",
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 8),
              Card(
                margin: EdgeInsets.zero,
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Column(
                    children: [
                      ToggleButtons(
                        onPressed: (int index) {
                          setState(() {
                            for (int buttonIndex = 0;
                                buttonIndex < isSelected.length;
                                buttonIndex++) {
                              if (buttonIndex == index) {
                                isSelected[buttonIndex] = true;
                              } else {
                                isSelected[buttonIndex] = false;
                              }
                            }
                          });
                        },
                        isSelected: isSelected,
                        children: const <Widget>[
                          Text(" Give URI "),
                          Text(" Give cached path "),
                        ],
                      ),
                      const Divider(),
                      CustomButton(
                          buttonText: 'Pick single file',
                          onPressed: _isBusy
                              ? null
                              : () async {
                                  final params = FilePickerParams(
                                    localOnly: _localOnly,
                                    copyFileToCacheDir: isSelected[1],
                                  );

                                  List<String>? result =
                                      await _filePicker(params);

                                  callSnackBar(
                                      mounted: mounted,
                                      context: context,
                                      text: result.toString());
                                }),
                      const Divider(),
                      CustomButton(
                          buttonText: 'Pick multiple files',
                          onPressed: _isBusy
                              ? null
                              : () async {
                                  final params = FilePickerParams(
                                    localOnly: _localOnly,
                                    copyFileToCacheDir: isSelected[1],
                                    enableMultipleSelection: true,
                                  );

                                  List<String>? result =
                                      await _filePicker(params);

                                  callSnackBar(
                                      mounted: mounted,
                                      context: context,
                                      text: result.toString());
                                }),
                      const Divider(),
                      CustomButton(
                          buttonText: 'Pick only image and pdf mime types',
                          onPressed: _isBusy
                              ? null
                              : () async {
                                  final params = FilePickerParams(
                                      localOnly: _localOnly,
                                      copyFileToCacheDir: isSelected[1],
                                      enableMultipleSelection: true,
                                      mimeTypesFilter: [
                                        "image/*",
                                        "application/pdf"
                                      ]);

                                  List<String>? result =
                                      await _filePicker(params);

                                  callSnackBar(
                                      mounted: mounted,
                                      context: context,
                                      text: result.toString());
                                }),
                      Text(
                        "Note - This will show only these mimes for selection.",
                        style: Theme.of(context).textTheme.labelSmall,
                      ),
                      const Divider(),
                      CustomButton(
                          buttonText: 'Pick only .txt extension types',
                          onPressed: _isBusy
                              ? null
                              : () async {
                                  final params = FilePickerParams(
                                    localOnly: _localOnly,
                                    copyFileToCacheDir: isSelected[1],
                                    enableMultipleSelection: true,
                                    allowedExtensions: [".txt"],
                                  );

                                  List<String>? result =
                                      await _filePicker(params);

                                  callSnackBar(
                                      mounted: mounted,
                                      context: context,
                                      text: result.toString());
                                }),
                      Text(
                        "Note - This will show only these extensions for selection if the extension has a valid mime type if not it will still only pick that extension and reject others.",
                        style: Theme.of(context).textTheme.labelSmall,
                      ),
                      const Divider(),
                      CustomButton(
                          buttonText: 'Open photo picker',
                          onPressed: _isBusy
                              ? null
                              : () async {
                                  final params = FilePickerParams(
                                    localOnly: _localOnly,
                                    copyFileToCacheDir: isSelected[1],
                                    enableMultipleSelection: true,
                                    allowedExtensions: [".png"],
                                    mimeTypesFilter: ["*/*"],
                                    pickerType: PickerType.photo,
                                  );

                                  List<String>? result =
                                      await _filePicker(params);

                                  callSnackBar(
                                      mounted: mounted,
                                      context: context,
                                      text: result.toString());
                                }),
                      Text(
                        "Note - This will show new photo picker on supported android devices and for unsupported the regular picker. Also it always needs mime type and only first mime type is selected for selection. Also, only pick provided extensions only and reject others.",
                        style: Theme.of(context).textTheme.labelSmall,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Text(
                "Saving",
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 8),
              Card(
                margin: EdgeInsets.zero,
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Column(
                    children: [
                      CustomButton(
                          buttonText: 'Saving single file from file path',
                          onPressed: _isBusy
                              ? null
                              : () async {
                                  File tempFile = await getTempFileFromData(
                                      base64.decode(testBase64));

                                  final params = FileSaverParams(
                                      localOnly: _localOnly,
                                      saveFiles: [
                                        SaveFileInfo(
                                            filePath: tempFile.path,
                                            fileName: "single file.png")
                                      ]);

                                  List<String>? result =
                                      await _fileSaver(params);

                                  callSnackBar(
                                      mounted: mounted,
                                      context: context,
                                      text: result.toString());
                                }),
                      const Divider(),
                      CustomButton(
                          buttonText: 'Saving single file from Uint8List',
                          onPressed: _isBusy
                              ? null
                              : () async {
                                  final params = FileSaverParams(
                                    localOnly: _localOnly,
                                    saveFiles: [
                                      SaveFileInfo(
                                          fileData: testUint8List,
                                          fileName: "single file.png")
                                    ],
                                  );

                                  List<String>? result =
                                      await _fileSaver(params);

                                  callSnackBar(
                                      mounted: mounted,
                                      context: context,
                                      text: result.toString());
                                }),
                      const Divider(),
                      CustomButton(
                          buttonText: 'Saving multiple files',
                          onPressed: _isBusy
                              ? null
                              : () async {
                                  final params = FileSaverParams(
                                    localOnly: _localOnly,
                                    saveFiles: [
                                      SaveFileInfo(
                                          fileData: testUint8List,
                                          fileName: "File 1.png"),
                                      SaveFileInfo(
                                          fileData: testUint8List,
                                          fileName: "File 2.png")
                                    ],
                                  );

                                  List<String>? result =
                                      await _fileSaver(params);

                                  callSnackBar(
                                      mounted: mounted,
                                      context: context,
                                      text: result.toString());
                                }),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Text(
                "Get Picked File Metadata",
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 8),
              Card(
                margin: EdgeInsets.zero,
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Column(
                    children: [
                      CustomButton(
                          buttonText: 'Pick single file',
                          onPressed: _isBusy
                              ? null
                              : () async {
                                  final params = FilePickerParams(
                                    localOnly: _localOnly,
                                    copyFileToCacheDir: isSelected[1],
                                  );

                                  _pickedFilePath = await _filePicker(params);

                                  callSnackBar(
                                      mounted: mounted,
                                      context: context,
                                      text: _pickedFilePath.toString());
                                }),
                      CustomButton(
                          buttonText: 'Display Metadata',
                          onPressed: _pickedFilePath == null
                              ? null
                              : _pickedFilePath!.isEmpty
                                  ? null
                                  : () async {
                                      final params = FileMetadataParams(
                                        filePath: _pickedFilePath![0],
                                      );

                                      FileMetadata? result =
                                          await _fileMetadata(params);

                                      callSnackBar(
                                          mounted: mounted,
                                          context: context,
                                          text: result.toString());
                                    }),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Text(
                "Get cache file path from Uri or Path",
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 8),
              Card(
                margin: EdgeInsets.zero,
                child: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Column(
                    children: [
                      CustomButton(
                          buttonText: 'Pick single file',
                          onPressed: _isBusy
                              ? null
                              : () async {
                                  final params = FilePickerParams(
                                    localOnly: _localOnly,
                                    copyFileToCacheDir: isSelected[1],
                                  );

                                  _pickedFilePath = await _filePicker(params);

                                  callSnackBar(
                                      mounted: mounted,
                                      context: context,
                                      text: _pickedFilePath.toString());
                                }),
                      CustomButton(
                          buttonText: 'Display Cache file path',
                          onPressed: _pickedFilePath == null
                              ? null
                              : _pickedFilePath!.isEmpty
                                  ? null
                                  : () async {
                                      final params =
                                          CacheFilePathFromPathParams(
                                        filePath: _pickedFilePath![0],
                                      );

                                      String? result =
                                          await _cacheFilePathFromUri(params);

                                      callSnackBar(
                                          mounted: mounted,
                                          context: context,
                                          text: result.toString());
                                    }),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

Uint8List testUint8List = base64.decode(testBase64);

const String testBase64 =
    "iVBORw0KGgoAAAANSUhEUgAAAyAAAAJYCAMAAACtqHJCAAADAFBMVEUAAAD///+A/4Cq/6qAv4CZzJmA1aqS25KA35+O46qAzJmL0aKV1ZWJ2J2S26SI3ZmPz5+H0qWO1ZyG16GM2ZmS256L3KKQ05uK1Z+P1qOJ2J2O2aGJ25uN056I1aKM1pyP15+L2KKP2p6K26CO1ZyK1p+N16GJ2J2M2Z+P2qKM1Z6O1aCL1pyO15+L2KGN2Z2K1Z+N1aGK1p6M16CO2J2M2Z+O2aGL1Z6N1p+L16GN156K2KCM2Z2K1Z+M1qCO156L15+N2KGL2J6N1aCL1p6M1p+K16CM156O2J+M2aGN1p+L1qCN156L15+N2KCL2J6M1p+L1qGM15+N16CM2J6N2J+L2KCN1p6L1p+M156L15+M2KCN2J6M1p+N1qCM156N15+L2J6N2J+L1qCM1p6L15+M16CN156M2J+N2J6M1p+N1qCL156M15+L2KCM2J+L1p+M1p6N15+M16CN156L2J+M1qCL1p+M15+L156M15+N2KCM2J6N1p+M16CN15+L15+M156L2J+M1qCL1p6M15+N16CM15+N2J+M2J6M1p+L16CM156L15+M16CN2J+M1p+N156M15+N16CM15+M2J+L1qCM1p+L15+M156N15+M2KCN2J+M1p+M16CM15+M15+L156M2J+L1qCM15+N15+M16CM15+M2J+M1p6M15+M16CL15+M15+N156M2J+N1p+M156M15+M16CM15+L2J+M1p6L15+M15+N15+M15+M16CM2J+M15+M156M15+L15+M15+N2J+M1qCN15+M15+M156M15+M15+M1p+M15+L16CM15+N15+M156M2J+M15+M15+M15+M16CM15+M15+L1p6M15+M15+M15+M15+M16CM1p+M15+M15+M15+M15+N15+M2J+M16CM15+M15+M15+M15+M15+M15+L15+M16CM15+M15+M15+M2J+M15+M15+M15+M16CM15+N15+M15+M15+M15+M15+M15+M16CM15+M15+M15+M15+ualV/AAAA/3RSTlMAAQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyAhIiMkJSYnKCkqKywtLi8wMTIzNDU2Nzg5Ojs8PT4/QEFCQ0RFRkdISUpLTE1O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";

Future<File> getTempFileFromData(Uint8List data) async {
  Directory tempDir = await getTemporaryDirectory();
  String tempPath = tempDir.path;
  File tempFile = File('$tempPath/file.png');
  tempFile.writeAsBytes(data);
  return tempFile;
}

class CustomButton extends StatelessWidget {
  const CustomButton({Key? key, required this.buttonText, this.onPressed})
      : super(key: key);

  final String buttonText;
  final void Function()? onPressed;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: OutlinedButton(
              onPressed: onPressed,
              child: Text(buttonText, textAlign: TextAlign.center)),
        ),
      ],
    );
  }
}

callSnackBar(
    {required bool mounted,
    required BuildContext context,
    required String text}) {
  if (mounted) {
    ScaffoldMessenger.of(context).hideCurrentSnackBar();
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text(text),
    ));
  }
}
