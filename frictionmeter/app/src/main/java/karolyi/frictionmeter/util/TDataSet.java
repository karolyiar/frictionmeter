package karolyi.frictionmeter.util;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.text.format.Time;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

import karolyi.frictionmeter.FrictionmeterActivity;

public class TDataSet {
    private ArrayList<XYZ> data_;
    private int globMaxPosition_, startPosition_, endPosition_;
    private float avg_, variance_;

    public TDataSet() { // konstruktor
        data_ = new ArrayList<XYZ>();
        clear();
    }

    public void clear() {
        data_.clear();
        globMaxPosition_ = 0;
        startPosition_ = 0;
        endPosition_ = 0;
        avg_ = -1f;
        variance_ = -1f;
    }

    public void add(float x, float y, float z) {
        data_.add( new XYZ(x, y, z) );
    }

    public float getAvg() {
        if ( avg_ <= 0 )
            calcAvg();
        return avg_;
    }

    public  float getVar() {
        if ( variance_ <= 0 ) {
            calcVar();
        }
        return variance_;
    }

    public void plotGraph(Canvas tempCanvas, int heightPx, int widthPx) {
        globMaxPosition_ = getGlobMaxPosition();

        /// globális max utáni első lokális maximum
        startPosition_ = getStartPosition();
        endPosition_ = getEndPosition();

        //Toast.makeText(getApplicationContext(), Integer.toString(startPosition_)+" "+Integer.toString(endPosition_)+" "+Integer.toString(globMaxPosition_), Toast.LENGTH_SHORT).show();

        float dx = (float)widthPx / data_.size();
        float dy = (float)heightPx / 1.05f / data_.get(globMaxPosition_).getXY();

        Paint p = new Paint();
        p.setColor( Color.WHITE );
        tempCanvas.drawRGB(0, 0, 0); /// háttér
        for (int i=0; i < data_.size(); i++) {
            if (i == startPosition_)
                p.setColor( Color.rgb(255, 165, 0) ); ///orange
            if (i == endPosition_)
                p.setColor( Color.WHITE );
            tempCanvas.drawPoint(i * dx, heightPx - dy * data_.get(i).getXY(), p);
        }
    }

    /**
     * A Downloads mappába ment, dátum+idő.csv fájlnévvel
     */
    public void exportData() {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
        File myDir = new File(root + ""); // Downloads mappába ment
        //myDir.mkdirs();
        Time now = new Time(Time.getCurrentTimezone());
        now.setToNow();
        String fname = "Data" + now.format("%y%m%d%H%M%S") + ".csv";
        File file = new File(myDir, fname);
        /*if (file.exists())
            file.delete();*/
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(("All,"+Integer.toString(data_.size())+"\n").getBytes());
            out.write(("From,"+Integer.toString(getStartPosition())+"\n").getBytes());
            out.write(("To,"+Integer.toString(getEndPosition())+"\n").getBytes());
            out.write("x,y,z,sqrt(x2+y2),sqrt(x2+y2)/z\n".getBytes());
            for( XYZ i : data_ )
                out.write( (i.getX() + "," + i.getY() + "," + i.getZ() + ","
                        + i.getXY() + "," + i.getXYpZ() + "\n" ).getBytes() );
            out.flush();
            out.close();
            Toast.makeText(FrictionmeterActivity.context, "File saved: "+root+"/"+fname, Toast.LENGTH_LONG).show();
        }
        catch (Exception e) {
            Toast.makeText(FrictionmeterActivity.context, "File writing failed "+e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void calcStartPosition() {
        startPosition_ = globMaxPosition_ + 2;
        float[] lastResults = { data_.get(globMaxPosition_).getXY(), data_.get(globMaxPosition_+1).getXY(), 0 };
        for (int i=globMaxPosition_+2;i<data_.size();i++) {
            lastResults[2] = data_.get(i).getXY();
            if (lastResults[0] < lastResults[1] && lastResults[1] > lastResults[2]) {
                startPosition_ = i-1;
                return;
            }
            lastResults[0] = lastResults[1];
            lastResults[1] = lastResults[2];
        }
        startPosition_ = 0;
    }

    private int getStartPosition() {
        if ( startPosition_==0 )
            calcStartPosition();
        return startPosition_;
    }

    private int getEndPosition() {
        if ( endPosition_==0 )
            calcEndPosition();
        return endPosition_;
    }

    private int getGlobMaxPosition() {
        if ( globMaxPosition_ == 0 )
            calcGlobMaxPosition();
        return globMaxPosition_;
    }

    private void calcEndPosition() {
        /* A végéről elindul, keres a MIN_DATA-nál nagyobb értéket, majd onnan az első maximum helyét
            adja vissza.
         */
        int endOfData = 0;
        //float MIN_DATA = 1f; /// változtatni?
        final float MIN_DATA = data_.get(data_.size()-1).getXY() + 0.5f; ///utolso ertek+0,5
        for (int i=data_.size()-3;i>0;i--) {
            if ( data_.get(i).getXY() > MIN_DATA ) {
                endOfData  = i;
                break;
            }
        }
        endPosition_ = endOfData;
        float[] lastResults = { data_.get(endOfData+1).getXY(), data_.get(endOfData).getXY(), 0 };
        for (int i=endOfData-1;i>0;i--) {
            lastResults[2] = data_.get(i).getXY();
            if (lastResults[0] < lastResults[1] && lastResults[1] > lastResults[2]) {
                endPosition_ = i+1;
                return;
            }
            lastResults[0] = lastResults[1];
            lastResults[1] = lastResults[2];
        }
        endPosition_ = 0;
    }

    private void calcGlobMaxPosition() {
        globMaxPosition_ = 0;
        float max = data_.get(globMaxPosition_).getXY();
        for (int i = 0; i < this.size(); i++) /// globális max keresés
            if (data_.get(i).getXY() > max) {
                globMaxPosition_ = i;
                max = data_.get(globMaxPosition_).getXY();
            }
    }

    /**
     * Átlagot számol, a start- és endposition között
     */
    private void calcAvg() {
        avg_ = 0f;
        for (int i = startPosition_; i < endPosition_; i++) {
            avg_ += data_.get(i).getXYpZ();
        }
        avg_ /= (endPosition_ - startPosition_);
    }

    private void calcVar() {
        float variance = 0f;
        for (int i=startPosition_; i < endPosition_; i++) {
            variance += (float)Math.pow( (double)data_.get(i).getXYpZ() - avg_, 2);
        }
        variance = (float)Math.sqrt( variance / (endPosition_ - startPosition_) / (endPosition_ - startPosition_-1) );
        variance_ = variance;
    }

    private int size() {
        return data_.size();
    }



    /*public XYZ get(int i) {
        return data_.get(i);
    }*/

}
