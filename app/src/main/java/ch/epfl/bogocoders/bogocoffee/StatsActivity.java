package ch.epfl.bogocoders.bogocoffee;

import android.app.ListActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class StatsActivity extends ListActivity {

    private static final String LOG_TAG = "StatsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stats);

        // Create a list data which will be displayed in inner ListView.
        List<String> listData = new ArrayList<String>();
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        for (String item : MainActivity.COFFEE_TYPE) {
            Log.e(LOG_TAG, "item : " + item);
            listData.add(item + " : " + sharedPref.getInt(item, 0));
        }


        ArrayAdapter<String> listDataAdapter = new ArrayAdapter<String>(this, R.layout.activity_stats_row, R.id.listRowTextView, listData);

        // Set this adapter to inner ListView object.
        this.setListAdapter(listDataAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Do something when a list item is clicked
    }
}