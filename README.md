[![pub package](https://img.shields.io/pub/v/pick_or_save.svg)](https://pub.dev/packages/pick_or_save) [![wakatime](https://wakatime.com/badge/user/83f3b15d-49de-4c01-b8de-bbc132f11be1/project/a5e5bda6-1125-46d7-9dc4-5028186265ca.svg)](https://wakatime.com/badge/user/83f3b15d-49de-4c01-b8de-bbc132f11be1/project/a5e5bda6-1125-46d7-9dc4-5028186265ca)

## Word from creator

**Hello👋, This package is completely compatible with flutter and it also provides option to disable copying of file in cache when picking and provide Android Uri of picked file to work with which offer some real benifits such as getting original file metadata, filtering files before caching or caching them anytime later using Uri.**

**Yes, without a doubt, giving a free 👍 or ⭐ will encourage me to keep working on this plugin.**

## Package description

A Flutter file picking and saving package that enables you to pick or save a single file and multiple files.

## Features

- Works on Android 5.0 (API level 21) or later.
- Pick single file, multiple files with certain extensions or mime types.
- Supports photo picker on supported devices.
- Get meta data like name, size and last modified from from android uri or file path.
- Saves single file while allowing user to choose location and name.
- Saves multiple file while allowing user to choose location or directory for saving all files.
- Saves file from either file path or file data(Uint8List).
- Could limit picking a file to the local device only.
- Get cached file path from android uri or file path.

**Note:** If you are getting errors in you IDE after updating this plugin to newer version and the error contains works like Redeclaration, Conflicting declarations, Overload resolution ambiguity then to fix that you probably need to remove the older version of plugin from pub cache `C:\Users\username\AppData\Local\Pub\Cache\hosted\pub.dev\older_version` or simply run `flutter clean`.

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

**Note:** To try the demos shown in below gifs run the example included in this plugin.

**Note:** For most below examples we set ```getCachedFilePath = false``` to get uri path instead of absolute file path from picker. A Uri path can only be used in android native code. By default ```getCachedFilePath = true``` which will provide cached file path from picker.

### Picking

| Picking single file | Picking multiple files |
| :-----: | :---: |
| <img src="https://user-images.githubusercontent.com/85361211/201424225-477c38d1-a7d0-4f13-8771-167483825049.gif"></img> | <img src="https://user-images.githubusercontent.com/85361211/201424373-d73a4cfc-bf1e-4f02-9bd5-cee785fb5fc2.gif"></img> |

#### Picking single file and getting uri

```dart
List<String>? result = await PickOrSave().filePicker(
  params: FilePickerParams(getCachedFilePath = false),
);
String filePath = result[0];
```

#### Picking single file and getting cache path

```dart
List<String>? result = await PickOrSave().filePicker(
  params: FilePickerParams(getCachedFilePath = true),
);
String filePath = result[0];
```

**Note:-**

If `getCachedFilePath = true` then the returned path file name will be different from picked file name. This was done to avoid deleting or rewriting existing cache files with same name. But you can still get the original name by following the pattern.

For example:- If you pick a file with name "My Test File.pdf" then the cached file will be something like this "My Test File.8190480413118007032.pdf". From that we see the pattern would be "original name prefix"+"."+"random numbers"+"."+"file extension". So what we need to do is to just remove the "."+"random numbers" to get the real name. Look at the below code to do that:

```dart
String getRealName(String pickOrSaveCachedFileName) {
  int indexOfExtDot = pickOrSaveCachedFileName.lastIndexOf('.');
  if (indexOfExtDot == -1) {
    return pickOrSaveCachedFileName;
  } else {
    String fileExt =
        pickOrSaveCachedFileName.substring(indexOfExtDot).toLowerCase();
    String fileNameWithoutExtension = pickOrSaveCachedFileName.substring(
        0, pickOrSaveCachedFileName.length - fileExt.length);
    int indexOfRandomNumDot = fileNameWithoutExtension.lastIndexOf('.');
    if (indexOfRandomNumDot == -1) {
      return pickOrSaveCachedFileName;
    } else {
      String dotAndRandomNum =
          fileNameWithoutExtension.substring(indexOfRandomNumDot).toLowerCase();
      String fileNameWithoutDotAndRandomNumAndExtension =
          fileNameWithoutExtension.substring(
              0, fileNameWithoutExtension.length - dotAndRandomNum.length);
      return fileNameWithoutDotAndRandomNumAndExtension + fileExt;
    }
  }
}
```

#### Picking multiple files

```dart
List<String>? filesPaths = await PickOrSave().filePicker(
  params: FilePickerParams(getCachedFilePath = false, enableMultipleSelection: true),
);
```

#### Resticting picking files to certain mime types

```dart
List<String>? filesPaths = await PickOrSave().filePicker(
  params: FilePickerParams(getCachedFilePath = false, mimeTypesFilter: ["image/*", "application/pdf"]),
);
```

#### Resticting picking files to certain extensions

```dart
List<String>? filesPaths = await PickOrSave().filePicker(
  params: FilePickerParams(getCachedFilePath = false, allowedExtensions: [".txt", ".png"]),
);
```

**Note:** This plugin automatically tries to convert the extensions to their respective mime types if supported so that only those become selectable but that may fail if it fails to convert them. Still if a user manages to select other extension files then this plugin automatically discards those other extension files from selection.

#### Photo Picker

```dart
List<String>? filesPaths = await PickOrSave().filePicker(
  params: FilePickerParams(getCachedFilePath = false, pickerType: PickerType.photo, mimeTypesFilter: ["*/*"]),
);
```

**Note:** This will show new photo picker only on supported android devices and for unsupported android devices it will show default picker. And it always needs mime type and only first mime type in mimeTypesFilter list is used. So if you want to filter multiple types of files then make sure to provide `allowedExtensions` as that automatically discards other extension files from selection if selected by user.

| Photo picker on supported devices | Photo picker on unsupported devices |
| :-----: | :---: |
| <img src="https://user-images.githubusercontent.com/85361211/201423620-e5349867-1e40-400e-a032-9c51a290ecb7.gif"></img> | <img src="https://user-images.githubusercontent.com/85361211/201423773-e1b6f1a3-ae03-410d-9b61-81c43e2e1c04.gif"></img> |

### Saving

#### Saving single file from file path

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

#### Saving multiple files from file path

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
| :-------------: | :-------------: |
| <img src="https://user-images.githubusercontent.com/85361211/201424602-fbcca525-2bdf-47e3-b0ba-3e5f02adff3f.gif"></img> | <img src="https://user-images.githubusercontent.com/85361211/201424629-4009a6bd-8add-443d-ba6c-8cca711e9df5.gif"></img> |

### File Metadata

```dart
FileMetadata? result = await PickOrSave().fileMetaData(
  params: FileMetadataParams(filePath: filePath),
);
```

| Picking file and get its metadata |
| :-------------: |
| <img src="https://user-images.githubusercontent.com/85361211/201424872-23d258bb-91e5-409d-adbe-b42e21ede8a2.gif"></img> |

### Get cache file path from file Uri or absolute file path

```dart
String? result = await PickOrSave().cacheFilePathFromPath(
  params: CacheFilePathFromPathParams(filePath: filePath),
);
```

| Picking file and get its cached file path |
| :-------------: |
| <img src="https://user-images.githubusercontent.com/85361211/201424906-dd07fd11-48a4-4cd1-a833-20e75f26abab.gif"></img> |
