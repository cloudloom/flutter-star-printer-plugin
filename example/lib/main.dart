import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_star_printer_plugin/flutter_star_printer_plugin.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:flutter_bugfender/flutter_bugfender.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await StarPrinter.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    FlutterBugfender.init("7QCDBeNhi8gVq0BnZH0UEODby6WRALwu");
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('United: Print Test'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              Text('Running on: $_platformVersion\n'),
              RaisedButton(
                child: Text('Test Print'),
                onPressed: () async {
                  List<PortInfo> list = await StarPrinter.portDiscovery("LAN");

                  if (list.length > 0) {
                    list.forEach((port) async {
                      if (port.portName.isNotEmpty) {

                        Fluttertoast.showToast(
                            msg: port.portName.toString(),
                            toastLength: Toast.LENGTH_LONG,
                            gravity: ToastGravity.BOTTOM,
                            timeInSecForIosWeb: 1,
                            backgroundColor: Colors.black54,
                            textColor: Colors.white,
                            fontSize: 16.0
                        );

                        // await StarPrinter.checkStatus( portName: port.portName, emulation: 'StarLine');

                        PrintCommands commands = PrintCommands();
//                        String one = "\n" +
//                            "United Cinemas \n" +
//                            "4 Vuko Place\n" +
//                            "Warriewood NSW 2102\n" +
//                            "\n";
//                        commands.push({
//                          'appendAlignment': 'Center',
//                          'append': one
//                        });
//
//                        String two = "Date: 27/08/2020\n" +
//                            "---------------------------------------------------------------------\n" +
//                            "\n";
//                        commands.push({
//                          'appendAlignment': "Left",
//                          'append': two
//                        });
//
//                        commands.push({
//                          'appendEmphasis': "SALE\n",
//                        });
//
//                        String three = "SKU                        Description                          Total\n" +
//                            "300678566                  PLAIN T-SHIRT                        10.99\n" +
//                            "300692003                  BLACK DENIM                          29.99\n" +
//                            "300651148                  BLUE DENIM                           29.99\n" +
//                            "300642980                  STRIPED DRESS                        49.99\n" +
//                            "300638471                  BLACK BOOTS                          35.99\n" +
//                            "\n" +
//                            "Subtotal                                                       156.95\n" +
//                            "Tax                                                              0.00\n" +
//                            "---------------------------------------------------------------------\n";
//                        commands.push({
//                          'append': three
//                        });
//
//                        String four = "Total                                            ";
//                        commands.push({
//                          'append': four
//                        });
//
//                        String five = "   \$156.95\n";
//                        commands.push({
//                          'width': 2,
//                          'height': 2,
//                          'appendMultiple': five,
//                        });
//
//                        String six = "---------------------------------------------------------------------\n" +
//                            "\n\n";
//                        commands.push({
//                          'append': six
//                        });
//
//                        commands.push({
//                          'appendPeripheral': 2
//                        });

//todo united ticket
//                        String one = "United Cinemas Rockingham\n" +
//                            "14 Leghorn Street Rockingham WA 6168\n" +
//                            "\n";
//                        commands.push({
//                          'append': one
//                        });
//
//                        String two = "Made In Italy\n" +
//                            "31/08/2020 2:20PM - 4:06PM\n" +
//                            "Cinema 6 - K01\n" +
//                            "\n";
//                        commands.push({
//                          'width': 2,
//                          'height': 2,
//                          'appendMultiple': two,
//                        });
//
//                        String three =  "Order 35352 | Ticket: Standard Adult\n" +
//                            "Date: 31/08/2020 2:20 PM | Interplay POS\n" +
//                            "\n";
//                        commands.push({
//                          'append': three
//                        });
//
//                        commands.push({
//                          'appendCutPaper': "FullCutWithFeed"
//                        });

//todo united receipt
                        String one = "\n" +
                            "United Cinemas Rockingham\n";
                        commands.push({
                          'width': 2,
                          'height': 2,
                          'appendMultiple': one,
                        });

                        String two = "14 Leghorn Street Rockingham WA 6168\n" +
                            "ABN: 53 572 683 298\n" +
                            "\n";
                        commands.push({
                          'append': two
                        });

                        String three = "\$13.20 - 1 x Combo P/C Drink - Medium\n" +
                            "\$10.00 - 1 x Standard Adult\n" +
                            "\n";
                        commands.push({
                          'append': three,
                        });

                        String four = "Total - \$23.20\n";
                        commands.push({
                          'width': 1,
                          'height': 2,
                          'appendMultiple': four,
                        });

                        String five = "Inclusive of GST if applicable.\n" +
                            "\n";
                        commands.push({
                          'append': five,
                        });

                        String six =  "Order 35352\n" +
                            "Date: 31/08/2020 2:20 PM | Interplay POS v1.0.1\n" +
                            "\n";
                        commands.push({
                          'append': six
                        });

                        commands.push({
                          'appendCutPaper': "FullCutWithFeed"
                        });

                        await StarPrinter.print( portName: port.portName, emulation: 'StarLine', printCommands: commands);
                      }
                    });
                  }


                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
