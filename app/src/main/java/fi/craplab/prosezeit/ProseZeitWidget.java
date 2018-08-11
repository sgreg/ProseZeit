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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Calendar;

/**
 * ProseZeit Widget itself.
 * Does all the widgety things.
 */
public class ProseZeitWidget extends AppWidgetProvider implements DatabaseLoaderTask.Listener {
    private static final String TAG = ProseZeitWidget.class.getSimpleName();

    private static final String ALARM_ACTION = "alaaaAaAAaarm";
    private static final String CLICK_ACTION = "clickediclick";
    private SQLiteDatabase database;
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private Quote lastShownQuote;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        Log.d(TAG, "onUpdated, context " + context  +" db is " + database);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.prose_zeit_widget);

        Intent intent = new Intent(context, ProseZeitWidget.class);
        intent.setAction(CLICK_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0, intent, 0);
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

        if (database == null) {
            new DatabaseLoaderTask(this).execute(context);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled");
        DatabaseHelper.createDatabaseFromAsset(context);
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "onDisabled");
        DatabaseHelper.getInstance(context).close();
        if (alarmManager != null && alarmIntent != null) {
            alarmManager.cancel(alarmIntent);
        }
        super.onDisabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ALARM_ACTION:
                    setQuote(context);
                    break;
                case CLICK_ACTION:
                    setQuoteClicked(context);
                    break;
            }
        }
    }

    /**
     * Displays the given {@link Quote}. If the given {@code lastUpdate} is not the current minute
     * of the day, the quote is displayed a bit lighter to indicate that the displayed time is not
     * the actual current one. If the {@code lastUpdate} is the current time, the new quote is
     * displayed and the quote's origin is hidden.
     *
     * @param context Calling context
     * @param quote The quote data
     * @param lastUpdate Minute of the day the last quote was displayed, If this is older than the
     *                   current minute of the day, it means there was no quote for the current
     *                   time, and the previous quote is to be re-used
     */
    private static void setQuoteView(Context context, Quote quote, int lastUpdate) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.prose_zeit_widget);

        views.setTextViewText(R.id.quote_text, Html.fromHtml(quote.text));
        views.setTextViewText(R.id.quote_origin, Html.fromHtml(quote.getOrigin(context)));

        if (lastUpdate < getMinuteOfDay()) {
            views.setTextColor(R.id.quote_text, ContextCompat.getColor(context, R.color.oldQuote));
        } else {
            views.setViewVisibility(R.id.quote_origin, View.INVISIBLE);
            views.setTextColor(R.id.quote_text, ContextCompat.getColor(context, R.color.freshQuote));
        }

        updateViews(context, views);
    }

    /**
     * Update the view when the widget was clicked. Clicking the widget will reveal the quote's
     * origin by displaying the book name and its author.
     *
     * @param context Calling context
     */
    private static void setQuoteClicked(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.prose_zeit_widget);
        views.setViewVisibility(R.id.quote_origin, View.VISIBLE);

        updateViews(context, views);
    }

    /**
     * Update all the views with whatever new content and behavior was added to it.
     *
     * @param context Calling context
     * @param views The views to update
     */
    public static void updateViews(Context context, RemoteViews views) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context.getPackageName(), ProseZeitWidget.class.getName());
        int[] widgetIds = appWidgetManager.getAppWidgetIds(componentName);

        for (int appWidgetId : widgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    /**
     * Get and display the first quote after the widget was opened / the database was loaded.
     * If now quote was found for the current time (happens, there are a few gaps in the data set),
     * the last time available is looked up instead.
     *
     * @param context Calling context
     */
    private void setFirstQuote(Context context) {
        Quote firstQuote;
        int minuteOfDay = getMinuteOfDay();
        int retries = 20;

        do {
            firstQuote = getQuote(context, minuteOfDay--);
        } while (firstQuote == null && retries-- > 0);

        if (firstQuote != null) {
            lastShownQuote = firstQuote;
            setQuoteView(context, firstQuote, firstQuote.minute);
        } else {
            Log.e(TAG, "Couldn't find a quote to show");
        }
    }

    /**
     * Get and display the current time's quote.
     * If on quote was found, the last found quote is used instead. If no last found quote exists,
     * the {@link DatabaseLoaderTask} is executed to make sure we have access to the data in the
     * first place (Widget life cycle and all..). Once loaded, the {@link DatabaseLoaderTask}'s
     * callbacks are taking care to display a quote by calling {@link #setFirstQuote(Context)}.
     *
     * @param context Calling context
     */
    private void setQuote(Context context) {
        int minuteOfDay = getMinuteOfDay();
        Quote quote = getQuote(context, minuteOfDay);

        if (quote != null) {
            lastShownQuote = quote;
            setQuoteView(context, quote, minuteOfDay);
        } else {
            Log.d(TAG, "No quote for minute " + minuteOfDay);
            if (lastShownQuote != null) {
                setQuoteView(context, lastShownQuote, lastShownQuote.minute);
            } else {
                new DatabaseLoaderTask(this).execute(context);
            }
        }
    }

    /**
     * Get the current minute of the day.
     *
     * @return Current time's minute of the day
     */
    private static int getMinuteOfDay() {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60
                + Calendar.getInstance().get(Calendar.MINUTE);
    }

    /**
     * Get a quote representing the given minute of the day from the database, if one exists.
     * If multiple quotes exists for the same time, a random one is returned. If none exists,
     * {@code null} is returned. If the {@link #database} object is {@code null}, the
     * {@link DatabaseLoaderTask} is executed and {@code null} is returned here.
     *
     * @param context Calling context
     * @param minuteOfDay Minute of the day to look a quote for
     * @return {@link Quote} object filled with the retrieved database entry if a quote matching
     *         the given minute of the day was found, {@code null} otherwise.
     */
    private Quote getQuote(Context context, int minuteOfDay) {
        if (database == null) {
            new DatabaseLoaderTask(this).execute(context);
            return null;
        }

        Cursor cursor = database.rawQuery(
                "SELECT * FROM quotes WHERE minute = ? ORDER BY RANDOM() LIMIT 1",
                new String[]{String.valueOf(minuteOfDay)});
        Quote quote = null;

        if (cursor.moveToNext()) {
            quote = new Quote(cursor);
            Log.d(TAG, "got quote " + quote);
        }
        cursor.close();
        return quote;
    }


    @Override
    public void setDatabase(SQLiteDatabase database) {
        this.database = database;
    }

    @Override
    public void onDatabaseLoaded(Context context) {
        setFirstQuote(context);
        setupTimer(context);
    }

    /**
     * Set up the {@link AlarmManager} timer to send a {@link #ALARM_ACTION} broadcast every minute.
     *
     * @param context Calling context
     */
    private void setupTimer(Context context) {
        if (alarmIntent != null) {
            // already got an alarm, never mind
            return;
        }

        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ProseZeitWidget.class);
        intent.setAction(ALARM_ACTION);
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        // set up calendar to the next minute
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis() + 60000);
        calendar.set(Calendar.SECOND, 0);
        Log.d(TAG, "setting alarm to " + calendar);

        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), 60000, alarmIntent);
    }
}
