package io.cloudloom.interplay.flutter_star_printer_plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import com.bugfender.sdk.Bugfender;
import com.starmicronics.stario.PortInfo;
import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;
import com.starmicronics.starioextension.ICommandBuilder;
import com.starmicronics.starioextension.ICommandBuilder.CodePageType;
import com.starmicronics.starioextension.ICommandBuilder.CutPaperAction;
import com.starmicronics.starioextension.IConnectionCallback;
import com.starmicronics.starioextension.StarIoExt;
import com.starmicronics.starioextension.StarIoExt.Emulation;
import com.starmicronics.starioextension.StarIoExtManager;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** FlutterStarPrinterPlugin */
public class FlutterStarPrinterPlugin implements FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    protected Context applicationContext;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "flutter_star_printer_plugin");
        channel.setMethodCallHandler(this);
        applicationContext = flutterPluginBinding.getApplicationContext();

        Bugfender.init(applicationContext, "7QCDBeNhi8gVq0BnZH0UEODby6WRALwu", BuildConfig.DEBUG);
        Bugfender.enableLogcatLogging();
        Bugfender.enableCrashReporting();
        Bugfender.setDeviceString("Device Id", "1234567890-04");
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_star_printer_plugin");
        channel.setMethodCallHandler(new FlutterStarPrinterPlugin());
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;
            case "portDiscovery":
                portDiscovery(call, result);
                break;
            case "checkStatus":
                checkStatus(call, result);
                break;
            case "print":
                print(call, result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    public void portDiscovery(@NonNull MethodCall call, @NonNull Result result) {
        String strInterface = call.argument("type");
        ArrayList<Map<String, String>> response;

        try {
            switch (strInterface) {
                case "LAN":
                    response = getPortDiscovery("LAN");
                    break;
                case "Bluetooth":
                    response = getPortDiscovery("Bluetooth");
                    break;
                case "USB":
                    response = getPortDiscovery("USB");
                    break;
                default:
                    response = getPortDiscovery("All");
                    break;
            }
            result.success(response);
        } catch (Exception e) {
            e.printStackTrace();
            result.error("PORT_DISCOVERY_ERROR", e.getMessage(), null);
        }
    }

    public ArrayList<Map<String, String>> getPortDiscovery(@NonNull String interfaceName) {
        ArrayList<PortInfo> arrayDiscovery = new ArrayList<>();
        ArrayList<Map<String, String>> arrayPorts = new ArrayList<>();

        if (interfaceName.equals("Bluetooth") || interfaceName.equals("All")) {
            try {
                for (PortInfo portInfo: StarIOPort.searchPrinter("BT:")) {
                    arrayDiscovery.add(portInfo);
                }
            } catch (StarIOPortException e) {
                e.printStackTrace();
            }
        }
        if (interfaceName.equals("LAN") || interfaceName.equals("All")) {
            try {
                for (PortInfo portInfo: StarIOPort.searchPrinter("TCP:")) {
                    arrayDiscovery.add(portInfo);
                }
            } catch (StarIOPortException e) {
                e.printStackTrace();
            }
        }
        if (interfaceName.equals("USB") || interfaceName.equals("All")) {
            try {
                for (PortInfo portInfo: StarIOPort.searchPrinter("USB:")) {
                    arrayDiscovery.add(portInfo);
                }
            } catch (StarIOPortException e) {
                e.printStackTrace();
            }
        }

        for (PortInfo discovery: arrayDiscovery) {
            HashMap<String, String> port = new HashMap<>();

            if (discovery.getPortName().startsWith("BT:")) {
                port.put("portName", "BT:" + discovery.getMacAddress());
            }
            else {
                port.put("portName", discovery.getPortName());
            }

            if (!discovery.getMacAddress().equals("")) {
                port.put("macAddress", discovery.getMacAddress());
                if (discovery.getPortName().startsWith("BT:")) {
                    port.put("modelName", discovery.getPortName());
                } else if (!discovery.getModelName().equals("")) {
                    port.put("modelName", discovery.getModelName());
                }
            } else if (interfaceName.equals("USB") || interfaceName.equals("All")) {
                if (!discovery.getModelName().equals("")) {
                    port.put("modelName", discovery.getModelName());
                }
                if (!discovery.getUSBSerialNumber().equals(" SN:")) {
                    port.put("USBSerialNumber", discovery.getUSBSerialNumber());
                }
            }

            arrayPorts.add(port);
        }

        return arrayPorts;
    }

    public void checkStatus(@NonNull MethodCall call, @NonNull Result result) {
        String portName = call.argument("portName");
        String emulation = call.argument("emulation");
        StarIOPort port = null;

        Toast.makeText(applicationContext, "emulation: " + emulation, Toast.LENGTH_LONG).show();

        try {
            CheckStatus checkStatus = new CheckStatus();
            checkStatus.execute(portName, emulation, applicationContext);
            result.success("status check called");
        } catch (Exception e) {
            result.error("CHECK_STATUS_ERROR", e.getMessage(), null);
        }


//        try {
//            String portSettings = getPortSettingsOption(emulation);
//            port = StarIOPort.getPort(portName, portSettings, 10000, applicationContext);
//
//            // A sleep is used to get time for the socket to completely open
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//            }
//
//            Toast.makeText(applicationContext, "PortSettings: " + port.getPortSettings(), Toast.LENGTH_LONG).show();
//
//            StarPrinterStatus status = port.retreiveStatus();
//            Map<String, String> firmwareInformationMap = port.getFirmwareInformation();
//
//            Toast.makeText(applicationContext, "Printer Online: " + !(status.offline), Toast.LENGTH_LONG).show();
//
//            HashMap<String, Object> json = new HashMap<>();
//            json.put("offline", status.offline);
//            json.put("coverOpen", status.coverOpen);
//            json.put("cutterError", status.cutterError);
//            json.put("receiptPaperEmpty", status.receiptPaperEmpty);
//            json.put("ModelName", firmwareInformationMap.get("ModelName"));
//            json.put("FirmwareVersion", firmwareInformationMap.get("FirmwareVersion"));
//
//            result.success(json);
//
//        } catch (Exception e) {
//            // e.printStackTrace();
//            result.error("CHECK_STATUS_ERROR", e.getMessage(), null);
//            Toast.makeText(applicationContext, "CHECK_STATUS_ERROR: " + e.toString(), Toast.LENGTH_LONG).show();
//        } finally {
//            if (port != null) {
//                try {
//                    StarIOPort.releasePort(port);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    result.error("CHECK_STATUS_ERROR", e.getMessage(), null);
//                }
//            }
//        }
    }

    public void print(@NonNull MethodCall call, @NonNull Result result) {
        String portName = call.argument("portName");
        String emulation = call.argument("emulation");
        ArrayList<Map<String, Object>> printCommands = call.argument("printCommands");

        try {
            ICommandBuilder builder = StarIoExt.createCommandBuilder(getEmulation(emulation));

            builder.beginDocument();
            appendCommands(builder, printCommands, applicationContext);
            builder.endDocument();
            sendCommand(portName, emulation, builder.getCommands(), applicationContext, result);

        } catch (Exception e) {
            Log.e("PRINT_ERROR", e.getMessage());
            Toast.makeText(applicationContext, "print_ERROR" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendCommand(String portName, String portSettings, byte[] commands, Context context, @NonNull Result result) {
        StarIOPort port = null;

        Toast.makeText(applicationContext, "send command " + portName + " " + portSettings, Toast.LENGTH_LONG).show();

        try{
            SendCommand sendCommand = new SendCommand();
            sendCommand.execute(portName, portSettings, commands, applicationContext);
            result.success("print called");
            Toast.makeText(applicationContext, "print called", Toast.LENGTH_LONG).show();
            Log.i("FLUTTER_PRINT_SUCCESS", "print called");
        } catch (Exception e) {
            result.error("print_ERROR", e.getMessage(), null);
            Log.e("FLUTTER_PRINT_ERROR", e.getMessage());
            Toast.makeText(applicationContext, "print_ERROR" + e.getMessage(), Toast.LENGTH_LONG).show();
        }

//        try {
//            port = StarIOPort.getPort(portName, portSettings, 10000, applicationContext);
//
//            Toast.makeText(applicationContext, port.getPortName(), Toast.LENGTH_LONG).show();
//
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//            }
//
//            StarPrinterStatus status = port.beginCheckedBlock();
//
//            if (status.offline) {
//                Toast.makeText(applicationContext, "Printer is offline", Toast.LENGTH_LONG).show();
//                throw new StarIOPortException("Printer is offline");
//            }
//
//            port.writePort(commands, 0, commands.length);
//            port.setEndCheckedBlockTimeoutMillis(30000); // Change the timeout time of endCheckedBlock method.
//            status = port.endCheckedBlock();
//
//            if (status.coverOpen) {
//                Toast.makeText(applicationContext, "Cover open", Toast.LENGTH_LONG).show();
//                result.error("STARIO_PORT_EXCEPTION", "Cover open", null);
//            } else if (status.receiptPaperEmpty) {
//                Toast.makeText(applicationContext, "Empty paper", Toast.LENGTH_LONG).show();
//                result.error("STARIO_PORT_EXCEPTION", "Empty paper", null);
//            } else if (status.offline) {
//                Toast.makeText(applicationContext, "Printer offline", Toast.LENGTH_LONG).show();
//                result.error("STARIO_PORT_EXCEPTION", "Printer offline", null);
//            } else {
//                Toast.makeText(applicationContext, "Success", Toast.LENGTH_LONG).show();
//                result.success("Success");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            result.error("STARIO_PORT_EXCEPTION", e.getMessage(), null);
//        }
    }

    private Emulation getEmulation(String emulation) {
        switch(emulation) {
            case "StarPRNT":
                return Emulation.StarPRNT;
            case "StarPRNTL":
                return Emulation.StarPRNTL;
            case "StarGraphic":
                return Emulation.StarGraphic;
            case "EscPos":
                return Emulation.EscPos;
            case "EscPosMobile":
                return Emulation.EscPosMobile;
            case "StarDotImpact":
                return Emulation.StarDotImpact;
            default:
                return Emulation.StarLine;
        }
    }

    private String getPortSettingsOption(String emulation) {
        switch(emulation) {
            case "EscPosMobile":
                return "mini";
            case "EscPos":
                return "escpos";
            case "StarPRNT":
            case "StarPRNTL":
                return "Portable";
            default:
                return emulation;
        }
    }

    private void appendCommands(ICommandBuilder builder, ArrayList<Map<String, Object>> printCommands, Context context) {
        Charset encoding = Charset.forName("US-ASCII");

        for (Map<String, Object> it: printCommands) {
            if (it.containsKey("appendCharacterSpace"))
                builder.appendCharacterSpace(((Integer)it.get("appendCharacterSpace")));
            else if (it.containsKey("appendEncoding"))
                encoding = getEncoding(it.get("appendEncoding").toString());
            else if (it.containsKey("appendCodePage"))
                builder.appendCodePage(getCodePageType(it.get("appendCodePage").toString()));
            else if (it.containsKey("appendPeripheral"))
                builder.appendPeripheral(getPeripheralChannel((Integer) it.get("appendPeripheral")));
            else if (it.containsKey("append"))
                builder.append(it.get("append").toString().getBytes(encoding));
            else if (it.containsKey("appendRaw"))
                builder.append(it.get("appendRaw").toString().getBytes(encoding));
            else if (it.containsKey("appendEmphasis"))
                builder.appendEmphasis(it.get("appendEmphasis").toString().getBytes(encoding));
            else if (it.containsKey("enableEmphasis"))
                builder.appendEmphasis(Boolean.parseBoolean(it.get("enableEmphasis").toString()));
            else if (it.containsKey("appendInvert"))
                builder.appendInvert(it.get("appendInvert").toString().getBytes(encoding));
            else if (it.containsKey("enableInvert"))
                builder.appendInvert(Boolean.parseBoolean(it.get("enableInvert").toString()));
            else if (it.containsKey("appendUnderline"))
                builder.appendUnderLine(it.get("appendUnderline").toString().getBytes(encoding));
            else if (it.containsKey("enableUnderline"))
                builder.appendUnderLine(Boolean.parseBoolean(it.get("enableUnderline").toString()));
            else if (it.containsKey("appendInternational"))
                builder.appendInternational(getInternational(it.get("appendInternational").toString()));
            else if (it.containsKey("appendLineFeed"))
                builder.appendLineFeed(((Integer)it.get("appendLineFeed")));
            else if (it.containsKey("appendUnitFeed"))
                builder.appendUnitFeed(((Integer)it.get("appendUnitFeed")));
            else if (it.containsKey("appendLineSpace"))
                builder.appendLineSpace(((Integer)it.get("appendLineSpace")));
            else if (it.containsKey("appendFontStyle"))
                builder.appendFontStyle((getFontStyle(it.get("appendFontStyle").toString())));
            else if (it.containsKey("appendCutPaper"))
                builder.appendCutPaper(getCutPaperAction(it.get("appendCutPaper").toString()));
            else if (it.containsKey("appendBlackMark"))
                builder.appendBlackMark(getBlackMarkType(it.get("appendBlackMark").toString()));
            else if (it.containsKey("appendBytes"))
                builder.append(it.get("appendBytes").toString().getBytes(encoding)); // TODO: to be tested
            else if (it.containsKey("appendRawBytes"))
                builder.appendRaw(it.get("appendRawBytes").toString().getBytes(encoding)); // TODO: to be tested
            else if (it.containsKey("appendAbsolutePosition")) {
                if (it.containsKey("data"))
                    builder.appendAbsolutePosition((it.get("data").toString().getBytes(encoding)), ((Integer)it.get("appendAbsolutePosition")));
                else
                    builder.appendAbsolutePosition((Integer)it.get("appendAbsolutePosition"));
            }
            else if (it.containsKey("appendAlignment")) {
                if (it.containsKey("data"))
                    builder.appendAlignment((it.get("data").toString().getBytes(encoding)), getAlignment(it.get("appendAlignment").toString()));
                else
                    builder.appendAlignment(getAlignment(it.get("appendAlignment").toString()));
            }
            else if (it.containsKey("appendHorizontalTabPosition"))
                builder.appendHorizontalTabPosition((int[]) it.get("appendHorizontalTabPosition")); // TODO: to be tested
            else if (it.containsKey("appendLogo")) {
                if (it.containsKey("logoSize")) builder.appendLogo(getLogoSize((String) it.get("logoSize")), (Integer) it.get("appendLogo"));
                else builder.appendLogo(getLogoSize("Normal"), (Integer) it.get("appendLogo"));
            }
            else if (it.containsKey("appendBarcode")) {
                ICommandBuilder.BarcodeSymbology barcodeSymbology = (it.containsKey("BarcodeSymbology")) ? getBarcodeSymbology(it.get("BarcodeSymbology").toString()) : getBarcodeSymbology("Code128");
                ICommandBuilder.BarcodeWidth barcodeWidth = (it.containsKey("BarcodeWidth")) ? getBarcodeWidth(it.get("BarcodeWidth").toString()) : getBarcodeWidth("Mode2");
                Integer height = (it.containsKey("height")) ? (Integer) (it.get("height")) : 40;
                Boolean hri = (it.containsKey("hri")) ? (Boolean) (it.get("hri")) : true;

                if (it.containsKey("absolutePosition")) {
                    builder.appendBarcodeWithAbsolutePosition(it.get("appendBarcode").toString().getBytes(encoding), barcodeSymbology, barcodeWidth, height, hri, (Integer) it.get("absolutePosition"));
                } else if (it.containsKey("alignment")) {
                    builder.appendBarcodeWithAlignment(it.get("appendBarcode").toString().getBytes(encoding), barcodeSymbology, barcodeWidth, height, hri, getAlignment(it.get("alignment").toString()));
                } else builder.appendBarcode(it.get("appendBarcode").toString().getBytes(encoding), barcodeSymbology, barcodeWidth, height, hri);
            }
            else if (it.containsKey("appendBitmap")) {
                Boolean diffusion = (it.containsKey("diffusion")) ? (Boolean) (it.get("diffusion")) : true;
                Integer width = (it.containsKey("width")) ? (Integer) (it.get("width")) : 576;
                Boolean bothScale = (it.containsKey("bothScale")) ? (Boolean) (it.get("bothScale")) : true;
                ICommandBuilder.BitmapConverterRotation rotation = (it.containsKey("rotation")) ? getConverterRotation(it.get("rotation").toString()) : getConverterRotation("Normal");

                try {
                    Uri imageUri = Uri.parse(String.valueOf(it.containsKey("appendBitmap")));
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);

                    if (it.containsKey("absolutePosition")) {
                        builder.appendBitmapWithAbsolutePosition(bitmap, diffusion, width, bothScale, rotation, (Integer) it.get("absolutePosition"));
                    }
                    else if (it.containsKey("alignment")) {
                        builder.appendBitmapWithAlignment(bitmap, diffusion, width, bothScale, rotation, getAlignment(it.get("alignment").toString()));
                    }
                    else builder.appendBitmap(bitmap, diffusion, width, bothScale, rotation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (it.containsKey("appendBitmapText")) {
                Float fontSize = (Float) ((it.containsKey("fontSize")) ? (it.get("fontSize")) : 25);
                Boolean diffusion = (it.containsKey("diffusion")) ? (Boolean) (it.get("diffusion")) : true;
                Integer width = (it.containsKey("width")) ? (Integer) (it.get("width")) : 576;
                Boolean bothScale = (it.containsKey("bothScale")) ? (Boolean) (it.get("bothScale")) : true;
                String text = it.get("appendBitmapText").toString();
                Typeface typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
                Bitmap bitmap = createBitmapFromText(text, fontSize, width, typeface);

                ICommandBuilder.BitmapConverterRotation rotation = (it.containsKey("rotation")) ? getConverterRotation(it.get("rotation").toString()) : getConverterRotation("Normal");

                if (it.containsKey("absolutePosition")) {
                    builder.appendBitmapWithAbsolutePosition(bitmap, diffusion, width, bothScale, rotation, (Integer) it.get("absolutePosition"));
                }
                else if (it.containsKey("alignment")) {
                    builder.appendBitmapWithAlignment(bitmap, diffusion, width, bothScale, rotation, getAlignment(it.get("alignment").toString()));
                }
                else builder.appendBitmap(bitmap, diffusion, width, bothScale, rotation);
            }
            else if (it.containsKey("appendMultiple")) {
                Integer width = (it.containsKey("width")) ? (Integer) (it.get("width")) : 2;
                Integer height = (it.containsKey("height")) ? (Integer) (it.get("height")) : 2;
                builder.appendMultiple(it.get("appendMultiple").toString().getBytes(encoding), width, height);
            }
        }
    }

    private Charset getEncoding(String encoding) {
        switch (encoding) {
            case "Windows-1252":
                try {
                    return Charset.forName("Windows-1252"); // French, German, Portuguese, Spanish
                } catch (UnsupportedCharsetException e) { // not supported using UTF-8 Instead
                    return StandardCharsets.UTF_8;
                }
            case "Shift-JIS":
                try {
                    return Charset.forName("Shift-JIS"); // Japanese
                } catch (UnsupportedCharsetException e) { // not supported using UTF-8 Instead
                    return StandardCharsets.UTF_8;
                }
            case "Windows-1251":
                try {
                    return Charset.forName("Windows-1251"); // Russian
                } catch (UnsupportedCharsetException e) { // not supported using UTF-8 Instead
                    return StandardCharsets.UTF_8;
                }
            case "GB2312":
                try {
                    return Charset.forName("GB2312"); // Simplified Chinese
                } catch (UnsupportedCharsetException e) { // not supported using UTF-8 Instead
                    return StandardCharsets.UTF_8;
                }
            case "Big5":
                try {
                    return Charset.forName("Big5"); // Traditional Chinese
                } catch (UnsupportedCharsetException e) { // not supported using UTF-8 Instead
                    return StandardCharsets.UTF_8;
                }
            case "UTF-8":
                return StandardCharsets.UTF_8; // UTF-8
            default:
                return StandardCharsets.US_ASCII;
        }
    }

    private ICommandBuilder.CodePageType getCodePageType(String codePageType) {
        switch (codePageType) {
            case "CP437":
                return CodePageType.CP437;
            case "CP737":
                return CodePageType.CP737;
            case "CP772":
                return CodePageType.CP772;
            case "CP774":
                return CodePageType.CP774;
            case "CP851":
                return CodePageType.CP851;
            case "CP852":
                return CodePageType.CP852;
            case "CP855":
                return CodePageType.CP855;
            case "CP857":
                return CodePageType.CP857;
            case "CP858":
                return CodePageType.CP858;
            case "CP860":
                return CodePageType.CP860;
            case "CP861":
                return CodePageType.CP861;
            case "CP862":
                return CodePageType.CP862;
            case "CP863":
                return CodePageType.CP863;
            case "CP864":
                return CodePageType.CP864;
            case "CP865":
                return CodePageType.CP866;
            case "CP869":
                return CodePageType.CP869;
            case "CP874":
                return CodePageType.CP874;
            case "CP928":
                return CodePageType.CP928;
            case "CP932":
                return CodePageType.CP932;
            case "CP999":
                return CodePageType.CP999;
            case "CP1001":
                return CodePageType.CP1001;
            case "CP1250":
                return CodePageType.CP1250;
            case "CP1251":
                return CodePageType.CP1251;
            case "CP1252":
                return CodePageType.CP1252;
            case "CP2001":
                return CodePageType.CP2001;
            case "CP3001":
                return CodePageType.CP3001;
            case "CP3002":
                return CodePageType.CP3002;
            case "CP3011":
                return CodePageType.CP3011;
            case "CP3012":
                return CodePageType.CP3012;
            case "CP3021":
                return CodePageType.CP3021;
            case "CP3041":
                return CodePageType.CP3041;
            case "CP3840":
                return CodePageType.CP3840;
            case "CP3841":
                return CodePageType.CP3841;
            case "CP3843":
                return CodePageType.CP3843;
            case "CP3845":
                return CodePageType.CP3845;
            case "CP3846":
                return CodePageType.CP3846;
            case "CP3847":
                return CodePageType.CP3847;
            case "CP3848":
                return CodePageType.CP3848;
            case "UTF8":
                return CodePageType.UTF8;
            case "Blank":
                return CodePageType.Blank;
            default:
                return CodePageType.CP998;
        }
    }

    private ICommandBuilder.InternationalType getInternational(String international) {
        switch (international) {
            case "UK":
                return ICommandBuilder.InternationalType.UK;
            case "France":
                return ICommandBuilder.InternationalType.France;
            case "Germany":
                return ICommandBuilder.InternationalType.Germany;
            case "Denmark":
                return ICommandBuilder.InternationalType.Denmark;
            case "Sweden":
                return ICommandBuilder.InternationalType.Sweden;
            case "Italy":
                return ICommandBuilder.InternationalType.Italy;
            case "Spain":
                return ICommandBuilder.InternationalType.Spain;
            case "Japan":
                return ICommandBuilder.InternationalType.Japan;
            case "Norway":
                return ICommandBuilder.InternationalType.Norway;
            case "Denmark2":
                return ICommandBuilder.InternationalType.Denmark2;
            case "Spain2":
                return ICommandBuilder.InternationalType.Spain2;
            case "LatinAmerica":
                return ICommandBuilder.InternationalType.LatinAmerica;
            case "Korea":
                return ICommandBuilder.InternationalType.Korea;
            case "Ireland":
                return ICommandBuilder.InternationalType.Ireland;
            case "Legal":
                return ICommandBuilder.InternationalType.Legal;
            default:
                return ICommandBuilder.InternationalType.USA;
        }
    }

    private ICommandBuilder.FontStyleType getFontStyle(String fontStyle) {
        if ("B".equals(fontStyle)) {
            return ICommandBuilder.FontStyleType.B;
        }
        return ICommandBuilder.FontStyleType.A;
    }

    private ICommandBuilder.CutPaperAction getCutPaperAction(String cutPaperAction) {
        switch (cutPaperAction) {
            case "FullCut":
                return CutPaperAction.FullCut;
            case "FullCutWithFeed":
                return CutPaperAction.FullCutWithFeed;
            case "PartialCut":
                return CutPaperAction.PartialCut;
            default:
                return CutPaperAction.PartialCutWithFeed;
        }
    }

    private ICommandBuilder.PeripheralChannel getPeripheralChannel(Integer peripheralChannel) {
        if (peripheralChannel == 1) return ICommandBuilder.PeripheralChannel.No1;
        else if (peripheralChannel == 2) return ICommandBuilder.PeripheralChannel.No2;
        else return ICommandBuilder.PeripheralChannel.No1;
    }

    private ICommandBuilder.BlackMarkType getBlackMarkType(String blackMarkType) {
        switch (blackMarkType) {
            case "Invalid":
                return ICommandBuilder.BlackMarkType.Invalid;
            case "ValidWithDetection":
                return ICommandBuilder.BlackMarkType.ValidWithDetection;
            default:
                return ICommandBuilder.BlackMarkType.Valid;
        }
    }

    private ICommandBuilder.AlignmentPosition getAlignment(String alignment) {
        switch (alignment) {
            case "Center":
                return ICommandBuilder.AlignmentPosition.Center;
            case "Right":
                return ICommandBuilder.AlignmentPosition.Right;
            default:
                return ICommandBuilder.AlignmentPosition.Left;
        }
    }

    private ICommandBuilder.LogoSize getLogoSize(String logoSize) {
        switch (logoSize) {
            case "DoubleWidth":
                return ICommandBuilder.LogoSize.DoubleWidth;
            case "DoubleHeight":
                return ICommandBuilder.LogoSize.DoubleHeight;
            case "DoubleWidthDoubleHeight":
                return ICommandBuilder.LogoSize.DoubleWidthDoubleHeight;
            default:
                return ICommandBuilder.LogoSize.Normal;
        }
    }

    private ICommandBuilder.BarcodeSymbology getBarcodeSymbology(String barcodeSymbology) {
        switch (barcodeSymbology) {
            case "Code39":
                return ICommandBuilder.BarcodeSymbology.Code39;
            case "Code93":
                return ICommandBuilder.BarcodeSymbology.Code93;
            case "ITF":
                return ICommandBuilder.BarcodeSymbology.ITF;
            case "JAN8":
                return ICommandBuilder.BarcodeSymbology.JAN8;
            case "JAN13":
                return ICommandBuilder.BarcodeSymbology.JAN13;
            case "NW7":
                return ICommandBuilder.BarcodeSymbology.NW7;
            case "UPCA":
                return ICommandBuilder.BarcodeSymbology.UPCA;
            case "UPCE":
                return ICommandBuilder.BarcodeSymbology.UPCE;
            default:
                return ICommandBuilder.BarcodeSymbology.Code128;
        }
    }

    private ICommandBuilder.BarcodeWidth getBarcodeWidth(String barcodeWidth) {
        switch (barcodeWidth) {
            case "Mode1":
                return ICommandBuilder.BarcodeWidth.Mode1;
            case "Mode3":
                return ICommandBuilder.BarcodeWidth.Mode3;
            case "Mode4":
                return ICommandBuilder.BarcodeWidth.Mode4;
            case "Mode5":
                return ICommandBuilder.BarcodeWidth.Mode5;
            case "Mode6":
                return ICommandBuilder.BarcodeWidth.Mode6;
            case "Mode7":
                return ICommandBuilder.BarcodeWidth.Mode7;
            case "Mode8":
                return ICommandBuilder.BarcodeWidth.Mode8;
            case "Mode9":
                return ICommandBuilder.BarcodeWidth.Mode9;
            default:
                return ICommandBuilder.BarcodeWidth.Mode2;
        }
    }

    private ICommandBuilder.BitmapConverterRotation getConverterRotation(String converterRotation) {
        switch (converterRotation) {
            case "Left90":
                return ICommandBuilder.BitmapConverterRotation.Left90;
            case "Right90":
                return ICommandBuilder.BitmapConverterRotation.Right90;
            case "Rotate180":
                return ICommandBuilder.BitmapConverterRotation.Rotate180;
            default:
                return ICommandBuilder.BitmapConverterRotation.Normal;
        }
    }

    private Bitmap createBitmapFromText(String printText, Float textSize, Integer printWidth, Typeface typeface) {
        Paint paint = new Paint();
        paint.setTextSize(textSize);
        paint.setTypeface(typeface);
        paint.getTextBounds(printText, 0, printText.length(), new Rect());

        TextPaint textPaint = new TextPaint(paint);
        StaticLayout staticLayout = new StaticLayout(printText, textPaint, printWidth, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);

        // Create bitmap
        Bitmap bitmap = Bitmap.createBitmap(staticLayout.getWidth(), staticLayout.getHeight(), Bitmap.Config.ARGB_8888);

        // Create canvas
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        canvas.translate(0, 0);
        staticLayout.draw(canvas);

        return bitmap;
    }
}
