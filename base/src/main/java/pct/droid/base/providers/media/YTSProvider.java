/*
 * This file is part of Popcorn Time.
 *
 * Popcorn Time is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Popcorn Time is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Popcorn Time. If not, see <http://www.gnu.org/licenses/>.
 */

package pct.droid.base.providers.media;

import android.accounts.NetworkErrorException;
import android.os.Build;

import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import pct.droid.base.Constants;
import pct.droid.base.PopcornApplication;
import pct.droid.base.R;
import pct.droid.base.providers.media.models.Genre;
import pct.droid.base.providers.media.models.Media;
import pct.droid.base.providers.media.models.Movie;
import pct.droid.base.providers.meta.MetaProvider;
import pct.droid.base.providers.meta.TraktProvider;
import pct.droid.base.providers.subs.SubsProvider;
import pct.droid.base.providers.subs.YSubsProvider;
import pct.droid.base.utils.LocaleUtils;

public class YTSProvider extends MediaProvider {

    private static final YTSProvider sMediaProvider = new YTSProvider();
    private static final String API_URL = "http://cloudflare.com/api/v2/";
    private static final String MIRROR_URL = "http://reddit.com/api/v2/";
    private static final String MIRROR_URL2 = "http://xor.image.yt/api/v2/";
    public static String CURRENT_URL = API_URL;

    private static final SubsProvider sSubsProvider = new YSubsProvider();

    @Override
    protected OkHttpClient getClient() {
        OkHttpClient client = super.getClient().clone();
        // Use only HTTP 1.1 for YTS
        List<Protocol> proto = new ArrayList<>();
        proto.add(Protocol.HTTP_1_1);
        client.setProtocols(proto);
        return client;
    }

    @Override
    protected Call enqueue(Request request, com.squareup.okhttp.Callback requestCallback) {
        request = request.newBuilder().removeHeader("User-Agent").addHeader("User-Agent", String.format("Mozilla/5.0 (Linux; U; Android %s; %s; %s Build/%s) AppleWebkit/534.30 (KHTML, like Gecko) PT/%s", Build.VERSION.RELEASE, LocaleUtils.getCurrent(), Build.MODEL, Build.DISPLAY, Constants.UA_VERSION)).build();
        return super.enqueue(request, requestCallback);
    }

    @Override
    public Call getList(final ArrayList<Media> existingList, Filters filters, final Callback callback) {
        final ArrayList<Media> currentList;
        if (existingList == null) {
            currentList = new ArrayList<>();
        } else {
            currentList = (ArrayList<Media>) existingList.clone();
        }

        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new NameValuePair("limit", "30"));

        if (filters == null) {
            filters = new Filters();
        }

        if (filters.keywords != null) {
            params.add(new NameValuePair("query_term", filters.keywords));
        }

        if (filters.genre != null) {
            params.add(new NameValuePair("genre", filters.genre));
        }

        if (filters.order == Filters.Order.ASC) {
            params.add(new NameValuePair("order_by", "asc"));
        } else {
            params.add(new NameValuePair("order_by", "desc"));
        }

        String sort;
        switch (filters.sort) {
            default:
            case POPULARITY:
                sort = "seeds";
                break;
            case YEAR:
                sort = "year";
                break;
            case DATE:
                sort = "date_added";
                break;
            case RATING:
                sort = "rating";
                break;
            case ALPHABET:
                sort = "title";
                break;
        }

        params.add(new NameValuePair("sort_by", sort));

        if (filters.page != null) {
            params.add(new NameValuePair("page", Integer.toString(filters.page)));
        }

        Request.Builder requestBuilder = new Request.Builder();
        String query = buildQuery(params);
        requestBuilder.url(CURRENT_URL + "list_movies.json?" + query);
        requestBuilder.tag(MEDIA_CALL);

