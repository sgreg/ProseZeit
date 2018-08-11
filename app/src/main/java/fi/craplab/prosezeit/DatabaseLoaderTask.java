/*
 * ProseZeit - Literary Clock Widget for Android
 *
 * Copyright (C) 2018 Sven Gregori <sven@craplab.fi>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.craplab.prosezeit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

/**
 * {@link AsyncTask} to load the database.
 */
public class DatabaseLoaderTask extends AsyncTask<Context, Void, Context> {
    private static final String TAG = DatabaseHelper.class.getSimpleName();

    /**
     * Listener interface to handle this {@link AsyncTask}'s results from an external listener.
     */
    public interface Listener {
        /**
         * Called when the database is opened to pass the {@link SQLiteDatabase} object back to the
         * calling party.
         *
         * @param database Read-only database object
         */
        void setDatabase(SQLiteDatabase database);

        /**
         * Called when the {@link AsyncTask} is done and the database is fully loaded. Passes the
         * {@link Context} given to {@link AsyncTask#execute(Object[])} back to the calling party.
         * Widgets and Context, you know...
         *
         * @param context Context initially given in the {@code DatabaseLoaderTask}'s constructor
         */
        void onDatabaseLoaded(Context context);
    }

    /** Callback listener */
    private final Listener listener;

    /**
     * Creates a new {@code DatabaseLoaderTask} and attaches the given {@link Listener} to it.
     *
     * @param listener Callback listener
     */
    public DatabaseLoaderTask(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected Context doInBackground(Context... contexts) {
        Log.d(TAG, "donInBg context " + contexts[0]);
        listener.setDatabase(DatabaseHelper.getInstance(contexts[0]).getReadableDatabase());
        return contexts[0];
    }

    @Override
    protected void onPostExecute(Context context) {
        Log.d(TAG, "onPostExec context " + context);
        listener.onDatabaseLoaded(context);
    }
}
