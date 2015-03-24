package karolyi.frictionmeter.util;

import android.graphics.Canvas;

import java.util.ArrayList;

/** Singleton pattern
 * Ebben tárolja, kezeli a dataSet-eket
 **/
public class Measurements {
    private static Measurements instance = null;
    private ArrayList<TDataSet> results_;
    private float average_, variance_, probability_;
    private boolean isAvgVarComputed;
    private int actualMeasurementNum_;
    private TDataSet actualMeasurement_;
    private boolean isRunning;


    private Measurements() {
        results_ = new ArrayList<TDataSet>();
        clear();
    }

    public static Measurements getInstance() {
        if (instance == null)
            instance = new Measurements();
        return instance;
    }

    /** Reseteli az objektumot, a méréseket */
    public void clear() {
        average_ = -1f;
        variance_ = -1f;
        actualMeasurementNum_ = -1;
        actualMeasurement_ = null;
        isAvgVarComputed = false;
        isRunning = false;
        results_.clear();
    }

    /** A meresek szamat adja meg **/
    public int size() {
        return results_.size();
    }

    /** Számláló értékét határozza meg */
    public String getCounterText() {
        if (results_.size()<=0) {
            return "0/0";
        }
        return Integer.toString(actualMeasurementNum_+1) + " / " + Integer.toString(results_.size());
    }

    /** Új mérést indít */
    public void addNewMeasurement() {
        actualMeasurement_ = new TDataSet();
        results_.add ( actualMeasurement_ );
        actualMeasurementNum_ = results_.size()-1;
        isAvgVarComputed = false;
    }

    /** Aktuális méréshez hozzáadja az adatokat */
    public void addNewData(float x, float y, float z) {
        actualMeasurement_.add(x, y, z);
    }

    /** Következő gombra indul
     * Ha fut mérés, nem csinál semmit
     * Ha az utolsón áll, nem csinál semmit
     */
    public boolean setNextMeasurement() {
        if (isRunning)
            return false;
        if (actualMeasurementNum_ >= results_.size()-1)
            return false;
        actualMeasurementNum_ ++;
        actualMeasurement_ = results_.get(actualMeasurementNum_);
        return true;
    }

    /**
     * (visszagombra) az előző mérés lesz az aktuális
     * @return Ha sikerült, True
     */
    public boolean setPreviousMeasurement() {
        if (isRunning)
            return false;
        if (actualMeasurementNum_ <= 0)
            return false;
        actualMeasurementNum_ --;
        actualMeasurement_ = results_.get(actualMeasurementNum_);
        return true;
    }

    /**
     * Törli az aktuális mérést
     * @return ha sikerült, True
     */
    public boolean deleteMeasurement() {
        if (isRunning)
            return false;
        isAvgVarComputed = false;
        if (results_.size() <= 0) {
            return false;
        }
        if (results_.size()==1) {
            results_.remove(actualMeasurementNum_);
            actualMeasurement_ = null;
            actualMeasurementNum_ = -1;
            return false;
        }

        if ( actualMeasurementNum_ >= results_.size()-1 ) {
            results_.remove(actualMeasurementNum_);
            actualMeasurementNum_ --;
        }
        else {
            results_.remove(actualMeasurementNum_);
        }

        actualMeasurement_ = results_.get(actualMeasurementNum_);
        return true;
    }

    /**
     * Számol, kirajzolja az aktuális mérést
     * @param tempCanvas alkalmazás háttere
     * @param heightPx képernyő paraméterei
     * @param widthPx
     */
    public void plotMeasurement(Canvas tempCanvas, int heightPx, int widthPx) {
        actualMeasurement_.plotGraph(tempCanvas, heightPx, widthPx);
    }

    /**
     * Számol is
     * @param probability szóráshoz
     * @return összes mérés átlaga
     */
    public float getAverage(float probability) {
        if ( !isAvgVarComputed || probability != probability_ )
            computeAvgVar();
        return average_;
    }

    /**
     * Student (t) eloszlás szerint
     * @param probability szóráshoz
     * @return összes mérés konfidenciaintervallumának félszélessége
     */
    public float getVariance(float probability) {
        if ( !isAvgVarComputed || probability != probability_ ) {
            probability_ = probability;
            computeAvgVar();
        }
        return variance_;
    }

    /**
     * Számolást is hívja
     * @return aktuális mérés átlaga
     */
    public float getActualAvg() {
        if (actualMeasurementNum_>=0)
            return actualMeasurement_.getAvg();
        return 0f;
    }

    /**
     * Számolást is hívja
     * @return aktuális mérés hibája
     */
    public float getActualVar() {
        if (actualMeasurementNum_ >= 0)
            return actualMeasurement_.getVar();
        return 0f;
    }

    /** Mérés indítása
     * Utolsóra ugrik, létrehozza a következőt
     */
    public void startMeasurement() {
        actualMeasurementNum_ = results_.size()-1;
        addNewMeasurement();
        isRunning = true;
    }

    /**
     * Megállítja a mérést
     */
    public void stopMeasurement() {
        isRunning = false;
    }

