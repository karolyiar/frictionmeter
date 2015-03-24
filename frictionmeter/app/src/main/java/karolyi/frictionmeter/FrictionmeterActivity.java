package karolyi.frictionmeter;

import karolyi.frictionmeter.util.Measurements;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;


public class FrictionmeterActivity extends Activity implements SensorEventListener {
    public static Context context;
    private SensorManager sensorManager;
    private Button startButton, stopButton;
    private TextView acceleration, counterText, avgText, varText, varPText, allAvgText, allVarText, allVarPText;
    private ImageView imgGraph;
    private int sensorDelay;
    private Measurements measurements;
    private static float PROBABILITY = 0.9f;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Button resetButton, exportButton, nextButton, previousButton, deleteButton;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); ///fekvő, álló, állíthatóra

        measurements = Measurements.getInstance();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frictionmeter);
        startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                onStartMeasure();
            }
        } );
        stopButton = (Button)findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                onStopMeasure();
            }
        } );
        resetButton = (Button)findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (measurements.isRunning())
                    return;
                onStop();
                measurements.clear();
                counterText.setText( measurements.getCounterText() );
                writeTexts(); //crashel
            }
        } );
        exportButton = (Button)findViewById(R.id.exportButton);
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                measurements.exportData();
            }
        });
        nextButton = (Button)findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( measurements.setNextMeasurement() ) {
                    plotGraph();
                }
            }
        });
        previousButton = (Button)findViewById(R.id.previousButton);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( measurements.setPreviousMeasurement() ) {
                    plotGraph();
                }
            }
        });
        deleteButton = (Button)findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( measurements.deleteMeasurement() ) {
                    plotGraph();
                    counterText.setText( measurements.getCounterText() );
                }
                counterText.setText( measurements.getCounterText() );
            }
        });

        acceleration = (TextView)findViewById(R.id.acceleration);
        counterText = (TextView)findViewById(R.id.counterText);
        imgGraph = (ImageView)findViewById(R.id.graphImg);
        avgText = (TextView)findViewById(R.id.avgText);
        varText = (TextView)findViewById(R.id.varText);
        varPText = (TextView)findViewById(R.id.varPText);
        allAvgText = (TextView)findViewById(R.id.allAvgText);
        allVarText = (TextView)findViewById(R.id.allVarText);
        allVarPText = (TextView)findViewById(R.id.allVarPText);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorDelay = SensorManager.SENSOR_DELAY_GAME;  /// változtatni kell
        context = getApplicationContext();
    }

    public void onStartMeasure() {
        if (measurements.isRunning())
            return;
        measurements.startMeasurement();
        counterText.setText( measurements.getCounterText() );
        onResume();
        ///indítja a mérést
    }

    public void onStopMeasure() {
        if (!measurements.isRunning())
            return;
        measurements.stopMeasurement();
        onPause();
        plotGraph();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0], y = event.values[1], z = event.values[2];
            measurements.addNewData( x, y, z );
            acceleration.setText("X: "+x+
                    "\nY: "+y+
                    "\nZ: "+z);
        }
    }

    /**
     * Grafikon rajzolása
     */
    public void plotGraph() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int heightPx = metrics.heightPixels/2;
        int widthPx = metrics.widthPixels;
        //Toast.makeText(getApplicationContext(), Integer.toString(lokMaxPos1)+" "+Integer.toString(lokMaxPos2)+" "+Integer.toString(globMaxPos), Toast.LENGTH_SHORT).show();

        Bitmap bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(bitmap);
        imgGraph.draw(tempCanvas);
        measurements.plotMeasurement(tempCanvas, heightPx, widthPx);
        imgGraph.setImageBitmap(bitmap);
        writeTexts();
    }

    /**
     * Átlag, hibafeliratok
     */
    public void writeTexts() {
        String avg = context.getString(R.string.avg);
        String var = context.getString(R.string.var);
        String varP = context.getString(R.string.varP);
        if (measurements.size()==0) {
            avgText.setText( avg );
            varText.setText( var );
            allAvgText.setText( avg );
            allVarText.setText( var );
            varPText.setText( varP );
            allVarPText.setText( varP );
            return;
        }
        DecimalFormat df = new DecimalFormat("0.000");
        avgText.setText(avg + " " + df.format( measurements.getActualAvg() ));
        varText.setText(var + " ±" + df.format( measurements.getActualVar() ));
        allAvgText.setText(avg + " " + df.format(measurements.getAverage( PROBABILITY )));
        allVarText.setText(var + " ±" + df.format( measurements.getVariance( PROBABILITY ) ));
        df.applyPattern("0.0 %");
        varPText.setText(varP + " " + df.format( measurements.getActualVar() / measurements.getActualAvg() ));
        allVarPText.setText(varP + " " + df.format( measurements.getVariance( PROBABILITY ) / measurements.getAverage( PROBABILITY ) ));
        counterText.setText( measurements.getCounterText() );
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        if (measurements.isRunning()) {
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    sensorDelay);
        }
        else
            sensorManager.unregisterListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!measurements.isRunning())
            sensorManager.unregisterListener(this);
    }

}