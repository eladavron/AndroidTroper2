package ambious.androidtroper;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Elad on 02/10/13.
 */
public class MyListActivity extends AppCompatActivity {
    //INSTANCE CONSTANTS//
    private final String LOG_TAG = "AndroidTroper: List";
    private final int TYPE_RECENT = 0;
    private final int TYPE_FAVORITES = 1;
    private final int TYPE_READLATER = 2;
    private final int TYPE_OFFLINE = 5;
    private final int SORT_NAME = 0;
    private final int SORT_OLDEST = 1;
    private final int SORT_NEWEST = 2;

    private Intent _intent;
    private int _intentFlag = -1;
    private String _sortDefaultString;
    private SharedPreferences _mainPreferences;
    private SharedPreferences _recentSettings;
    private SharedPreferences _favoriteSettings;
    private SharedPreferences _readLaterSettings;
    private SharedPreferences _offlineSettings;
    private ListView _listView;
    private List<MyListItem> _listItems;
    private File _offlineDir;
    ItemsAdapter _adapter;
    ActionBar _actionBar;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _intent = getIntent();
        _intentFlag = _intent.getFlags();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        switch (_intentFlag)
        {
            case TYPE_FAVORITES:
                _sortDefaultString = "favSortDefault";
                setTitle(getString(R.string.favorites));
                break;
            case TYPE_RECENT:
                _sortDefaultString = "recentSortDefault";
                setTitle(getString(R.string.recent));
                break;
            case TYPE_READLATER:
                _sortDefaultString = "rlSortDefault";
                setTitle(getString(R.string.readlater));
                break;
            case TYPE_OFFLINE:
                _sortDefaultString = "favSortDefault";
                setTitle(getString(R.string.offlineSettings));
                break;
            default:
                Log.wtf(LOG_TAG,"Tried to set a sort default string without a sort type");
                setResult(RESULT_CANCELED);
                finish();
                return;
        }

        _recentSettings = getSharedPreferences("recentSettings",0);
        _favoriteSettings = getSharedPreferences("favoriteSettings",0);
        _mainPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        _readLaterSettings = getSharedPreferences("readlaterSettings",0);
        _offlineSettings = getSharedPreferences("offlineSettings",0);
        _offlineDir = ContextCompat.getExternalFilesDirs(this,"offline")[0];

        setTheme(_mainPreferences.getBoolean("nightMode", false) ? R.style.AppDark : R.style.AppLight);
        setContentView(R.layout.listview);

        //Setup Actiobar//
        Toolbar myToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(myToolbar);
        _actionBar = getSupportActionBar();
        _actionBar.setDisplayHomeAsUpEnabled(true);

