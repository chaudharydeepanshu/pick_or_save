[![pub package](https://img.shields.io/pub/v/pick_or_save.svg)](https://pub.dev/packages/pick_or_save) 

## Package description

A Flutter file picking and saving package that enables you to pick or save a single file and multiple files.

## Features

- Works on Android 5.0 (API level 21) or later.
- Pick single file or multiple files.
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
### Picking multiple files

```dart
List<String>? filesPaths = await PickOrSave().filePicker(
  params: FilePickerParams(filePickingType: FilePickingType.multiple),
);
```

### Saving single file

```dart
List<String>? result = await PickOrSave().fileSaver(
  params: FileSaverParams(
    sourceFilesPaths: [filePath],
    filesNames: ["file.png"],
  ),
);
String savedFilePath = result[0];
```

### Saving multiple files

```dart
List<String>? savedFilesPaths = await PickOrSave().fileSaver(
  params: FileSaverParams(
    sourceFilesPaths: [file1Path, file2Path],
    filesNames: ["file 1.png, file 2.png"],
  ),
);
```

| Saving single file  | Saving multiple files |
| ------------- | ------------- |
| ![WhatsApp Image 2022-09-19 at 1 34 02 PM](https://user-images.githubusercontent.com/85361211/190974633-6aab39c9-e817-4b92-84ed-b3fd0a4405b9.jpeg) | ![WhatsApp Image 2022-09-19 at 1 33 04 PM](https://user-images.githubusercontent.com/85361211/190974687-fa5f0ba1-391f-4103-8ffc-acdf9c8bca73.jpeg) |
