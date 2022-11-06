## 1.0.5

* **Breaking:** `filePickingType` is replaced with `enableMultipleSelection`.
* Added support [photo picker](https://developer.android.com/training/data-storage/shared/photopicker).

  To use photo picker set `pickerType` to `PickerType.photo` and then set the `mimeTypesFilter`.

  Note 1: Photo picker only supports single mime type so `mimeTypesFilter` first value should be valid such as image mime(image/*) or video mime(video/*) or both(*/*).

  Note 2: For Photo picker `allowedExtensions` doesn't automatically combines with `mimeTypeFilter` by detecting mimeType from extensions because of note 1. But still the result would contain only files with extension provided in `allowedExtensions`.

  Note 3: If photo picker is not available on the users device then we automatically fallback to `PickerType.file`.

* Fixed `mimeTypesFilter` not working for `filePicker`.

## 1.0.3

* **Breaking:** `mimeTypeFilter` is replaced with `mimeTypesFilter` everywhere.
* Fixed `localOnly` for `fileSaver` and `filePicker` not working as intended.
* Fixed `fileSaver` only opening download folder for when saving single file due to the updates made in version 1.0.1.
* Fixed `mimeTypesFilter` not working for `fileSaver`.
* Fixed `fileSaver` giving error instead of file paths when saving multiple files even after saving files successfully due to the updates made in version 1.0.1.

## 1.0.2

* Fixed `copyFileToCacheDir` and few other things not working due to the updates made in version 1.0.1.
* Added `cacheFilePathFromUri` to create a cache file from uri and get the cached file path.

## 1.0.1

* **Breaking:** Now `allowedExtensions` automatically combines with `mimeTypeFilter` by detecting mimeType from extensions.
* **Breaking:** Use `saveFiles` as `sourceFilesPaths`, `data`, `filesNames` are removed (see updated documentation or example).

  `saveFiles` takes list of `SaveFileInfo()` and `SaveFileInfo()` takes `filePath`, `fileData` and `fileName`.

  This removes the limitation of only using either file path or file data for saving files as it can take many `SaveFileInfo()` objects created through file path or file data.

* Project completely refactored.

## 0.1.3

* Fixed `allowedExtensions` for files picking.

## 0.1.1

* Added `cancelFilesSaving()` to cancel saving files.
* Automatically cancels ongoing saving of files when trying to save new files.
* Fixed example.

## 0.0.9

* Added more assertions on saving files.

## 0.0.8

* Tackles exceptions and errors.
* Fixes "Dialog already active" error on retrying a failed save.

## 0.0.7

* Fixes file metadata not working for URI.

## 0.0.6

* Allows using absolute file path for metadata.
* **Breaking:** Use ```sourceFileUri``` for URIs and ```sourceFilePath``` for absolute file paths to get metadata.

## 0.0.5

* Fixes file metadata last modified format.

## 0.0.4

* Adds function to get metadata of a uri.

## 0.0.3

* Fixes mimeTypeFilter not working.

## 0.0.2

* Fixes changelog.

## 0.0.1

* Initial release.