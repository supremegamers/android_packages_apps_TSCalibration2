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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;

// Added to allow for automatic startup
public class StartupIntentReceiver extends BroadcastReceiver {

    private static String cal_path = "/data/misc/tscal/pointercal";

    @Override public void onReceive(Context context, Intent intent) {
        File calFile = new File(cal_path);
        if (!calFile.exists()) {
            Intent starterIntent = new Intent(context, TSCalibration.class);
            starterIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(starterIntent);
        }
    }
}