        return fetchList(currentList, requestBuilder, filters, callback);
    }

    /**
     * Fetch the list of movies from YTS
     *
     * @param currentList    Current shown list to be extended
     * @param requestBuilder Request to be executed
     * @param callback       Network callback
     * @return Call
     */
    private Call fetchList(final ArrayList<Media> currentList, final Request.Builder requestBuilder, final Filters filters, final Callback callback) {
        requestBuilder.addHeader("Host", "xor.image.yt");
        return enqueue(requestBuilder.build(), new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                if (generateFallback(requestBuilder)) {
                    fetchList(currentList, requestBuilder, filters, callback);
                } else {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseStr;
                    try {
                        responseStr = response.body().string();
                    } catch (SocketException e) {
                        onFailure(response.request(), new IOException("Socket failed"));
                        return;
                    }

                    YTSReponse result;
                    try {
                        result = mGson.fromJson(responseStr, YTSReponse.class);
                    } catch (IllegalStateException e) {
                        onFailure(response.request(), new IOException("JSON Failed"));
                        return;
                    } catch (JsonSyntaxException e) {
                        onFailure(response.request(), new IOException("JSON Failed"));
                        return;
                    }

                    if (result.status != null && result.status.equals("error")) {
                        callback.onFailure(new NetworkErrorException(result.status_message));
                    } else if(result.data != null && ((result.data.get("movies") != null && ((ArrayList<LinkedTreeMap<String, Object>>)result.data.get("movies")).size() <= 0) || ((Double)result.data.get("movie_count")).intValue() <= currentList.size())) {
                        callback.onFailure(new NetworkErrorException("No movies found"));
                    } else {
                        ArrayList<Media> formattedData = result.formatForPopcorn(currentList);
                        callback.onSuccess(filters, formattedData, true);
                        return;
                    }
                }
                onFailure(response.request(), new IOException("Couldn't connect to YTS"));
            }
        });
    }

    @Override
    public Call getDetail(String videoId, Callback callback) {
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(API_URL + "movie_details.json?movie_id=" + videoId);
        requestBuilder.addHeader("Host", "xor.image.yt");
        requestBuilder.tag(MEDIA_CALL);

        return fetchDetail(requestBuilder, callback);
    }

    private Call fetchDetail(final Request.Builder requestBuilder, final Callback callback) {
        return enqueue(requestBuilder.build(), new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                if (generateFallback(requestBuilder)) {
                    fetchDetail(requestBuilder, callback);
                } else {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseStr;
                    try {
                        responseStr = response.body().string();
                    } catch (SocketException e) {
                        onFailure(response.request(), new IOException("Socket failed"));
                        return;
                    }

                    YTSReponse result;
                    try {
                        result = mGson.fromJson(responseStr, YTSReponse.class);
                    } catch (IllegalStateException e) {
                        onFailure(response.request(), new IOException("JSON Failed"));
                        return;
                    } catch (JsonSyntaxException e) {
                        onFailure(response.request(), new IOException("JSON Failed"));
                        return;
                    }

                    if (result.status != null && result.status.equals("error")) {
                        callback.onFailure(new NetworkErrorException(result.status_message));
                    } else {
                        final Movie movie = result.formatDetailForPopcorn();

                        TraktProvider traktProvider = new TraktProvider();

                        traktProvider.getMovieMeta(movie.imdbId, new MetaProvider.Callback() {
                            @Override
                            public void onResult(MetaProvider.MetaData metaData, Exception e) {
                                if (e == null) {
                                    if (metaData.images != null) {
                                        if (metaData.images.poster != null) {
                                            movie.image = metaData.images.poster.replace("/original/", "/medium/");
                                            movie.fullImage = metaData.images.poster;
                                        }

                                        if (metaData.images != null && metaData.images.backdrop != null) {
                                            movie.headerImage = metaData.images.backdrop.replace("/original/", "/medium/");
                                        }
                                    } else {
                                        movie.fullImage = movie.image;
                                        movie.headerImage = movie.image;
                                    }


                                    if (metaData.title != null) {
                                        movie.title = metaData.title;
                                    }

                                    if (metaData.overview != null) {
                                        movie.synopsis = metaData.overview;
                                    }

                                    if (metaData.tagline != null) {
                                        movie.tagline = metaData.tagline;
                                    }

                                    if (metaData.trailer != null) {
                                        movie.trailer = metaData.trailer;
                                    }

                                    if (metaData.runtime != null) {
                                        movie.runtime = Integer.toString(metaData.runtime);
                                    }

                                    if (metaData.certification != null) {
                                        movie.certification = metaData.certification;
                                    }
                                }

                                final ArrayList<Media> returnData = new ArrayList<>();
                                returnData.add(movie);
                                callback.onSuccess(null, returnData, true);
                            }
                        });

                        return;
                    }
                }
                onFailure(response.request(), new IOException("Couldn't connect to YTS"));
            }
        });
    }

    private boolean generateFallback(Request.Builder requestBuilder) {
        String url = requestBuilder.build().urlString();
        if (url.contains(MIRROR_URL2)) {
            return false;
        } else if (url.contains(MIRROR_URL)) {
            url = url.replace(MIRROR_URL, MIRROR_URL2);
            CURRENT_URL = MIRROR_URL2;
        } else {
            url = url.replace(API_URL, MIRROR_URL);
            CURRENT_URL = MIRROR_URL;
        }
        requestBuilder.url(url);
        return true;
    }

    private class YTSReponse {
        public String status;
        public String status_message;
        public LinkedTreeMap<String, Object> data;

        /**
         * Test if there is an item that already exists
         *
         * @param results List with items
         * @param id      Id of item to check for
         * @return Return the index of the item in the results
         */
        private int isInResults(ArrayList<Media> results, String id) {
            int i = 0;
            for (Media item : results) {
                if (item.videoId.equals(id)) return i;
                i++;
            }
            return -1;
        }

        /**
         * Format data for the application
         *
         * @return List with items
         */
        public Movie formatDetailForPopcorn() {
            Movie movie = new Movie(sMediaProvider, sSubsProvider);
            LinkedTreeMap<String, Object> movieObj = data;
            if (movieObj == null) return movie;

            Double id = (Double) movieObj.get("id");
            movie.videoId = Integer.toString(id.intValue());
            movie.imdbId = (String) movieObj.get("imdb_code");

            movie.title = (String) movieObj.get("title");
            Double year = (Double) movieObj.get("year");
            movie.year = Integer.toString(year.intValue());
            movie.rating = movieObj.get("rating").toString();
            movie.genre = ((ArrayList<String>) movieObj.get("genres")).get(0);

            ArrayList<LinkedTreeMap<String, Object>> torrents =
                    (ArrayList<LinkedTreeMap<String, Object>>) movieObj.get("torrents");
            if (torrents != null) {
                for (LinkedTreeMap<String, Object> torrentObj : torrents) {
                    String quality = (String) torrentObj.get("quality");
                    if (quality == null) continue;

                    Media.Torrent torrent = new Media.Torrent();

                    torrent.seeds = ((Double) torrentObj.get("seeds")).intValue();
                    torrent.peers = ((Double) torrentObj.get("peers")).intValue();
                    torrent.hash = (String) torrentObj.get("hash");
                    try {
                        String magnet = "magnet:?xt=urn:btih:" + torrent.hash + "&amp;dn=" + URLEncoder.encode(movieObj.get("title_long").toString(), "utf-8") + "&amp;tr=http://exodus.desync.com:6969/announce&amp;tr=udp://tracker.openbittorrent.com:80/announce&amp;tr=udp://open.demonii.com:1337/announce&amp;tr=udp://exodus.desync.com:6969/announce&amp;tr=udp://tracker.yify-torrents.com/announce";
                        torrent.url = magnet;
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        torrent.url = (String) torrentObj.get("url");
                    }

                    movie.torrents.put(quality, torrent);
                }
            }


            return movie;
        }

        /**
         * Format data for the application
         *
         * @param existingList List to be extended
         * @return List with items
         */
        public ArrayList<Media> formatForPopcorn(ArrayList<Media> existingList) {
            ArrayList<LinkedTreeMap<String, Object>> movies = new ArrayList<>();
            if (data != null) {
                movies = (ArrayList<LinkedTreeMap<String, Object>>) data.get("movies");
            }

            for (LinkedTreeMap<String, Object> item : movies) {
                Movie movie = new Movie(sMediaProvider, sSubsProvider);

                Double id = (Double) item.get("id");
                movie.videoId = Integer.toString(id.intValue());
                movie.imdbId = (String) item.get("imdb_code");

                int existingItem = isInResults(existingList, movie.videoId);
                if (existingItem == -1) {
                    movie.title = (String) item.get("title");
                    Double year = (Double) item.get("year");
                    movie.year = Integer.toString(year.intValue());
                    movie.rating = item.get("rating").toString();
                    movie.genre = ((ArrayList<String>) item.get("genres")).get(0);
                    movie.image = (String) item.get("medium_cover_image");
                    movie.backgroundImage = (String) item.get("background_image");

                    if (movie.image != null) {
                        movie.image = movie.image.replace("medium-cover", "large-cover");
                    }

                    ArrayList<LinkedTreeMap<String, Object>> torrents =
                            (ArrayList<LinkedTreeMap<String, Object>>) item.get("torrents");
                    if (torrents != null) {
                        for (LinkedTreeMap<String, Object> torrentObj : torrents) {
                            String quality = (String) torrentObj.get("quality");
                            if (quality == null || quality.equals("3D")) continue;

                            Media.Torrent torrent = new Media.Torrent();

                            torrent.seeds = ((Double) torrentObj.get("seeds")).intValue();
                            torrent.peers = ((Double) torrentObj.get("peers")).intValue();
                            torrent.hash = (String) torrentObj.get("hash");
                            try {
                                torrent.url = "magnet:?xt=urn:btih:" + torrent.hash + "&amp;dn=" + URLEncoder.encode(item.get("title_long").toString(), "utf-8") + "&amp;tr=http://exodus.desync.com:6969/announce&amp;tr=udp://tracker.openbittorrent.com:80/announce&amp;tr=udp://open.demonii.com:1337/announce&amp;tr=udp://exodus.desync.com:6969/announce&amp;tr=udp://tracker.yify-torrents.com/announce";
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                                torrent.url = (String) torrentObj.get("url");
                            }

                            movie.torrents.put(quality, torrent);
                        }
                    }

                    existingList.add(movie);
                }
            }
            return existingList;
        }
    }

    @Override
    public int getLoadingMessage() {
        return R.string.loading_movies;
    }

    @Override
    public List<NavInfo> getNavigation() {
        List<NavInfo> tabs = new ArrayList<>();
        tabs.add(new NavInfo(Filters.Sort.POPULARITY, Filters.Order.DESC, PopcornApplication.getAppContext().getString(R.string.popular_now)));
        tabs.add(new NavInfo(Filters.Sort.RATING, Filters.Order.DESC, PopcornApplication.getAppContext().getString(R.string.top_rated)));
        tabs.add(new NavInfo(Filters.Sort.DATE, Filters.Order.DESC, PopcornApplication.getAppContext().getString(R.string.release_date)));
        tabs.add(new NavInfo(Filters.Sort.YEAR, Filters.Order.DESC, PopcornApplication.getAppContext().getString(R.string.year)));
        tabs.add(new NavInfo(Filters.Sort.ALPHABET, Filters.Order.ASC, PopcornApplication.getAppContext().getString(R.string.a_to_z)));
        return tabs;
    }

    @Override
    public List<Genre> getGenres() {
        List<Genre> returnList = new ArrayList<>();
        returnList.add(new Genre(null, R.string.genre_all));
        returnList.add(new Genre("action", R.string.genre_action));
        returnList.add(new Genre("adventure", R.string.genre_adventure));
        returnList.add(new Genre("animation", R.string.genre_animation));
        returnList.add(new Genre("biography", R.string.genre_biography));
        returnList.add(new Genre("comedy", R.string.genre_comedy));
        returnList.add(new Genre("crime", R.string.genre_crime));
        returnList.add(new Genre("documentary", R.string.genre_documentary));
        returnList.add(new Genre("drama", R.string.genre_drama));
        returnList.add(new Genre("family", R.string.genre_family));
        returnList.add(new Genre("fantasy", R.string.genre_fantasy));
        returnList.add(new Genre("filmnoir", R.string.genre_film_noir));
        returnList.add(new Genre("history", R.string.genre_history));
        returnList.add(new Genre("horror", R.string.genre_horror));
        returnList.add(new Genre("music", R.string.genre_music));
        returnList.add(new Genre("musical", R.string.genre_musical));
        returnList.add(new Genre("mystery", R.string.genre_mystery));
        returnList.add(new Genre("romance", R.string.genre_romance));
        returnList.add(new Genre("scifi", R.string.genre_sci_fi));
        returnList.add(new Genre("short", R.string.genre_short));
        returnList.add(new Genre("sport", R.string.genre_sport));
        returnList.add(new Genre("thriller", R.string.genre_thriller));
        returnList.add(new Genre("war", R.string.genre_war));
        returnList.add(new Genre("western", R.string.genre_western));
        return returnList;
    }

}
