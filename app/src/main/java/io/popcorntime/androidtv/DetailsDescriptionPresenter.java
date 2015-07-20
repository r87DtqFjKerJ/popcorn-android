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

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;

import pct.droid.base.providers.media.models.Media;
import pct.droid.base.providers.media.models.Movie;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        Media media = (Media) item;

        if (media != null) {
            if (media.isMovie == true) {
                Movie movie = (Movie) media;
                viewHolder.getTitle().setText(movie.title);
                viewHolder.getSubtitle().setText(movie.genre + ", " + movie.year + ", " + movie.rating);
                viewHolder.getBody().setText(movie.synopsis);
            }
        }
        else {
            viewHolder.getTitle().setText(media.title);
            viewHolder.getSubtitle().setText(media.year);
            viewHolder.getBody().setText(media.imdbId);
        }
    }
}
