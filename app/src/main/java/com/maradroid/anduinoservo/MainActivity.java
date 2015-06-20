package com.maradroid.anduinoservo;

import android.app.Activity;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity {

    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothSocket mSocket = null;
    private OutputStream mOutStream = null;
    private static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Button povezi;
    private SeekBar seekBar;
    private TextView tv;
    private boolean spojeno = false;
    private String[] devices;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        povezi = (Button) findViewById(R.id.button);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        tv = (TextView) findViewById(R.id.textView);
        tv.setText("0");
        seekBar.setEnabled(false);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv.setText(""+progress);
                String message = "" + seekBar.getProgress();
                message += "\n";
                try {
                    mOutStream.write(message.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),
                            "Slanje poruke neuspjašno!", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    public void Povezi (View v){

        if(spojeno){
            if(mSocket!=null){
                try {
                    mSocket.close();
                    povezi.setText("Poveži");
                    seekBar.setEnabled(false);
                    spojeno = false;
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(),
                        "Veza nije prekinuta!\nPokušajte ponovno kasnije.", Toast.LENGTH_SHORT)
                        .show();
                }
            }
        }else{

            if (mBluetoothAdapter == null) {
                Toast.makeText(getApplicationContext(),
                        "Bluetooth trenutno nedostupan!", Toast.LENGTH_SHORT)
                        .show();
            }else if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
            else{
                pairedDevices = mBluetoothAdapter.getBondedDevices();
                devices = new String[pairedDevices.size()];
                if(pairedDevices.size() > 0)
                {
                    int i = 0;
                    for(BluetoothDevice device : pairedDevices)
                    {
                        devices[i] = device.getName();
                        i++;
                    }
                    CustomDialog("odabir");

                }else{
                    CustomDialog("upozorenje");
                }
            }
        }
    }

    public void UspostavaVeze(int position){

        Object[] o = new Object[pairedDevices.size()];
        o = pairedDevices.toArray();
        final BluetoothDevice bluetoothDevice = (BluetoothDevice) o[position];
        Toast.makeText(getApplicationContext(),"Uspostava veze...", Toast.LENGTH_SHORT).show();


        new Thread(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.cancelDiscovery();
                try {
                    mSocket = bluetoothDevice.createRfcommSocketToServiceRecord(mUUID);
                    mSocket.connect();
                    mOutStream = mSocket.getOutputStream();

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Veza uspostavljena!", Toast.LENGTH_SHORT)
                                    .show();

                            seekBar.setEnabled(true);
                            tv.setEnabled(true);
                            spojeno = true;
                            povezi.setText("Odustani");
                            if(dialog!=null) dialog.cancel();
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        mSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Veza nije uspostavljena!", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                }
            }
        }).start();
    }

    public void CustomDialog(String string){
        dialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(this.LAYOUT_INFLATER_SERVICE);
        if(string.equals("odabir")){
            View view = inflater.inflate(R.layout.dialog_layout, null, false);
            dialog.setContentView(view);
            ListView lv = (ListView) dialog.findViewById(R.id.list);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,R.layout.listview_item, R.id.list_item, devices);
            lv.setAdapter(arrayAdapter);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    UspostavaVeze(position);
                }
            });
        }else if(string.equals("upozorenje")){
            View view = inflater.inflate(R.layout.error_dialog, null, false);
            dialog.setContentView(view);
            TextView upozorenje = (TextView) dialog.findViewById(R.id.upozorenje_tv);
            upozorenje.setText("Prije spajanja na željeni uređaj potrebno je upariti se s njim u Bluetooth postavkama telefona!");
            upozorenje.setTextColor(Color.WHITE);
        }


        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode ==1 && resultCode == -1){
            Toast.makeText(getApplicationContext(),"Bluetooth uključen!\nPonovno pritisnite gumb Poveži!", Toast.LENGTH_LONG).show();
        }else if (requestCode == 1 && resultCode == 0) {
            Toast.makeText(getApplicationContext(),"Bluetooth nije uključen!", Toast.LENGTH_LONG).show();
        }
        Log.e("gfdsgfdsg",""+resultCode);
    }

    public void Odustani(View v){
        if(dialog!=null) dialog.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mSocket!=null){
            try {
                mSocket.close();
            } catch (IOException e) { }
        }

    }
}
