package me.lifegrep.heart.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Toast;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import me.lifegrep.heart.R;
import me.lifegrep.heart.model.DailyActivity;
import me.lifegrep.heart.services.ScratchWriter;
import me.lifegrep.heart.adapters.CustomExpandableListAdapter;
import me.lifegrep.heart.adapters.DailyActivitiesList;

public class ActivitySelectionActivity extends AppCompatActivity {

    HashMap<String, List<String>> listDetail;
    List<String> listTitle;
    ExpandableListAdapter listAdapter;
    ScratchWriter writer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        Toast.makeText(this, "activity selection on create", Toast.LENGTH_SHORT).show();
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        writer = new ScratchWriter(this, "activities.csv");

        ExpandableListView listView = (ExpandableListView) findViewById(R.id.expandableListView);
        listDetail = DailyActivitiesList.getData();
        listTitle = new ArrayList<String>(listDetail.keySet());
        listAdapter = new CustomExpandableListAdapter(this, listTitle, listDetail);
        listView.setAdapter(listAdapter);

        listView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {

            @Override
            public void onGroupExpand(int groupPosition) {
            }
        });

        listView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {

            @Override
            public void onGroupCollapse(int groupPosition) {
            }
        });

        listView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                String data = listDetail.get(listTitle.get(groupPosition)).get(childPosition);
                //TODO add user, type and posture
                DailyActivity ev = new DailyActivity(0, "event", data);
                writer.saveData(ev.toCSV());
                Toast.makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
                return false;
            }
        });


        FloatingActionButton fabinput = (FloatingActionButton) findViewById(R.id.fabinput);
        fabinput.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = writer.getData();
                if (!data.isEmpty()) {
                    Toast.makeText(getApplicationContext(), data, Toast.LENGTH_LONG).show();
                } else {
                    String text = "No records";
                    Snackbar.make(view, text, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                }
            }
        });


        FloatingActionButton fabdelete = (FloatingActionButton) findViewById(R.id.fabdelete);
        fabdelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = "Erasing content";
                Snackbar.make(view, text, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                writer.eraseContents();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
