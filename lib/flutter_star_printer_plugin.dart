
import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_star_printer_plugin/portInfo.dart';
import 'package:flutter_star_printer_plugin/enums.dart';
import 'package:flutter_star_printer_plugin/print_commands.dart';
import 'package:fluttertoast/fluttertoast.dart';

export 'enums.dart';
export 'portInfo.dart';
export 'print_commands.dart';

class StarPrinter {
  static const MethodChannel _channel =
      const MethodChannel('flutter_star_printer_plugin');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<List<PortInfo>> portDiscovery(String portType) async {
    dynamic type = portType;
    dynamic result = await _channel.invokeMethod('portDiscovery', {'type': type});
    if (result is List) {
      if (result.length > 0) {

        return result.map<PortInfo>((port) {
          return PortInfo(port);
        }).toList();
      } else {
        return null;
      }

    } else {
      return null;
    }
  }

  static Future<dynamic> checkStatus({
    @required String portName,
    @required String emulation,
  }) async {
    dynamic result = await _channel.invokeMethod('checkStatus', {
      'portName': portName,
      'emulation': emulation,
    });

    return result;
  }

  static Future<dynamic> print(
      {@required String portName,
        @required String emulation,
        @required PrintCommands printCommands}) async {
    dynamic result = await _channel.invokeMethod('print', {
      'portName': portName,
      'emulation': emulation,
      'printCommands': printCommands.getCommands(),
    });
    return result;
  }

  static Future<dynamic> connect({
    @required String portName,
    @required String emulation,
    bool hasBarcodeReader = false,
  }) async {
    dynamic result = await _channel.invokeMethod('connect', {
      'portName': portName,
      'emulation': emulation,
      'hasBarcodeReader': hasBarcodeReader,
    });
    return result;
  }

}
