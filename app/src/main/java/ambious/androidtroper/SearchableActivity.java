package ambious.androidtroper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.text.Html;
import android.text.format.Time;
import android.util.Log;
import android.view.*;
import android.widget.*;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Elad on 25/09/13.
 */

public class SearchableActivity extends Activity {

    private final String LOG_TAG = "AndroidTroper: Search";
    private Intent _intent;
    private List<ResultItem> _resultItems = new ArrayList<ResultItem>();
    private SharedPreferences _mainPreferences;
    private SharedPreferences _readLaterSettings;
    private SharedPreferences _favoriteSettings;
    private boolean _nightMode;
    private Activity _parent = getParent();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _intent = getIntent();
        String _query = _intent.getStringExtra("query");
        getActionBar().setHomeButtonEnabled(true); //Enables Sets the home button
        getActionBar().setDisplayHomeAsUpEnabled(true); //Makes the home button go back
        _mainPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        _favoriteSettings = getSharedPreferences("favoriteSettings", 0);
        _readLaterSettings = getSharedPreferences("readlaterSettings", 0);
        _nightMode = _mainPreferences.getBoolean("nightMode",false);
        if (_nightMode)
            setTheme(R.style.SecondaryTheme_Dark);
        else
            setTheme(R.style.SecondaryTheme);
        doMySearch(_query);
    }

    /**
     * Selects action to take when clicking a button on the option bar
     * @param item The button that was pressed
     * @return true if handled, false if dropped
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()){
            case (android.R.id.home):
                setResult(RESULT_CANCELED);
                finish();
                return true;
        }
        return false;
    }

    /**
     * Sets up the search windows action bar
     * @param menu The option menu created
     * @return true if handled, false if dropped
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_options, menu);
        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView _searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        _searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        _searchView.setSubmitButtonEnabled(true);
        //Set the search window to be opened with the queried search already.
        _searchView.setIconified(false);
        _searchView.setQuery(_intent.getStringExtra("query"),false);
        _searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                doMySearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.search_results) {
            final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            final ResultItem _resultItem = _resultItems.get(info.position);
            menu.setHeaderTitle(_resultItem.getTitle());
            menu.add(0,0,0,getString(R.string.open)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Intent result = new Intent();
                    result.putExtra("name",_resultItem.getTitle());
                    result.putExtra("url",_resultItem.getUrl());
                    setResult(RESULT_OK, result);
                    finish();
                    return true;
                }
            });
            menu.add(0,1,1,getString(R.string.add_to_rl)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    addToReadLater(_resultItem.getTitle(), _resultItem.getUrl());
                    return true;
                }
            });
            menu.add(0,2,2,getString(R.string.add_to_fav)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    addToFavorites(_resultItem.getTitle(),_resultItem.getUrl());
                    return true;
                }
            });
        }
    }

    private void addToFavorites(String _title, String _url){
        try{
            SharedPreferences.Editor editor = _favoriteSettings.edit();
            int _favCount = _favoriteSettings.getInt("favCount", 0); //0 is always empty
            Time now = new Time();
            now.setToNow();
            String _time = now.toString();
            //First, check that it doesn't already exist
            for (int i=1;i<=_favCount;i++){
                String favUrl = _favoriteSettings.getString("favUrl" + i, null);
                if (_url.equals(favUrl))
                {
                    Log.i(LOG_TAG,"Article already in favorites, updating time string");
                    editor.putString("favTime" + i, _time);
                    editor.commit();
                    Toast.makeText(this,"\"" + _title + "\" " + getString(R.string.alreadyInFavorites),Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            //If we reached here - it doesn't already exist
            _favCount++;
            editor.putString("favTitle" + _favCount, _title);
            editor.putString("favUrl" + _favCount, _url);
            editor.putString("favTime" + _favCount, _time);
            editor.putInt("favCount", _favCount);
            editor.commit();
            Toast.makeText(this,"\"" + _title + "\" " + getString(R.string.addedToFav),Toast.LENGTH_SHORT).show();
        }	catch (Exception e){
            Log.e(LOG_TAG, "Saving to favorites failed: " + e.getMessage());
        }
    }

    private void addToReadLater (String _title, String _url){
        try{
            SharedPreferences.Editor editor = _readLaterSettings.edit();
            int _rlCount = _readLaterSettings.getInt("rlCount", 0); //0 is always empty
            Time now = new Time();
            now.setToNow();
            String _time = now.toString();
            //First, check that it doesn't already exist
            for (int i=1;i<=_rlCount;i++){
                String rlUrl = _readLaterSettings.getString("rlUrl" + i, null);
                if (_url.equals(rlUrl))
                {
                    Log.i(LOG_TAG,"Article already in list, updating time string");
                    editor.putString("rlTime" + i, _time);
                    editor.commit();
                    Toast.makeText(this,"\"" + _title + "\" " + getString(R.string.alreadyInReadLater),Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            //If we reached here - it doesn't already exist
            _rlCount++;
            editor.putString("rlTitle" + _rlCount, _title);
            editor.putString("rlUrl" + _rlCount, _url);
            editor.putString("rlTime" + _rlCount, _time);
            editor.putInt("rlCount", _rlCount);
            editor.commit();
            Toast.makeText(this,"\"" + _title + "\" " + getString(R.string.addedToReadLater),Toast.LENGTH_SHORT).show();
        }	catch (Exception e){
            Log.e(LOG_TAG, "Saving to read-later failed: " + e.getMessage());
        }
    }

    private void doMySearch(String query) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,MySuggestionProvider.AUTHORITY, MySuggestionProvider.MODE);
        suggestions.saveRecentQuery(query, null); //Adds the search to recent searches suggestions
        _resultItems.clear(); //In case it has items left over from previous search
        SearchAction sa = new SearchAction(query);
        sa.execute();
    }

    private void parseResults(SearchResults result) {
        try{
            setContentView(R.layout.search);
            ListView _resultList = (ListView) findViewById(R.id.search_results);
            JSONObject obj = result.getData();
            JSONArray res = obj.getJSONArray("items");
            int i = 0;
            for (i=0;i<res.length();i++)
            {
                JSONObject resultItem = res.getJSONObject(i);
                String title = resultItem.getString("title").split("- TV Tropes")[0].trim();
                String description = resultItem.getString("snippet");
                String url = resultItem.getString("link");
                if (!description.contains(" to start this new page. This page has not been indexed.") && url.contains("tvtropes.org/pmwiki/pmwiki.php/")) {
                    _resultItems.add(new ResultItem(title, description, url));
                }
            }
            if (_resultItems.size() == 0) //No Result
            {
                _resultList.setVisibility(View.GONE);
                TextView noResults = (TextView) findViewById(R.id.noResults);
                noResults.setVisibility(View.VISIBLE);
                if (_nightMode)
                    noResults.setBackgroundColor(getResources().getColor(R.color.at_dark));
                return;
            }
            ResultItemAdapter adapter = new ResultItemAdapter();
            _resultList.setAdapter(adapter);
            _resultList.setTextFilterEnabled(true);
            _resultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
                    ResultItem selectedResult = _resultItems.get(position);
                    Intent result = new Intent();
                    result.putExtra("name",selectedResult.getTitle());
                    result.putExtra("url",selectedResult.getUrl());
                    setResult(RESULT_OK, result);
                    finish();
                }
            });
        } catch (JSONException e)
        {
            Log.e(LOG_TAG,"Failed parsing JSON!");
        }
    }

    private void errorDialog(String title, String message, final String query, boolean showRetry)
    {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        if (showRetry)
        {
            alertDialog.setPositiveButton(R.string.Retry, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Log.w(LOG_TAG,"Retrying...");
                    dialog.dismiss();
                    SearchAction sa = new SearchAction(query);
                    sa.execute();
                }
            });
        }
        alertDialog.setNegativeButton(R.string.Abort, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.w(LOG_TAG, "Aborted after error!");
                dialog.dismiss();
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }
    public class SearchAction extends AsyncTask<String, Void, SearchResults> {

        // Searche Variables
        private ProgressDialog _dialog;
        private String _query;

        private final String ENGINE_ID = "005788057032214784679:a1q2xrz--z0";
        private final String API_KEY = "AIzaSyBnt2CZDjAXqEDIqvVYwjcIf3IOWW6te3o";
        private String GOOGLE = "https://www.googleapis.com/customsearch/v1?";

        // Interface Objects
        private String _searching;

        // Constructor

        public SearchAction(String query){
            _query = query;
            //Initiate and assign strings
            _searching = getString(R.string.Searching);
            //Logger("New SearchAction created:\nQuery: " + _query + "\nSearchURL: " + _searchString + "\n");
        }

        @Override
        protected void onPreExecute(){
            _dialog = new ProgressDialog(SearchableActivity.this);
            _dialog.setMessage(_searching + " \"" + _query + "\"...");
            _dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            _dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    Log.i(LOG_TAG, "Search Cancelled by user!");
                    _dialog.dismiss();
                    Toast.makeText(SearchableActivity.this, R.string.searchCancelled, Toast.LENGTH_SHORT).show();
                    cancel(true);
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });
            _dialog.setCancelable(true);
            _dialog.show();
        }

        protected SearchResults doInBackground(String...params){
            Document doc;
            try {
                //Check query is valid
                if (_query == null || _query.isEmpty())
                    throw new Exception("Search query is null or empty.");

                //Sanitize request
                _query = _query.replace(" ", "%20"); //Whitespace
                //Build search string
                String query = ("key=" + API_KEY + "&cx=" + ENGINE_ID + "&q=" + _query + "&alt-json");
                URL url = new URL("https://www.googleapis.com/customsearch/v1?" + query);
                // read from the URL

                HttpURLConnection conn2 = (HttpURLConnection) url.openConnection();
                System.out.println("Connection opened!");
                conn2.setRequestMethod("GET");
                conn2.setRequestProperty("Accept", "application/json");
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        (conn2.getInputStream())));

                String line = "";
                String response = "";
                while((line = reader.readLine()) != null){
                    response += line + "\n";
                }

                // build a JSON object
                JSONObject obj = new JSONObject(response);
                return new SearchResults(true,obj);
            } catch (UnknownHostException e) {
                Log.e(LOG_TAG,"Connection Error!");
                if (e.getMessage() != null)
                    Log.e(LOG_TAG,e.getMessage());
                errorDialog("Connection Error!", "Connection Error!\n" + e.getMessage(), _query, true);
                return null;
            } catch (Exception e) {
                Log.e(LOG_TAG,"Unknown search error occurred!");
                if (e.getMessage() != null)
                    Log.e(LOG_TAG,e.getMessage());
                errorDialog("Unknown search error occurred!", "Unknown search error occurred!\n" + e.getMessage(),_query, true);
                return null;
            }
        }

        protected void onPostExecute(SearchResults results){
          try{
                if (_dialog.isShowing())
                    _dialog.dismiss();
                if (results == null)
                {
                    Log.e(LOG_TAG, "Results are null!");
                    return;
                }
                parseResults(results);
            }
            catch (IllegalArgumentException ie)
            {
                Log.e(LOG_TAG,"Couldn't dismiss progress window.");
                parseResults(results);
            }
        }
    }

    /**
     * Represents a complete result of a search operation
     */

    private class SearchResults{
        //Result Variables
        private JSONObject _data;
        public SearchResults(Boolean success, JSONObject data){
            _data = data;
        }

        public JSONObject getData(){
            return _data;
        }
    }

    /**
     * Represents a single item in the search results
     */

    private class ResultItem{

        //Item Variables

        private String _title;
        private String _description;
        private String _url;

        public ResultItem(String title, String description, String url){
            _title = title;
            _description = description;
            _url = url;
        }

        public String getTitle(){
            return _title;
        }

        public String getDescription(){
            return _description;
        }

        public String getUrl(){
            return _url;
        }
    }

    /**
     * Inflates the search results screen with data
     */
    private class ResultItemAdapter extends ArrayAdapter<ResultItem> {
        ResultItemAdapter() {
            super(SearchableActivity.this,android.R.layout.simple_list_item_1, _resultItems);
        }

        public View getView(int position, View convertView, ViewGroup parent){
            View row = convertView;
            ResultItemWrapper wrapper;
            if (row == null){
                LayoutInflater inflater=getLayoutInflater();
                row = inflater.inflate(R.layout.search_result, null);
                wrapper = new ResultItemWrapper(row);
                row.setTag(wrapper);
            } else {
                wrapper = (ResultItemWrapper) row.getTag();
            }
            wrapper.populateFrom(_resultItems.get(position));
            return(row);
        }
    }

    /**
     * Actually puts the data in the appropriate fields
     */

    private class ResultItemWrapper{
        private TextView _title;
        private TextView _description;
        private View row;

        public ResultItemWrapper(View row){
            this.row = row;
        }

        public void populateFrom(ResultItem r){
            getTitle().setText(r.getTitle());
            getDescription().setText(Html.fromHtml(r.getDescription()));
        }

        TextView getTitle(){
            if (_title == null)
                _title=(TextView)row.findViewById(R.id.title);
            return _title;
        }

        TextView getDescription(){
            if (_description == null)
                _description=(TextView)row.findViewById(R.id.description);
            return _description;
        }

    }
}