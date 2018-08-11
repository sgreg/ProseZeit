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
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper class to handle the Database creation, updating and copying the original asset data.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = DatabaseHelper.class.getSimpleName();

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "quotes.db";

    private static DatabaseHelper instance;

    /**
     * Create the {@code DatabaseHandler} instance with the given {@link Context}.
     *
     * @param context Calling context
     */
    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * Retrieve the {@code DatabaseHelper} singleton instance. If no instance exists yet, it will
     * be created first.
     *
     * @param context Calling context
     * @return {@code DatabaseHelper} instance
     */
    public static DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            Log.d(TAG, "db helper create new instance");
            instance = new DatabaseHelper(context);
        }

        Log.d(TAG, "db helper get instance: " + instance);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // do nothing
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        // do nothing yet
    }

    /**
     * Creates the application's internal database file from the pre-populated assets file shipped
     * with the app itself. If the internal database already exists, nothing happens.
     *
     * @param context Calling context
     */
    public static void createDatabaseFromAsset(Context context) {
        String dbPath = context.getApplicationInfo().dataDir + "/databases/";
        File dbFile = new File(dbPath + DB_NAME);

        Log.d(TAG, "create db, file " + dbFile.getAbsolutePath() + " exists: " + dbFile.exists());
        if (!dbFile.exists()) {
            // open database to create the file, and close it before copying the file
            getInstance(context).getReadableDatabase();
            getInstance(context).close();

            // copy original assets db file to app's database folder
            try {
                InputStream input = context.getAssets().open(DB_NAME);
                OutputStream output = new FileOutputStream(dbFile);

                byte[] mBuffer = new byte[1024];
                int length;

                while ((length = input.read(mBuffer)) > 0) {
                    output.write(mBuffer, 0, length);
                }

                output.flush();
                output.close();
                input.close();

                Log.i(TAG, "Database created");
            } catch (IOException e) {
                Log.e(TAG, "Creating database failed", e);
            }
        }
    }
}
