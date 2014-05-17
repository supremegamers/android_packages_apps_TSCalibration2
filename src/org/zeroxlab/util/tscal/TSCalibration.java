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
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.util.Log;

public class TSCalibration extends Activity {

    final private static String TAG = "TSCalibration";
    final private static String POINTERCAL = "/data/misc/tscal/pointercal";
    final private static String defaultPointercalValues = "1 0 0 0 0 1 1 1 1 0 0 0 0 1 1 1\n";
    final protected static File FILE = new File(POINTERCAL);

    private TSCalibrationView mTSCalibrationView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTSCalibrationView = new TSCalibrationView(this);
        setContentView(R.layout.intro);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTSCalibrationView.reset();
        reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (mTSCalibrationView.isFinished()) {
            mTSCalibrationView.dumpCalData(FILE);
            setResult(0);
            finish();
        } else {
            setContentView(mTSCalibrationView);
        }
        return true;
    }

    private void reset() {
        try {
            FileOutputStream fos = new FileOutputStream(FILE);
            fos.flush();
            fos.getFD().sync();
            fos.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    public void onCalTouchEvent(MotionEvent ev) {
        mTSCalibrationView.invalidate();
        if (mTSCalibrationView.isFinished()) {
            setContentView(R.layout.done);
        }
    }

    static protected void setImmersiveMode(View v) {
        v.setSystemUiVisibility(v.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | v.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | v.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | v.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | v.SYSTEM_UI_FLAG_FULLSCREEN      // hide status bar
            | v.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
