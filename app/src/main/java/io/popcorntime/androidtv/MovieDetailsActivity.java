/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.popcorntime.androidtv;

import android.os.Bundle;

/*
 * Details activity class that loads LeanbackDetailsFragment class
 */
public class MovieDetailsActivity extends MediaDetailsActivity {
    public static final String SHARED_ELEMENT_NAME = "hero";
    public static final String MOVIE = "Movie";
    public static final String STREAM_INFO = "Stream_info";
    public static final String TYPE = "Type";
    public enum Type {
        MOVIE, TRAILER
    }
    public static final String TYPE_MOVIE = "type_movie";
    public static final String TYPE_TRAILER = "type_trailer";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(io.popcorntime.androidtv.R.layout.activity_details);
    }

}
