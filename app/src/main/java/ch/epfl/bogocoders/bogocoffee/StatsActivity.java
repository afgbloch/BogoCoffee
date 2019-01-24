package ch.epfl.bogocoders.bogocoffee;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class StatsActivity extends ListActivity {

    public static String COFFEE_TYPE[] = {
            "EspressoForte",
            "EspressoDecaffeinato",
            "LungoForte",
            "Ristretto",
            "LungoDecaffeinato",
            "RistrettoIntenso",
            "Unknown"
    };
    private static final String LOG_TAG = "StatsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stats);

        List<String> listData = new ArrayList<>();
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        for (String item : COFFEE_TYPE) {
            Log.e(LOG_TAG, "item : " + item);
            listData.add(item + " : " + sharedPref.getInt(item, 0));
        }

        ArrayAdapter<String> listDataAdapter = new ArrayAdapter<>(this, R.layout.activity_stats_row, R.id.listRowTextView, listData);

        this.setListAdapter(listDataAdapter);
    }
}