        //Set the list
        _listItems = getList(_intentFlag);
        if (_listItems == null)
        {
            String _message;
            switch(_intentFlag){
                case TYPE_RECENT:
                    _message = getString(R.string.recentEmpty);
                    break;
                case TYPE_FAVORITES:
                    _message = getString(R.string.favoritesEmpty);
                    break;
                case TYPE_READLATER:
                    _message = getString(R.string.readLaterEmpty);
                    break;
                case TYPE_OFFLINE:
                    _message = getString(R.string.offlineFilesEmpty);
                    break;
                default:
                    Log.w(LOG_TAG,"Empty list with no recognized flag! Flag: " + _intentFlag);
                    _message = getString(R.string.listEmpty);
                    break;
            }
            Toast.makeText(this,_message,Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        _listView = (ListView) findViewById(R.id.myListView);
        if (_listView == null)
        {
            Log.e(LOG_TAG,"ListView returned null!");
            return;
        }
        _adapter = new ItemsAdapter();
        sortList(Integer.valueOf(_mainPreferences.getString(_sortDefaultString,"2")));
        _listView.setAdapter(_adapter);
        _listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
                MyListItem selectedResult = _listItems.get(position);
                Intent result = new Intent();
                result.putExtra("name",selectedResult.getName());
                result.putExtra("url",selectedResult.getUrl());
                setResult(RESULT_OK,result);
                if (_intentFlag == TYPE_READLATER)
                {
                    final int index = selectedResult.getIndex();
                    Animation anim = AnimationUtils.loadAnimation(getBaseContext(), android.R.anim.slide_out_right);
                    anim.setDuration(500);
                    anim.setFillAfter(true);
                    view.startAnimation(anim);
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            removeFromReadLater(index);
                            finish();
                        }
                    }, anim.getDuration());
                }
                else
                {
                    finish();
                }
            }
        });
    }
    private void sortList (int sortType)
    {
        try{
            SharedPreferences.Editor editor = _mainPreferences.edit();
            switch (sortType)
            {
                case 0: //Sort by Name
                    _adapter.sort(new Comparator<MyListItem>() {
                        @Override
                        public int compare(MyListItem lhs, MyListItem rhs) {
                            return lhs.getName().compareTo(rhs.getName());
                        }
                    });
                    break;
                case 1: //Oldest First
                    _adapter.sort(new Comparator<MyListItem>() {
                        @Override
                        public int compare(MyListItem lhs, MyListItem rhs) {
                            return lhs.getTime().compareTo(rhs.getTime());
                        }
                    });
                    break;
                case 2:
                    _adapter.sort(new Comparator<MyListItem>() {
                        @Override
                        public int compare(MyListItem lhs, MyListItem rhs) {
                            return rhs.getTime().compareTo(lhs.getTime());
                        }
                    });
                    break;
                default:
                    Log.wtf(LOG_TAG, "Tried sorting with an unknown sorting type: " + sortType);
                    return;
            }
            editor.putString(_sortDefaultString,""+sortType);
            editor.apply();
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG,"Sorting failed!");
            Log.e(LOG_TAG,e.getMessage());
        }
    }

    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.recent_options, menu);
        String sortValue = _mainPreferences.getString(_sortDefaultString,"2");
        menu.getItem(1).getSubMenu().getItem(Integer.valueOf(sortValue)).setChecked(true);
        return true;
    }

    /**
     * Selects action to take when clicking a button on the option bar
     * @param item The button that was pressed
     * @return true if handled, false if dropped
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        boolean _reverse = item.isChecked(); //Selected the item already checked
        switch (item.getItemId()){
            case (android.R.id.home):
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case (R.id.sort_name):
                item.setChecked(true);
                sortList(SORT_NAME);
                return true;
            case (R.id.sort_oldest):
                item.setChecked(true);
                sortList(SORT_OLDEST);
                return true;
            case (R.id.sort_newest):
                item.setChecked(true);
                sortList(SORT_NEWEST);
                return true;
            case (R.id.action_clear_all):
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
                switch (_intentFlag)
                {
                    case TYPE_FAVORITES:
                        alertDialog.setTitle(R.string.confirmTitle);
                        alertDialog.setMessage(R.string.confirmClearFavorites);
                        alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences.Editor editor = _favoriteSettings.edit();
                                editor.clear();
                                editor.putInt("favCount",0);
                                editor.apply();
                                recreate();
                            }
                        });
                        alertDialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        });
                        alertDialog.show();
                        break;
                    case TYPE_READLATER:
                        alertDialog.setTitle(R.string.confirmTitle);
                        alertDialog.setMessage(R.string.confirmClearReadLater);
                        alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences.Editor editor = _readLaterSettings.edit();
                                editor.clear();
                                editor.putInt("rlCount",0);
                                editor.apply();
                                recreate();
                            }
                        });
                        alertDialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        });
                        alertDialog.show();
                        break;
                    case TYPE_RECENT:
                        alertDialog.setTitle(R.string.confirmTitle);
                        alertDialog.setMessage(R.string.confirmClearRecent);
                        alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences.Editor editor = _recentSettings.edit();
                                editor.clear();
                                editor.putInt("recentCount",0);
                                editor.apply();
                                recreate();
                            }
                        });
                        alertDialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                        alertDialog.show();
                        break;
                    case TYPE_OFFLINE:
                        alertDialog.setTitle(R.string.confirmTitle);
                        alertDialog.setMessage(R.string.confirmClearOffline);
                        alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                File offline[] = _offlineDir.listFiles();
                                for (File offlineFile : offline)
                                    offlineFile.delete();
                                _offlineSettings = getSharedPreferences("offlineSettings",0);
                                _offlineSettings.edit().clear().apply();
                                recreate();
                            }
                        });
                        alertDialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        });
                        alertDialog.show();
                        break;
                }
                return true;
        }
        return false;
    }

    private ArrayList<MyListItem> getList(int listType)
    {
        int _count;
        switch (listType) {
            case TYPE_RECENT:
                _count = _recentSettings.getInt("recentCount", 0);
                if (_count == 0)
                    return null;
                ArrayList<MyListItem> _recentList = new ArrayList<MyListItem>();
                for (int i = 1; i <= _count; i++) {
                    String _name = _recentSettings.getString("recentTitle" + i, null);
                    String _url = _recentSettings.getString("recentUrl" + i, null);
                    String _time = _recentSettings.getString("recentTime" + i, null);
                    if (_name != null && _url != null && _time != null) {
                        MyListItem _recentItem = new MyListItem(i, _name, _url, _time);
                        _recentList.add(_recentItem);
                    } else {
                        Log.e(LOG_TAG, "One of the recent items' parameters was null");
                    }
                }
                return _recentList;
            case TYPE_FAVORITES:
                _count = _favoriteSettings.getInt("favCount", 0);
                if (_count == 0)
                    return null;
                ArrayList<MyListItem> _favoriteList = new ArrayList<MyListItem>();
                for (int i = 1; i <= _count; i++) {
                    String _name = _favoriteSettings.getString("favTitle" + i, null);
                    String _url = _favoriteSettings.getString("favUrl" + i, null);
                    String _time = _favoriteSettings.getString("favTime" + i, null);
                    if (_name != null && _url != null && _time != null) {
                        MyListItem _favItem = new MyListItem(i, _name, _url, _time);
                        _favoriteList.add(_favItem);
                    } else {
                        Log.e(LOG_TAG, "One of the favorites' parameters was null");
                    }
                }
                return _favoriteList;
            case TYPE_READLATER:
                _count = _readLaterSettings.getInt("rlCount", 0);
                if (_count == 0)
                    return null;
                ArrayList<MyListItem> _readLaterList = new ArrayList<MyListItem>();
                for (int i = 1; i <= _count; i++) {
                    String _name = _readLaterSettings.getString("rlTitle" + i, null);
                    String _url = _readLaterSettings.getString("rlUrl" + i, null);
                    String _time = _readLaterSettings.getString("rlTime" + i, null);
                    if (_name != null && _url != null && _time != null) {
                        MyListItem _rlItem = new MyListItem(i, _name, _url, _time);
                        _readLaterList.add(_rlItem);
                    } else {
                        Log.e(LOG_TAG, "One of the read later item's parameters was null");
                    }
                }
                return _readLaterList;
            case TYPE_OFFLINE:
                File[] offlineList = _offlineDir.listFiles();
                _count = offlineList.length;
                int i=0;
                if (_count == 0)
                    return null;
                ArrayList<MyListItem> _offlineList = new ArrayList<MyListItem>();
                for (File offlineFile : offlineList) {
                    String _name = _offlineSettings.getString(offlineFile.getName() + "_title", null);
                    String _url = _offlineSettings.getString(offlineFile.getName() + "_url", null);
                    String _time = _offlineSettings.getString(offlineFile.getName() + "_time", null);
                    if (_name != null && _url != null && _time != null) {
                        MyListItem _favItem = new MyListItem(i++, _name, _url, _time);
                        _offlineList.add(_favItem);
                    } else {
                        Log.e(LOG_TAG, "One of the offline files' parameters was null");
                    }
                }
                return _offlineList;
            default:
                Log.e(LOG_TAG, "List type unrecognized: " + listType);
                return null;
        }
    }

    private void removeFromReadLater(int itemToRemove){
        try{
            int rlCount = _readLaterSettings.getInt("rlCount", 0);
            SharedPreferences.Editor editor = _readLaterSettings.edit();

            //Cycle items, moving the next one instead of this one.
            for (int i=itemToRemove;i<rlCount;i++){
                editor.putString("rlTitle" + i, _readLaterSettings.getString("rlTitle" + (i+1), null));
                editor.putString("rlUrl" + i, _readLaterSettings.getString("rlUrl" + (i+1), null));
                editor.putString("rlTime" + i, _readLaterSettings.getString("rlTime" + (i+1), null));
            }
            editor.remove("rlUrl" + rlCount);
            editor.remove("rlTitle" + rlCount);
            editor.remove("rlTime" + rlCount);
            rlCount--;
            editor.putInt("rlCount", rlCount);
            editor.apply();
        } catch (Exception e)
        {
            Log.e(LOG_TAG,"An error occured removing item " + itemToRemove + " from read-later!");
            Log.e(LOG_TAG,e.getMessage() + "");
        }
    }

    private void removeFromFavorites(int itemToRemove){
        try{
            int favCount = _favoriteSettings.getInt("favCount", 0);
            SharedPreferences.Editor editor = _favoriteSettings.edit();

            //Cycle items, moving the next one instead of this one.
            for (int i=itemToRemove;i<favCount;i++){
                editor.putString("favUrl" + i, _favoriteSettings.getString("favUrl" + (i+1), null));
                editor.putString("favTitle" + i, _favoriteSettings.getString("favTitle" + (i+1), null));
                editor.putString("favTime" + i, _favoriteSettings.getString("favTime" + (i+1), null));
            }
            editor.remove("favUrl" + favCount);
            editor.remove("favTitle" + favCount);
            editor.remove("favTime" + favCount);
            favCount--;
            editor.putInt("favCount", favCount);
            editor.apply();
        } catch (Exception e) {
            Log.e(LOG_TAG,"An error occured removing item " + itemToRemove + " from favorites!");
            Log.e(LOG_TAG,e.getMessage() + "");
        }
    }

    private void removeFromOffline(int itemToRemove){
        try{
            SharedPreferences.Editor editor = _offlineSettings.edit();
            int hash = getList(TYPE_OFFLINE).get(itemToRemove).getUrl().hashCode();
            File offlineFile = new File(_offlineDir,String.valueOf(hash));
            offlineFile.delete();
            editor.remove(hash + "_url");
            editor.remove(hash + "_name");
            editor.remove(hash + "_time");
            editor.apply();
        } catch (Exception e)
        {
            Log.e(LOG_TAG,"An error occured removing item " + itemToRemove + " from offline files!");
            Log.e(LOG_TAG,e.getMessage() + "");
            return;
        }
    }

    private class MyListItem
    {
        private String _name;
        private String _url;
        private String  _time;
        private int _index;
        public MyListItem(int index, String name, String url, String time)
        {
            _name = name;
            _url = url;
            _time = time;
            _index = index;
        }
        public String getName()
        {
            return _name;
        }
        public String getUrl()
        {
            return _url;
        }
        public String getTime()
        {
            return _time;
        }
        public int getIndex() { return _index; }
    }


    private class ItemsAdapter extends ArrayAdapter<MyListItem> {
        ItemsAdapter() {
            super(MyListActivity.this,android.R.layout.simple_list_item_1, _listItems);
        }

        public View getView(int position, View convertView, ViewGroup parent){
            View row = convertView;
            ItemWrapper wrapper;
            if (row == null){
                LayoutInflater inflater=getLayoutInflater();
                row = inflater.inflate(R.layout.list_item, null);
                wrapper = new ItemWrapper(row);
                row.setTag(wrapper);
            } else {
                wrapper = (ItemWrapper) row.getTag();
            }
            wrapper.populateFrom(_listItems.get(position));
            ImageButton removeButton = (ImageButton) row.findViewById(R.id.listRemove);
            if (_intentFlag == TYPE_FAVORITES || _intentFlag == TYPE_READLATER || _intentFlag == TYPE_OFFLINE)
            {
                final MyListItem selectedResult = _listItems.get(position);
                removeButton.setVisibility(View.VISIBLE);
                removeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final int index = selectedResult.getIndex();
                        final int position = _listView.getPositionForView(v) - _listView.getFirstVisiblePosition();
                        Animation anim = AnimationUtils.loadAnimation(getContext(), android.R.anim.slide_out_right);
                        anim.setDuration(500);
                        anim.setFillAfter(true);
                        _listView.getChildAt(position).startAnimation(anim);
                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                switch (_intentFlag) {
                                    case TYPE_FAVORITES:
                                        removeFromFavorites(index);
                                        break;
                                    case TYPE_READLATER:
                                        removeFromReadLater(index);
                                        break;
                                    case TYPE_OFFLINE:
                                        removeFromOffline(index);
                                        break;
                                }
                                recreate();
                            }
                        }, anim.getDuration());
                    }
                });
            }
            else
                removeButton.setVisibility(View.GONE);
            return(row);
        }
    }

    /**
     * Actually puts the data in the appropriate fields
     */

    private class ItemWrapper {
        private TextView _name;
        private TextView _url;
        private View row;

        public ItemWrapper(View row){
            this.row = row;
        }

        public void populateFrom(MyListItem r){
            getName().setText(r.getName());
            getUrl().setText(r.getUrl());
        }

        TextView getName(){
            if (_name == null)
                _name=(TextView)row.findViewById(R.id.title);
            return _name;
        }

        TextView getUrl(){
            if (_url == null)
                _url=(TextView)row.findViewById(R.id.url);
            return _url;
        }
    }
}

