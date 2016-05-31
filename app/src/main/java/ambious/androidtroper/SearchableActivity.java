package ambious.androidtroper;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SearchView;

import static java.util.regex.Pattern.matches;

/**
 * Created by Elad on 25/09/13.
 */

public class SearchableActivity extends Activity {

    private final String LOG_TAG = "AndroidTroper: Search";
    private Intent _intent;
    private WebView _webView;
    private SharedPreferences _mainPreferences;
    private WebViewClient _webViewClient;
    private String _urlString = "http://tvtropes.org/pmwiki/search_result.php?q=";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        _mainPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.search);
        _webView = (WebView)findViewById(R.id.searchWeb);
        _webView.setWebViewClient(new InternalWebViewClient());

        /* JavaScript must be enabled if you want it to work, obviously */
        _webView.getSettings().setJavaScriptEnabled(true);

        _intent = getIntent();

        String _query = _intent.getStringExtra("query");
        getActionBar().setHomeButtonEnabled(true); //Enables Sets the home button
        getActionBar().setDisplayHomeAsUpEnabled(true); //Makes the home button go back
        doSearch(_query);
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
                doSearch(query);
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
    public void onBackPressed ()
    {
        if (_webView != null && _webView.canGoBack())
            _webView.goBack();
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case (android.R.id.home):
                setResult(RESULT_CANCELED);
                finish();
                return true;
        }
        return false;
    }

    private void doSearch(String query) {
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,MySuggestionProvider.AUTHORITY, MySuggestionProvider.MODE);
        suggestions.saveRecentQuery(query, null); //Adds the search to recent searches suggestions
        _webView.loadUrl(_urlString + query);
    }

    /**
     * An internal class for handling and redirecting links.
     *  Any link pressed in the program will be redirected to this application.
     * @author Elad Avron
     */
    public class InternalWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            //Check if local link
            boolean isArticle = matches("(?i)^(https?:\\/\\/)?(www\\.)?(tvtropes\\.org\\/pmwiki\\/pmwiki\\.php\\/)(\\S)*",url);
            boolean isTvTropes = matches("(?i)^(https?:\\/\\/)?(www\\.)?(tvtropes\\.org\\/)(\\S)*",url);
            boolean googleRedirect = matches("(?i)^(https?:\\/\\/)?(www\\.)?(google\\.)(\\S)*(\\/url\\?q=)(\\S)*",url);
            //
            if (isArticle)
            {
                String _name = MainActivity.parseTitle(url);
                Intent result = new Intent();
                result.putExtra("name",_name);
                result.putExtra("url",url);
                setResult(RESULT_OK, result);
                finish();
                return true;
            }
            if (isTvTropes || googleRedirect)
                return false;
            else
            {
                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }
        }

        @Override
        public void onReceivedError (WebView view, int errorCode, String description, String failingUrl){
            String errorMessage = errorCode + ":<br>" + description + "<br>" + failingUrl;
            Log.e(LOG_TAG, "WebBrowser Error: " + description + "(" + errorCode + ")");
            view.loadData(errorMessage, "text/html", "ISO-8859-1");
        }
    }

}