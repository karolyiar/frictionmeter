package karolyi.frictionmeter.util;

/**
 * Created by a on 2014.02.16..
 */
public class XYZ {
    float x_, y_, z_, xy_, xypz_;

    public XYZ(float x, float y, float z) {
        x_ = x;
        y_ = y;
        z_ = z;
        if (z<=0.1f && z>=-0.1f) /// nullával osztás elkerülésére
            z = 0.1f;
        xy_ = (float)Math.sqrt(x*x + y*y);
        xypz_ = (float) xy_ /z;
    }

    public float getX() { return x_; }
    public float getY() { return y_; }
    public float getZ() { return z_; }
    public float getXY() { return xy_; }
    public float getXYpZ() { return xypz_; }
}
