package com.example.yusuf.connecttohost;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private String hostAddress;
    private int port = 0;
    Socket clientSocket;

    PrintWriter outputStream;
    BufferedReader inputStream;
    BufferedReader userInput;
    Scanner scannerInput;
    Scanner inputStream2;

    Button receiveFromServerBtn;
    Button sendToServerBtn;
    EditText clientEditTx;
    TextView serverMsgTxV;

    String editTextStr = "";
    Intent intent;

    EditText ipAddressField;
    EditText portField;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Views
        clientEditTx = (EditText)findViewById(R.id.clientEditTx);
        clientEditTx.setHint("Insert Message Here!"); // Hint message for user EditText.

        serverMsgTxV = (TextView)findViewById(R.id.serverMsgTxV);

        receiveFromServerBtn = (Button)findViewById(R.id.receiveFromServerBtn);
        receiveFromServerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call thread that reads server input.
                ReadServerInput readThreadObj = new ReadServerInput();
                readThreadObj.start();
            }
        });

        sendToServerBtn = (Button)findViewById(R.id.sendToServerBtn);
        sendToServerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve user text input from EditText field.
                editTextStr = clientEditTx.getText().toString(); // * If retrieving EditText is done outside an event handler, the EditText field text won't be retrieved.
                // Call thread that outputs client input to server.
                WriteToServer writeThreadObj = new WriteToServer();
                writeThreadObj.start();
                // Dismiss soft keyboard.
                InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(clientEditTx.getWindowToken(),0);
                // Clear EditText field.
                clientEditTx.setText("");
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        /*
        // Settings Menu item.
        if (id == R.id.action_settings) {
            return true;
        }
        */

        // Connect Menu item.
        if (id == R.id.action_connect) {
            // Show custom dialog fragment.
            DialogFragment dialogFragment = new ConnectionDialog();
            dialogFragment.show(this.getFragmentManager(),"connect to server");

            // Create Snackbar.
            Snackbar connectSnackBar = Snackbar.make(findViewById(R.id.grandParentLayout), "Connect to Server", Snackbar.LENGTH_SHORT);
            connectSnackBar.show();

            return true;
        }

        // Guide Menu item.
        else if (id == R.id.action_guide) {
            // Create Toast.
            String guideText = "1) Click on 'Connect' Option item." +
                    "2) Submit the port numer (server application port) and ip address (host ip address)." +
                    "3) Enter a message in textfield entry." +
                    "4) Press the 'Send Message' button." +
                    "5) Once the host submits a message, click on 'Receive Message button." +
                    "6) Rinse and repeat the steps from 3-5!";
            Toast guideToast = Toast.makeText(this, guideText, Toast.LENGTH_LONG);
            guideToast.show();

            return true;
        }
        // Refresh Menu item.
        else if (id == R.id.action_refresh) {
            // Finish and recreate Activity.
            finish();
            intent = getIntent();
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }


    // *** Network functionality must be done outside the main UI thread!


    private class OpenConnection extends Thread {
        public OpenConnection() {
            // Port and IP retrieved through dialog instead of here.
            //port = 6000; // Should be same port as host.
            //hostAddress = "192.168.1.65"; // <- If connected through WiFi, use private IP address ("192.168.1.???") of the server. // Should be same address as host.
        }
        public void run() {
            // * Opening the socket must be done inside the body of the run() and not in the constructor (or in the main thread), otherwise "NetworkOnMainThreadException" exception occurs.
            // Open Socket.
            try {
                clientSocket = new Socket(hostAddress,port);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    // Write text input to server.
    private class WriteToServer extends  Thread {
        @Override
        public void run() {
            super.run();
            try {
                // Open Output Stream.
                // *** Make sure that the second parameter of PrintWriter - autoFlush is set to the Boolean value (true)!
                outputStream = new PrintWriter(clientSocket.getOutputStream(), true);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            // Output user/client text input to server.
            outputStream.println(editTextStr);
        }

    }


    // Thread for reading and displaying server text input.
    private class ReadServerInput extends Thread {
        String serverMsg;
        public ReadServerInput() {
            serverMsg = "";
        }
        @Override
        public void run() {
            try {
                // Open Input Stream.
                inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                inputStream2 = new Scanner(clientSocket.getInputStream());
                // Store server input in a String variable.
                serverMsg = inputStream.readLine();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            // Views can only be manipulated in UI thread, so setting TextView text must be done in UI thread.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Display server input using TextView.
                    serverMsgTxV.setText(serverMsg);
                }
            });
        }
    }


    public class ConnectionDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Instantiate AlertDialog.Builder with its constructor.
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater.
            LayoutInflater inflater = getActivity().getLayoutInflater();
            // Inflate and set the layout for the dialog.
            alertDialogBuilder.setView(inflater.inflate(R.layout.connection_dialog,null)) // pass null as the parent view because its going in the dialog layout
            // Add action buttons.
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // Retrieve entered IP and Port from user Dialog entry.
                    ipAddressField = (EditText) getDialog().findViewById(R.id.ipAddressField); // "getDialog()"
                    portField = (EditText) getDialog().findViewById(R.id.portField);
                    hostAddress = ipAddressField.getText().toString();
                    String portStr = portField.getText().toString();
                    port = Integer.parseInt(portStr);
                    // Connect to server.
                    OpenConnection connectThreadObj = new OpenConnection();
                    connectThreadObj.start();
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // Dismiss dialog.
                    ConnectionDialog.this.getDialog().cancel();
                }
            });
            return alertDialogBuilder.create();
        }
    }


}
