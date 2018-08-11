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
import android.database.Cursor;
import android.provider.BaseColumns;

/**
 * Quote model, stores the data retrieved from a quote entry in the SQLite database.
 */
public class Quote {
    /** Database column name for minute of the day */
    private static final String COLUMN_MINUTE = "minute";
    /** Database column name for the quote text */
    private static final String COLUMN_TEXT = "text";
    /** Database column name for the quote's author */
    private static final String COLUMN_AUTHOR = "author";
    /** Database column name for the quote's book origin */
    private static final String COLUMN_BOOK = "book";

    /** Quote's primary key id */
    public final long id;
    /** Quote's minute of the day value */
    public final int minute;
    /** Quote's text to display */
    public final String text;
    /** Quote's author information */
    public final String author;
    /** Quote's book origin */
    public final String book;

    /**
     * Creates a new {@code Quote} from the given database {@link Cursor}.
     *
     * @param cursor Cursor object retrieved from the SQLite database
     */
    public Quote(Cursor cursor) {
        id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
        minute = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MINUTE));
        text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT));
        author = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR));
        book = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOOK));
    }

    /**
     * Get the quote's origin string containing of the book and its author as HTML formatted string.
     *
     * @param context Calling context
     * @return Quote origin string
     */
    public String getOrigin(Context context) {
        return context.getString(R.string.quote_origin, book, author);
    }

    @Override
    public String toString() {
        return "Quote{" +
                "id=" + id +
                ", minute=" + minute +
                ", text='" + text + '\'' +
                '}';
    }
}
