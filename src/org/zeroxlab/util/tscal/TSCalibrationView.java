/* Copyright (C) 2010 0xlab.org
 * Authored by: Kan-Ru Chen <kanru@0xlab.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zeroxlab.util.tscal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import android.util.Log;

public class TSCalibrationView extends View {

    final private static String TAG = "TSCalibration";

    private class TargetPoint {
        public int x;
        public int y;
        public int calx;
        public int caly;
        public TargetPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private int mStep = 0;
    private TargetPoint mTargetPoints[];
    private TSCalibration mContext;

    public TSCalibrationView(TSCalibration context) {
        super(context);

        mContext = context;
        context.setImmersiveMode(this);
    }

    public void reset() {
        mStep = 0;
    }

    public boolean isFinished() {
        return mStep >= 5;
    }

    public void dumpCalData(File file) {
        String cal = performCalibration();
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(cal.getBytes());
            fos.flush();
            fos.getFD().sync();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Cannot open file " + file);
        } catch (IOException e) {
            Log.e(TAG, "Cannot write file " + file);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (isFinished())
            return true;
        if (ev.getAction() != MotionEvent.ACTION_UP)
            return true;
        mTargetPoints[mStep].calx = (int)ev.getRawX();
        mTargetPoints[mStep].caly = (int)ev.getRawY();
        mStep++;
        mContext.onCalTouchEvent(ev);
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mTargetPoints == null) {
            int w = getWidth(), h = getHeight();
            mTargetPoints = new TargetPoint[5];
            mTargetPoints[0] = new TargetPoint(50, 50);
            mTargetPoints[1] = new TargetPoint(w - 50, 50);
            mTargetPoints[2] = new TargetPoint(w - 50, h - 50);
            mTargetPoints[3] = new TargetPoint(50, h - 50);
            mTargetPoints[4] = new TargetPoint(w / 2, h / 2);
        }
        if (isFinished())
            return;
        canvas.drawColor(Color.BLACK);
        drawTarget(canvas, mTargetPoints[mStep].x, mTargetPoints[mStep].y);
    }

    private void drawTarget(Canvas c, int x, int y) {
        Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint red = new Paint(Paint.ANTI_ALIAS_FLAG);
        white.setColor(Color.WHITE);
        red.setColor(Color.RED);
        c.drawCircle(x, y, 25, red);
        c.drawCircle(x, y, 21, white);
        c.drawCircle(x, y, 17, red);
        c.drawCircle(x, y, 13, white);
        c.drawCircle(x, y, 9, red);
        c.drawCircle(x, y, 5, white);
        c.drawCircle(x, y, 1, red);
    }

    private String performCalibration() {
        int cal[] = new int[7];
        double n, x, y, x2, y2, xy, z, zx, zy;
        double det, a, b, c, e, f, i;
        double scaling = 65536.0;

        // Get sums for matrix
        n = x = y = x2 = y2 = xy = 0;
        for (TargetPoint point : mTargetPoints) {
            n += 1;
            x += (double)point.calx;
            y += (double)point.caly;
            x2 += (double)(point.calx * point.calx);
            y2 += (double)(point.caly * point.caly);
            xy += (double)(point.calx * point.caly);
            Log.v(TAG, n + "x=" + point.x + " y=" + point.y + " calx=" + point.calx + " caly=" + point.caly);
        }

        // Get determinant of matrix -- check if determinant is too small
        det = n*(x2*y2 - xy*xy) + x*(xy*y - x*y2) + y*(x*xy - y*x2);
        if (det > -0.1 && det < 0.1) {
            Log.w(TAG, "determinant is too small -- " + det);
        }

        // Get elements of inverse matrix
        a = (x2*y2 - xy*xy);
        b = (xy*y - x*y2);
        c = (x*xy - y*x2);
        e = (n*y2 - y*y);
        f = (x*y - n*xy);
        i = (n*x2 - x*x);

        // Get sums for x calibration
        z = zx = zy = 0;
        for (TargetPoint point : mTargetPoints) {
            z += (double)point.x;
            zx += (double)(point.x * point.calx);
            zy += (double)(point.x * point.caly);
        }

        // Now multiply out to get the calibration for framebuffer x coord
        cal[2] = (int)((a*z + b*zx + c*zy) * scaling / det);
        cal[0] = (int)((b*z + e*zx + f*zy) * scaling / det);
        cal[1] = (int)((c*z + f*zx + i*zy) * scaling / det);

        // Get sums for y calibration
        z = zx = zy = 0;
        for (TargetPoint point : mTargetPoints) {
            z += (double)point.y;
            zx += (double)(point.y * point.calx);
            zy += (double)(point.y * point.caly);
        }

        // Now multiply out to get the calibration for framebuffer y coord
        cal[5] = (int)((a*z + b*zx + c*zy) * scaling / det);
        cal[3] = (int)((b*z + e*zx + f*zy) * scaling / det);
        cal[4] = (int)((c*z + f*zx + i*zy) * scaling / det);

        // If we got here, we're OK, so assign scaling to a[6] and return
        cal[6] = (int)scaling;

        StringBuilder sb = new StringBuilder();
        for (int s : cal) {
            sb.append(s);
            sb.append(" ");
        }
        sb.append(getWidth());
        sb.append(" ");
        sb.append(getHeight());
        sb.append("\n");
        String ret = sb.toString();
        Log.i(TAG, "pointercal = " + ret);
        return ret;
    }
}
