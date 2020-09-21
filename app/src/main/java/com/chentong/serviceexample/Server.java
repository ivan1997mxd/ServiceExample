package com.chentong.serviceexample;


import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * This class facilitates the communication between this app and the server.
 * <p>
 * It holds methods and data needed to send/receive requests.
 */
public class Server extends AsyncTask<String, Void, String> {

    public AsyncResponse asyncResponse;
    public void setOnAsyncResponse(AsyncResponse asyncResponse){
        this.asyncResponse = asyncResponse;
    }
    /*
        The IP Addresses stored in URL_STRING are temporary.
        When a proper server is set up, the IP Addresses will be changed to the server address.
     */

    private static final String URL_STRING = "http://192.168.12.3:5000/map";    // To save test data
    //private static final String URL_STRING = "http://142.55.230.222:5000/update";       // Ethernet IP in Office
    //private static final String URL_STRING = "http://10.16.4.94:5000/update";           // WiFi IP in StudentUnion building
    private static final String METHOD = "PUT"; // Server is set to respond only to PUT requests.

    public Server() {
    }

    /**
     * This method is specifically used by the WifiReceiver object.
     * Because the WifiReceiver only needs to send a JSONObject and no other data,
     * this method provides that signature.
     *
     * @param jObject the JSON object to be sent to the server.
     */
    // TODO: Generalize this in the future?
    public void execute(JSONObject jObject) {
        super.execute(URL_STRING, jObject.toString());
    }

    @Override
    protected String doInBackground(String... params) {
        Log.i(Tags.SERVER, "Starting to send a request to " + URL_STRING);
        /*
            Note about params:

            params[0] = URL request is being sent to, which is saved in URL_STRING.
            params[1] = The JSON object being sent, already converted to a string.

            The current implementation does not use any further parameters, but Android/Java
            requires this method signature to properly implement doInBackground().
         */

        StringBuilder stringBuilder = new StringBuilder();
        Log.i(Tags.SERVER, "PutData=" + params[1]);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(params[0]).openConnection();
            conn.setRequestMethod(METHOD);
            conn.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes("PutData=" + params[1]);
            wr.flush();
            wr.close();

            InputStream in = conn.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(in);

            int inputStreamData = inputStreamReader.read();
            while (inputStreamData != -1) {
                char current = (char) inputStreamData;
                inputStreamData = inputStreamReader.read();
                stringBuilder.append(current);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        Log.i(Tags.SERVER, "Request sent, and response received.");
        return stringBuilder.toString();
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.i(Tags.SERVER, "Response: " + result);
        if (result!=null){
            asyncResponse.onDataReceivedSuccess(result);
        }else {
            asyncResponse.onDataReceivedFailed();
        }
    }

}