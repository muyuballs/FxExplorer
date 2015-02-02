/*
 * Copyright 2015. Qiao
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

package info.breezes.fxapi.countly;

import info.breezes.fxapi.ThemeAbleActivity;
import ly.count.android.api.Countly;

public class CountlyActivity extends ThemeAbleActivity {

    @Override
    protected void onStop() {
        if(CountlyUtils.COUNTLY_ENABLE) {
            Countly.sharedInstance().onStop();
        }
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(CountlyUtils.COUNTLY_ENABLE) {
            Countly.sharedInstance().onStart();
        }
    }

}
