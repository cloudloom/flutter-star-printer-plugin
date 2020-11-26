package io.cloudloom.interplay.flutter_star_printer_plugin;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;

import java.util.HashMap;

public class SendCommand extends AsyncTask<Object, Void, StarPrinterStatus> {
    Context context;

    @Override
    protected StarPrinterStatus doInBackground(Object... objects) {
        String portName = (String) objects[0];
        String portSettings = (String) objects[1];
        byte[] commands = (byte[]) objects[2];
        context = (Context) objects[3];
        StarIOPort port = null;

        try {
            port = StarIOPort.getPort(portName, portSettings, 10000, context);

            StarPrinterStatus status = port.beginCheckedBlock();

            if (status.offline) {
                Log.e("FLUTTER_PRINT_ERROR", "Printer is offline");
                throw new StarIOPortException("Printer is offline");
            }

            port.writePort(commands, 0, commands.length);
            port.setEndCheckedBlockTimeoutMillis(30000); // Change the timeout time of endCheckedBlock method.
            status = port.endCheckedBlock();

            return status;

        } catch (Exception e) {
            Log.e("FLUTTER_PRINT_ERROR", e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(StarPrinterStatus status) {
        Log.e("FLUTTER_PRINT_ERROR", status);
        try{
            if (status != null) {
                super.onPostExecute(status);

                if (status.coverOpen) {
                    Toast.makeText(context, "Cover open", Toast.LENGTH_LONG).show();
                    Log.e("FLUTTER_PRINT_ERROR", "Cover open");
                } else if (status.receiptPaperEmpty) {
                    Toast.makeText(context, "Empty paper", Toast.LENGTH_LONG).show();
                    Log.e("FLUTTER_PRINT_ERROR", "Empty paper");
                } else if (status.offline) {
                    Toast.makeText(context, "Printer offline", Toast.LENGTH_LONG).show();
                    Log.e("FLUTTER_PRINT_ERROR", "Printer offline");
                } else {
                    Toast.makeText(context, "Success", Toast.LENGTH_LONG).show();
                    Log.i("FLUTTER_PRINT_SUCCESS", "Success");
                }
            }
        } catch (e){
            return null;
        }
    }
}