    /** Fut-e jelenleg mérés? **/
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * .csv fájba exportálja az aktuális mérést
     */
    public void exportData() {
        if (isRunning)
            return;
        if (actualMeasurementNum_ < 0)
            return;
        actualMeasurement_.exportData();
    }

    private void computeAvgVar() {
        if (isRunning)
            return;
        float sum = 0f;
        for ( TDataSet i : results_ ) {
            sum += i.getAvg();
        }
        average_ = sum / size();

        float variance = 0f;
        for ( TDataSet i : results_ ) {
            variance += (float)Math.pow( (double)i.getAvg() - average_, 2);
        }
        variance_ = (float)Math.sqrt( variance / size() / (size()-1) ) * tDistribution(size()-1, probability_);

        isAvgVarComputed = true;
    }


    /**
     * Student (t) eloszlás táblázat alapján
     * @param degOfFreedom szabadsági fok
     * @param probability valószínűség 0.55 < p < 1
     * @return egyoldali t-eloszlás
     */
    private float tDistribution(int degOfFreedom, float probability) {
        /** első oszlop a valószínűség **/
        double t[][] = {
                {0.55, 0.158, 0.142, 0.137, 0.134, 0.132, 0.131, 0.130, 0.130, 0.129, 0.129, 0.129, 0.128, 0.128, 0.128, 0.128, 0.128, 0.128, 0.127, 0.127, 0.127, 0.127, 0.127, 0.127, 0.127, 0.127, 0.127, 0.127, 0.127, 0.127, 0.127 },
                {0.6, 0.325, 0.289, 0.277, 0.271, 0.267, 0.265, 0.263, 0.262, 0.261, 0.260, 0.260, 0.259, 0.259, 0.258, 0.258, 0.258, 0.257, 0.257, 0.257, 0.257, 0.257, 0.256, 0.256, 0.256, 0.256, 0.256, 0.256, 0.256, 0.256, 0.256 },
                {0.7, 0.727, 0.617, 0.584, 0.569, 0.559, 0.553, 0.549, 0.546, 0.543, 0.542, 0.540, 0.539, 0.538, 0.537, 0.536, 0.535, 0.534, 0.534, 0.533, 0.533, 0.532, 0.532, 0.532, 0.531, 0.531, 0.531, 0.531, 0.530, 0.530, 0.530 },
                {0.75, 1.000, 0.816, 0.765, 0.741, 0.727, 0.718, 0.711, 0.706, 0.703, 0.700, 0.697, 0.695, 0.694, 0.692, 0.691, 0.690, 0.689, 0.688, 0.688, 0.687, 0.686, 0.686, 0.685, 0.685, 0.684, 0.684, 0.684, 0.683, 0.683, 0.683 },
                {0.8, 1.376, 1.061, 0.978, 0.941, 0.920, 0.906, 0.896, 0.889, 0.883, 0.879, 0.876, 0.873, 0.870, 0.868, 0.866, 0.865, 0.863, 0.862, 0.861, 0.860, 0.859, 0.858, 0.858, 0.857, 0.856, 0.856, 0.855, 0.855, 0.854, 0.854 },
                {0.9, 3.078, 1.886, 1.638, 1.533, 1.476, 1.440, 1.415, 1.397, 1.383, 1.372, 1.363, 1.356, 1.350, 1.345, 1.341, 1.337, 1.333, 1.330, 1.328, 1.325, 1.323, 1.321, 1.319, 1.318, 1.316, 1.315, 1.314, 1.313, 1.311, 1.310 },
                {0.95, 6.314, 2.920, 2.353, 2.132, 2.015, 1.943, 1.895, 1.860, 1.833, 1.812, 1.796, 1.782, 1.771, 1.761, 1.753, 1.746, 1.740, 1.734, 1.729, 1.725, 1.721, 1.717, 1.714, 1.711, 1.708, 1.706, 1.703, 1.701, 1.699, 1.697 },
                {0.975, 12.706, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228, 2.201, 2.179, 2.160, 2.145, 2.131, 2.120, 2.110, 2.101, 2.093, 2.086, 2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052, 2.048, 2.045, 2.042 },
                {0.99, 31.821, 6.965, 4.541, 3.747, 3.365, 3.143, 2.998, 2.896, 2.821, 2.764, 2.718, 2.681, 2.650, 2.624, 2.602, 2.583, 2.567, 2.552, 2.539, 2.528, 2.518, 2.508, 2.500, 2.492, 2.485, 2.479, 2.473, 2.467, 2.462, 2.457 },
                {0.995, 63.656, 9.925, 5.841, 4.604, 4.032, 3.707, 3.499, 3.355, 3.250, 3.169, 3.106, 3.055, 3.012, 2.977, 2.947, 2.921, 2.898, 2.878, 2.861, 2.845, 2.831, 2.819, 2.807, 2.797, 2.787, 2.779, 2.771, 2.763, 2.756, 2.750},
        };
        for (double[] aT : t) {
            if (aT[0] >= probability)
                return (float) aT[min(degOfFreedom, 30)];
        }
        return 0f;
    }
    private static int min(int a, int b) {
        return (a <= b) ? a : b;
    }


}
