# flutter_star_printer_plugin

A flutter plugin project for [Star micronics printers](http://www.starmicronics.com/pages/All-Products).

## Getting Started

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/developing-packages/),
a specialized package that includes platform-specific implementation code for
Android.

For help getting started with Flutter, view our
[online documentation](https://flutter.dev/docs), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

```dart

import 'package:flutter_star_printer_plugin/flutter_star_printer_plugin.dart';

// find and list all the printers
List<PortInfo> list = await StarPrinter.portDiscovery("LAN");

// check status
// await StarPrinter.checkStatus( portName: port.portName, emulation: 'StarLine');

// Print command
PrintCommands commands = PrintCommands();

String title = "Example Title\n\n";
commands.push({
  'width': 2,
  'height': 2,
  'appendMultiple': title,
});

String text = "This is an example text for printing.\n";
commands.push({
  'append': text
});

commands.push({
  'appendCutPaper': "FullCutWithFeed"
});

await StarPrinter.print( portName: port.portName, emulation: 'StarLine', printCommands: commands);

```

## Android

Permissions required depending on your printer:

```xml
<uses-permission android:name="android.permission.INTERNET"></uses-permission>
<uses-permission android:name="android.permission.BLUETOOTH"></uses-permission>
```

## Documentation work in progress.

