[![pub package](https://img.shields.io/pub/v/pick_or_save.svg)](https://pub.dev/packages/pick_or_save) [![wakatime](https://wakatime.com/badge/user/83f3b15d-49de-4c01-b8de-bbc132f11be1/project/a5e5bda6-1125-46d7-9dc4-5028186265ca.svg)](https://wakatime.com/badge/user/83f3b15d-49de-4c01-b8de-bbc132f11be1/project/a5e5bda6-1125-46d7-9dc4-5028186265ca)

## Package description

A Flutter file picking and saving package that enables you to pick or save a single file and multiple files.

Note: Although this package supports picking and caching files by default to work with them in flutter, it is actually built for those who manage files natively in android as this package supports disabling copying of file in cache to work with Android URIs directly.

## Features

- Works on Android 5.0 (API level 21) or later.
- Pick single file or multiple files.
- Get meta data like name, size and last modified for files.
- Saves single file while allowing user to choose location and name.
- Saves multiple file while allowing user to choose location or directory for saving all files.
- Saves file from either file path or file data.
- Filter extensions when picking a document.
- Could limit picking a file from the local device only.

## Getting started

- In pubspec.yaml, add this dependency:

```yaml
pick_or_save: 
```

- Add this package to your project:

```dart
import 'package:pick_or_save/pick_or_save.dart';
```

## Basic Usage

### Picking single file

```dart
List<String>? result = await PickOrSave().filePicker(
  params: FilePickerParams(),
);
String filePath = result[0];
```
Note: Setting ```copyFileToCacheDir = false``` will provide uri path which can only be used in android native platform.

### Picking multiple files

```dart
List<String>? filesPaths = await PickOrSave().filePicker(
  params: FilePickerParams(enableMultipleSelection: true),
);
```
Note: Setting ```copyFileToCacheDir = false``` will provide uri paths which can only be used in android native platform.

### Saving single file

```dart
List<String>? result = await PickOrSave().fileSaver(
  params: FileSaverParams(
    saveFiles: [
      SaveFileInfo(
          filePath: filePath,
          fileName: "File.png")
    ],
  )
);
String savedFilePath = result[0];
```

### Saving multiple files

#### Saving multiple files from File

```dart
List<String>? result = await PickOrSave().fileSaver(
  params: FileSaverParams(
    saveFiles: [
      SaveFileInfo(
          filePath: filePath,
          fileName: "File 1.png"),
      SaveFileInfo(
          filePath: filePath,
          fileName: "File 2.png")
    ],
  )
);
```

#### Saving multiple files from Uint8List

```dart
List<String>? result = await PickOrSave().fileSaver(
  params: FileSaverParams(
    saveFiles: [
      SaveFileInfo(
          fileData: uint8List,
          fileName: "File 1.png"),
      SaveFileInfo(
          fileData: uint8List,
          fileName: "File 2.png")
    ],
  )
);
```

| Saving single file  | Saving multiple files |
| ------------- | ------------- |
| ![WhatsApp Image 2022-09-19 at 1 34 02 PM](https://user-images.githubusercontent.com/85361211/190974633-6aab39c9-e817-4b92-84ed-b3fd0a4405b9.jpeg) | ![WhatsApp Image 2022-09-19 at 1 33 04 PM](https://user-images.githubusercontent.com/85361211/190974687-fa5f0ba1-391f-4103-8ffc-acdf9c8bca73.jpeg) |

### File Metadata

```dart
FileMetadata result = await PickOrSave().fileMetaData(
  params: FileMetadataParams(sourceFileUri: fileUri),
);
```
