package io.cloudloom.interplay.flutter_star_printer_plugin;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarPrinterStatus;

import java.util.HashMap;
import java.util.Map;

public class CheckStatus extends AsyncTask<Object, Void, HashMap<String, Object>> {
    Context context;

    @Override
    protected HashMap<String, Object> doInBackground(Object... objects) {
        String portName = (String) objects[0];
        String emulation = (String) objects[1];
        context = (Context) objects[2];
        StarIOPort port = null;

        try {
            String portSettings = getPortSettingsOption(emulation);
            port = StarIOPort.getPort(portName, portSettings, 10000);

            StarPrinterStatus status = port.retreiveStatus();
            Map<String, String> firmwareInformationMap = port.getFirmwareInformation();

            HashMap<String, Object> json = new HashMap<>();
            json.put("offline", status.offline);
            json.put("coverOpen", status.coverOpen);
            json.put("cutterError", status.cutterError);
            json.put("receiptPaperEmpty", status.receiptPaperEmpty);
            json.put("ModelName", firmwareInformationMap.get("ModelName"));
            json.put("FirmwareVersion", firmwareInformationMap.get("FirmwareVersion"));

            return json;

        } catch (Exception e) {
            // e.printStackTrace();
            Log.e("FLUTTER_STATUS_ERROR", e.getMessage());
            return null;
        } finally {
            if (port != null) {
                try {
                    StarIOPort.releasePort(port);
                } catch (Exception e) {
                    Log.e("FLUTTER_STATUS_ERROR", e.getMessage());
                }
            }
        }
    }

    @Override
    protected void onPostExecute(HashMap<String, Object> json) {
        super.onPostExecute(json);

        Boolean offline = (Boolean) json.get("offline");
        Toast.makeText(context, "Printer Online: " + !offline, Toast.LENGTH_LONG).show();
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
}
