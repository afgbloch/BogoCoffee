package ch.epfl.bogocoders.bogocoffee;

import android.app.ListActivity;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.content.CursorLoader;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class StatsActivity extends ListActivity {

    // This is the Adapter being used to display the list's data
    SimpleCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stats);

        // Create a list data which will be displayed in inner ListView.
        List<String> listData = new ArrayList<String>();
        listData.add("Audi");
        listData.add("Benz");
        listData.add("BMW");
        listData.add("Ford");
        listData.add("Honda");
        listData.add("Toyoto");
        

        ArrayAdapter<String> listDataAdapter = new ArrayAdapter<String>(this, R.layout.activity_stats_row, R.id.listRowTextView, listData);

        // Set this adapter to inner ListView object.
        this.setListAdapter(listDataAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Do something when a list item is clicked
    }
}