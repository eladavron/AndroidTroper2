package ambious.androidtroper;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.*;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
public class MainActivity extends FragmentActivity {

    /**
     * Global Variables
     */
    private static final String LOG_TAG = "AndroidTroper";
    private final int TYPE_RECENT = 0;
    private final int TYPE_FAVORITES = 1;
    private final int TYPE_READLATER = 2;
    private final int TYPE_SEARCH = 3;
    private final int TYPE_SETTINGS = 4;
    private final int FLAG_SETTINGS = 0;
    private final int FLAG_ABOUT = 1;
    private boolean _blockKey = false;

    /**
     * Instance Variables: Main Interface
     */
    private DrawerLayout _drawerLayout;
    private FrameLayout _mainFrame;
    private ListView _tabList;
    private String _title;
    private ActionBarDrawerToggle _drawerToggle;
    private final int _menuMinSize = 96;
    private ShareActionProvider _shareActionProvider;
    private TextView _titleBar;
    private boolean _isLoaded = false;

    /**
     * Settings Variables
     */
    private SharedPreferences _mainPreferences;
    private SharedPreferences _recentSettings;
    private SharedPreferences _favoriteSettings;
    private SharedPreferences _readLaterSettings;
    private static SharedPreferences _tabState;

    /**
     * Tab variables
     */
    private static MyViewPager _pager;
    private ArticlePagerAdapter _pageAdapter;
    private static TabListAdapter _tabListAdapter;
    private static ArrayList<ArticleFragment> _fragmentAdapter;

    /**
     * Creates the SINGLE instance
     * The app has to run in "SingleTop" mode, otherwise the intent-filter breaks tab functionality.ce of the app.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG," ");
        Log.d(LOG_TAG,"=====");
        Log.d(LOG_TAG,"Creating activity...");
        super.onCreate(savedInstanceState);

        //Settings//
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        _mainPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        _mainPreferences.registerOnSharedPreferenceChangeListener(prefListener);
        _recentSettings = getSharedPreferences("recentSettings",0);
        _favoriteSettings = getSharedPreferences("favoriteSettings",0);
        _readLaterSettings = getSharedPreferences("readlaterSettings",0);
        _tabState = getSharedPreferences("tabState",0);
        checkFirstRun();


        //Set Rotation//
        if (_mainPreferences.getBoolean("lockRotation",false))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);

        Log.d(LOG_TAG,"Setting up main interface...");
        //Load the main view!!//
        setContentView(R.layout.activity_main);
        setNightMode(_mainPreferences.getBoolean("nightMode",false));

        //Set interface variables//
        int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        _titleBar = (TextView)findViewById(titleId);
        _drawerLayout = (DrawerLayout) findViewById(R.id.main_layout);
        _tabList = (ListView) findViewById(R.id.tab_list);
        _mainFrame = (FrameLayout) findViewById(R.id.left_drawer);
        _title = getString(R.string.app_name); //initial title
        _drawerToggle = new ActionBarDrawerToggle(this, _drawerLayout, R.drawable.ic_drawer, R.string.drawerOpen, R.string.drawerClose)
        {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(_title);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(R.string.app_name);
            }
        };

        LinearLayout _gplusView = (LinearLayout) findViewById(R.id.gplus_clickable);
        _gplusView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://plus.google.com/117422405870944934987")));
            }
        });

        //Set Icons for main menu//
        ImageView _icon;
        _icon = (ImageView) findViewById(R.id.main_random_img);
        _icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_random));
        _icon = (ImageView) findViewById(R.id.main_recent_img);
        _icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_recent));
        _icon = (ImageView) findViewById(R.id.main_favorites_img);
        _icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_favorites));
        _icon = (ImageView) findViewById(R.id.main_settings_img);
        _icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_settings));
        _icon = (ImageView) findViewById(R.id.main_readlater_img);
        _icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_read_later));

        Spinner spinner = (Spinner) findViewById(R.id.main_menu_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.main_menu,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position)
                {
                    case 1:
                        mainMenuHandler(R.id.main_random);
                        break;
                    case 2:
                        mainMenuHandler(R.id.main_recent);
                        break;
                    case 3:
                        mainMenuHandler(R.id.main_favorites);
                        break;
                    case 4:
                        mainMenuHandler(R.id.main_readlater);
                        break;
                    case 5:
                        mainMenuHandler(R.id.main_settings);
                        break;
                    default:
                        Log.e(LOG_TAG,"Spinner menu selected unexpected value: " + position);
                        break;
                }
                parent.setSelection(0);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //Set interface settings//
        findViewById(R.id.content_frame).setKeepScreenOn(_mainPreferences.getBoolean("preventSleep",true));

        //Setup Drawer//
        _drawerLayout.setDrawerListener(_drawerToggle);		// Set the drawer toggle as the DrawerListener
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        Log.d(LOG_TAG,"Setting up tab list...");
        //Setup tab list//
        _tabListAdapter = new TabListAdapter(this, R.layout.tab_item);
        _tabList.setAdapter(_tabListAdapter); //Set the adapter for the tab list view
        _tabList.setOnItemClickListener(new TabClickListener()); // Set the tab list's click listener

        Log.d(LOG_TAG,"Initializing tab elements...");
        //Init tab elements//
        _pager = (MyViewPager) findViewById(R.id.pager);
        _fragmentAdapter = new ArrayList<ArticleFragment>();
        FragmentManager _fm = getSupportFragmentManager();
        _pageAdapter = new ArticlePagerAdapter(_fm, _fragmentAdapter);
        _pager.setAdapter(_pageAdapter);
        _pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                _tabList.setItemChecked(i, true);
                invalidateTitle();
                invalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        Log.d(LOG_TAG, "Activity Created.");
        _mainFrame.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                _isLoaded = true;
            }
        });
        handleIntent(getIntent());
    }

    private void restoreTabs()
    {
        Log.d(LOG_TAG,"Restoring tabs...");
        _pager.removeAllViews();
        int _tabCount = _tabState.getInt("tabCount",0);
        if (_tabCount <= 0)
        {
            Log.i(LOG_TAG,"Restored empty session!");
            return;
        } else {
            for (int i=0;i<_tabCount;i++)
            {
                String _name = _tabState.getString("tabName" + i,null);
                String _url = _tabState.getString("tabUrl"+ i,null);
//                String _html = tabSession.getString("tabHtml"+ i,null);
                if (_name == null || _url == null)
                {
                    Log.e(LOG_TAG,"Tried restoring a tab with a null name or url");
                    Log.e(LOG_TAG,"Name: " + _name + ", Url: " + _url);
                } else{
                    newTab(_name,_url,false);
                }
            }
            int _current = _tabState.getInt("selectedTab",0);
            _tabList.setItemChecked(_current,true);
            _tabList.setSelection(_current);
            _pager.setCurrentItem(_current);
            _tabState.edit().clear().commit();
        }
        Log.d(LOG_TAG,"Tabs restored!");
    }
    private void restoreTabs(Bundle tabSession)
    {
        if (tabSession==null)
        {
            Log.e(LOG_TAG,"Tried restoring tabs with an empty session!");
            return;
        }
        int _tabCount = tabSession.getInt("tabCount",0);
        if (_tabCount <= 0)
        {
            Log.i(LOG_TAG,"Restored empty session!");
            return;
        } else {
            for (int i=0;i<_tabCount;i++)
            {
                String _name = tabSession.getString("tabName" + i,null);
                String _url = tabSession.getString("tabUrl"+ i,null);
//                String _html = tabSession.getString("tabHtml"+ i,null);
                if (_name == null || _url == null)
                {
                    Log.e(LOG_TAG,"Tried restoring a tab with a null name or url");
                    Log.e(LOG_TAG,"Name: " + _name + ", Url: " + _url);
                } else{
                    newTab(_name,_url,false);
                }
            }
            int _current = tabSession.getInt("selectedTab",0);
            _tabList.setItemChecked(_current,true);
            _tabList.setSelection(_current);
            _pager.setCurrentItem(_current);
        }
    }

    @Override
    public void onNewIntent(Intent intent){
        handleIntent(intent);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        Log.d(LOG_TAG, "Activity stopped.");
    }

    @Override
    public void onSaveInstanceState(Bundle state)
    {
        super.onSaveInstanceState(state);
        Log.d(LOG_TAG,"Saving tabs for restoration...");
        int _tabCount = _tabListAdapter.getCount();
        for (int i=0;i<_tabCount;i++)
        {
            String _name = _tabListAdapter.getItem(i).getName();
            String _url = _tabListAdapter.getItem(i).getUrl();
//            String _html = _tabListAdapter.getItem(i).getHtml();
            state.putString("tabName" + i,_name);
            state.putString("tabUrl" + i,_url);
//            state.putString("tabHtml" + i, _html);
        }
        state.putInt("tabCount",_tabCount);
        state.putInt("selectedTab", _tabList.getSelectedItemPosition());
        Log.d(LOG_TAG,"Tabs saved.");
    }

    public void saveOpenTabs ()
    {
        Log.d(LOG_TAG,"Saving open tabs...");
        int _tabCount = _tabListAdapter.getCount();
        SharedPreferences.Editor editor = _tabState.edit();
        for (int i=0;i<_tabCount;i++)
        {
            String name = _tabListAdapter.getItem(i).getName();
            String url = _tabListAdapter.getItem(i).getUrl();
//            String html = _tabListAdapter.getItem(i).getHtml();
            editor.putString("tabName" + i,name);
            editor.putString("tabUrl" + i,url);
        }
        editor.putInt("tabCount",_tabCount);
        editor.putInt("selectedTab",_tabList.getSelectedItemPosition());
        editor.commit();
        Log.d(LOG_TAG,"Tabs saved.");
    }

    @Override
    public void onRestoreInstanceState(final Bundle state)
    {
        //Restoring state
        Log.d(LOG_TAG,"Restoring activity...");
        Log.d(LOG_TAG, "_pager : " + _pager.getChildCount());
        Log.d(LOG_TAG, "_fragmentAdapter : " + _fragmentAdapter.size());
        Log.d(LOG_TAG, "_pageAdapter : " + _pageAdapter.getCount());
        Log.d(LOG_TAG, "_tabListAdapter : " + _tabListAdapter.getCount());
        Log.d(LOG_TAG, "_tabList : " + _tabList.getCount());
        _pager.removeAllViews();

        super.onRestoreInstanceState(state);
        _mainFrame.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            public void onGlobalLayout() {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    _mainFrame.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    _mainFrame.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                restoreTabs(state);
            }
        });
    }

    /**
     * Incoming intent filter
     * @param _incomingIntent the incoming intent
     */
    private void handleIntent(Intent _incomingIntent)
    {
        try{
            if (_incomingIntent == null)
            {
                Log.e(LOG_TAG,"Incoming intent is null!");
                return;
            }
            if (_incomingIntent.getAction().equals(Intent.ACTION_SEARCH)) //Search requests
            {
                String _query = _incomingIntent.getStringExtra(SearchManager.QUERY);
                Intent _intent = new Intent(this, SearchableActivity.class);
                _intent.putExtra("query", _query);
                startActivityForResult(_intent, TYPE_SEARCH);
            }
            else if (_incomingIntent.getAction().equals(Intent.ACTION_VIEW)) { //URL Navigation
                String _name = null;
                if (_incomingIntent.getExtras() != null)
                    _name = _incomingIntent.getExtras().getString("name");
                final String _url = _incomingIntent.getDataString();
                if (_name == null)
                    _name = getString(R.string.incomingExternal);
                if (_url == null)
                {
                    errorDialog(getString(R.string.error), getString(R.string.incomingError),new NullPointerException());
                    return;
                }
                final String __name = _name;
                if (_isLoaded)
                    newTab(_name,_url,true);
                else{
                    _mainFrame.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                        public void onGlobalLayout() {
                            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                                _mainFrame.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            } else {
                                _mainFrame.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                            newTab(__name,_url,true);
                        }
                    });
                }
            }
            else //An unregistered or 'regular' intent
            {
                _drawerLayout.openDrawer(_mainFrame);
            }

        }
        catch (Exception e)
        {
            Log.e(LOG_TAG,"An error occurred redirecting the intent!");
            Log.e(LOG_TAG,e.getMessage() + "");
            Log.e(LOG_TAG, Log.getStackTraceString(e));
            errorDialog(getString(R.string.error), getString(R.string.incomingError),e);
        }
    }

    /**
     * Show an exit confirmation
     */
    private void confirmExit()
    {
        if (!_mainPreferences.getBoolean("confirmExit",true)) {
            if (_mainPreferences.getBoolean("saveTabs",true))
                saveOpenTabs();
            finish();
        } else {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle(R.string.exitConfirm);
            alertDialog.setIcon(R.drawable.ic_launcher);
            alertDialog.setMessage(R.string.exitConfirmText);
            alertDialog.setPositiveButton(R.string.exitYes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (_mainPreferences.getBoolean("saveTabs",true))
                        saveOpenTabs();
                    finish();
                }
            });
            alertDialog.setNegativeButton(R.string.exitNo, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            });
            CheckBox checkBox = new CheckBox(getApplicationContext());
            checkBox.setText(R.string.neverAsk);
            checkBox.setTextColor(getResources().getColor(android.R.color.black));
            int id = Resources.getSystem().getIdentifier("btn_check_holo_light", "drawable", "android");
            checkBox.setButtonDrawable(id);
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CheckBox checkBox = (CheckBox) v;
                    if (_mainPreferences.getBoolean("confirmExit",true) == checkBox.isChecked())
                    {
                        SharedPreferences.Editor _mainEditor = _mainPreferences.edit();
                        _mainEditor.putBoolean("confirmExit", !checkBox.isChecked());
                        _mainEditor.commit();
                    }
                }
            });
            alertDialog.setView(checkBox);
            alertDialog.create().show();
        }
    }

    /**
     * An generic error dialog displayed to the user.
     */
    private void errorDialog()
    {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(R.string.app_name);
        alertDialog.setMessage(R.string.unexpectedError);
        alertDialog.setNegativeButton(android.R.string.ok,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    /**
     * An error dialog with a custom title and message
     * @param title
     * @param message
     */
    private void errorDialog(String title, String message, final Exception e)
    {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.setPositiveButton(R.string.error, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String errorLog = e.getMessage() + "\n" + Log.getStackTraceString(e);
                try {
                    File myFile = new File("/sdcard/androidtroper.log");
                    myFile.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(myFile);
                    OutputStreamWriter myOutWriter =
                            new OutputStreamWriter(fOut);
                    myOutWriter.append(errorLog);
                    myOutWriter.close();
                    fOut.close();
                    Toast.makeText(getBaseContext(), "Log saved to \"/sdcard/androidtroper.log\"", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(getBaseContext(), e.getMessage() + "",Toast.LENGTH_LONG).show();
                }
            }
        });
        alertDialog.show();
    }

    private void mainMenuHandler(int menuTarget)
    {
        Intent intent;
        switch (menuTarget){
            case R.id.main_random:
                newTab(getString(R.string.random),"http://tvtropes.org/pmwiki/randomitem.php",true);
                break;
            case R.id.main_settings:
                intent = new Intent(this,SettingsActivity.class).addFlags(FLAG_SETTINGS);
                startActivityForResult(intent,TYPE_SETTINGS);
                break;
            case R.id.main_recent:
                intent = new Intent(this,MyListActivity.class);
                intent.setFlags(TYPE_RECENT);
                startActivityForResult(intent, TYPE_RECENT);
                break;
            case R.id.main_favorites:
                intent = new Intent(this,MyListActivity.class);
                intent.setFlags(TYPE_FAVORITES);
                startActivityForResult(intent, TYPE_FAVORITES);
                break;
            case R.id.main_readlater:
                intent = new Intent(this,MyListActivity.class);
                intent.setFlags(TYPE_READLATER);
                startActivityForResult(intent, TYPE_READLATER);
            default:
                Log.w(LOG_TAG, "Main menu adapter sent an unknown position: " + menuTarget);
                break;
        }
        _drawerLayout.closeDrawer(_mainFrame);
    }

    public void onMainMenuClick(View v){
        mainMenuHandler(v.getId());
    }

    /**
     * This method checks if the program was ever run before by checking against a boolean.
     * If it was never run before - all settings are cleared in case coming from earlier version!
     */
    private void checkFirstRun()
    {
        try{
            boolean firstRun = _mainPreferences.getBoolean("firstRun",true); //If the value doesn't exist, it means the program was never run - and the default will return true.
            if (firstRun)
            {
                Log.w(LOG_TAG,"It seems as if this is the first run of this version - all the favorites and recents will be reset as a result!");
                SharedPreferences.Editor recentEditor = _recentSettings.edit();
                recentEditor.clear();
                recentEditor.putInt("recentCount",0);
                recentEditor.commit();
                SharedPreferences.Editor favoriteEditor = _favoriteSettings.edit();
                favoriteEditor.clear();
                favoriteEditor.putInt("favCount",0);
                favoriteEditor.commit();
                SharedPreferences.Editor readLaterEditor = _readLaterSettings.edit();
                readLaterEditor.clear();
                readLaterEditor.commit();
                SharedPreferences.Editor _mainEditor = _mainPreferences.edit();
                _mainEditor.clear();
                _mainEditor.putBoolean("firstRun",false);
                _mainEditor.commit();
            }
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionInfo = packageInfo.versionName;
            if (!versionInfo.equals(_mainPreferences.getString("versionString",null)))
            {
                //If the version stored is not the same as the one launched
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.updateSummary)
                        .setTitle(R.string.updateTitle)
                        .setIcon(getResources().getDrawable(R.drawable.ic_launcher))
                        .setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create();
                TextView textView = (TextView) builder.show().findViewById(android.R.id.message);
                textView.setTextAppearance(this,android.R.style.TextAppearance_Small);
                SharedPreferences.Editor _editor = _mainPreferences.edit();
                _editor.putString("versionString",packageInfo.versionName);
                _editor.commit();
            }
            if (!_mainPreferences.getBoolean("tabletModeSet",false)) //Settings have not been entered, some settings need to be enforced
            {
                SharedPreferences.Editor editor = _mainPreferences.edit();
                editor.putBoolean("tabletView",getResources().getBoolean(R.bool.isTablet)).putBoolean("tabletModeSet",true).commit();
            }
        }
        catch (PackageManager.NameNotFoundException nnfex)
        {
            Log.e(LOG_TAG, "Could not get version information");
            return;
        }
    }

    /**
     * A listener for changed preferences.
     * This has to be outside of a method otherwise the garbage collector would throw it away and ignore it.
     */

    private void validateRotation(){
        try {
            if (_mainPreferences.getBoolean("lockRotation", false))
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
            else
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);

            // Check screen height

            int drawer_height = findViewById(R.id.left_drawer).getMeasuredHeight();
            int listHeight = findViewById(R.id.main_menu_layout).getMeasuredHeight();

            if ((_tabListAdapter != null && _tabListAdapter.getCount() > 0 && listHeight > (drawer_height / 2))|| listHeight >= drawer_height){
                findViewById(R.id.main_menu_layout).setVisibility(View.GONE);
                findViewById(R.id.main_menu_spinner).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.main_menu_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.main_menu_spinner).setVisibility(View.GONE);
            }
            if (_pager.getVisibleArticle() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && _mainPreferences.getBoolean("kitkatReflow", true))
            {
                WebView webView = _pager.getWebView(_pager.getVisibleArticle().getIndex());
                if (webView != null){
                    webView.evaluateJavascript("reflow()", null);
                    webView.setScrollX(0);
                }
            }
        } catch (Exception ex)
        {
            Log.e(LOG_TAG,"Something went wrong validating rotation!");
        }
    }

    Animation.AnimationListener blockKeyListener =
            new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    _blockKey = true;
                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            };

    SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs,String key) {
                    if (key.equals("showSpoilers") || key.equals("highlightSpoilers")) {
                        //TODO: refresh open articles
                    } else if (key.equals("nightMode")){
                        setNightMode(_mainPreferences.getBoolean("nightMode", false));
                    } else if (key.equals("allowZoom")){
                        for (int i=0;i<_pageAdapter.getCount();i++)
                        {
                            _pager.getWebView(i).getSettings().setSupportZoom(_mainPreferences.getBoolean("allowZoom",true));
                        }
                    } else if (key.equals("zoomToolbar")){
                        for (int i=0;i<_pageAdapter.getCount();i++)
                        {
                            _pager.getWebView(i).getSettings().setBuiltInZoomControls(_mainPreferences.getBoolean("allowZoom",true));
                        }
                    } else if (key.equals("lockRotation")){
                        if  (!_mainPreferences.getBoolean("lockRotation",false))
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                        else
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                    }
                }
            };


    private void setNightMode(boolean _state)
    {
        if (_state) //Settings nightmode on
        {
            View mainView = findViewById(R.id.main_layout);
            if (mainView != null)
            {
                mainView.setBackgroundColor(getResources().getColor(android.R.color.black));
                ((TextView)mainView.findViewById(R.id.gplusText)).setTextColor(getResources().getColor(android.R.color.white));
            }
            if (_pager != null)
            {
                for (int i=0;i<_pager.getChildCount();i++)
                {
                    _pager.getWebView(i).setBackgroundColor(Color.BLACK);
                    _pager.getWebView(i).loadUrl("javascript:nightMode()");
                }
            }
        }
        else //Settings nightmode off
        {
           View mainView = findViewById(R.id.main_layout);
            if (mainView != null)
            {
                mainView.setBackgroundColor(getResources().getColor(android.R.color.white));
                ((TextView)mainView.findViewById(R.id.gplusText)).setTextColor(getResources().getColor(android.R.color.black));
            }
            if (_pager != null)
            {
                for (int i=0;i<_pager.getChildCount();i++)
                {
                    _pager.getWebView(i).setBackgroundColor(Color.WHITE);
                    _pager.getWebView(i).loadUrl("javascript:dayMode()");
                }
            }
        }
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        _drawerToggle.syncState();
        if (_tabState.getInt("tabCount",0) != 0)
        {
            _mainFrame.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        _mainFrame.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    } else {
                        _mainFrame.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                    validateRotation();
                    restoreTabs();
                }
            });
        }
    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        _drawerToggle.onConfigurationChanged(newConfig);
        _mainFrame.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    _mainFrame.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    _mainFrame.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                validateRotation();
            }
        });
    }

    /**
     * Create option menu
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_options, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView _searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        _searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        _searchView.setSubmitButtonEnabled(true);
        _searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Closes the search widget
                _searchView.setIconified(true);
                _searchView.onActionViewCollapsed();
                return false; //If returned true, it will override the search query request
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        //Set the checkboxes according to settings
        menu.findItem(R.id.action_nightMode).setChecked(_mainPreferences.getBoolean("nightMode", false));
        menu.findItem(R.id.action_lockRotation).setChecked(_mainPreferences.getBoolean("lockRotation",false));

        //Show or hide buttons according to what is displayed.
        if (_fragmentAdapter!= null && _fragmentAdapter.size() > 0) //There are articles
        {
            ArticleFragment _visibleFragment = _pager.getVisibleFragment();
            if (_visibleFragment != null && _pager.getWebView(_pager.getCurrentItem()) != null && _pager.getWebView(_pager.getCurrentItem()).getVisibility() == View.VISIBLE) //An article is displayed
            {
                menu.findItem(R.id.action_shuffle).setVisible(true);
                menu.findItem(R.id.action_favorite).setVisible(true);
                menu.findItem(R.id.action_find).setVisible(true);
                menu.findItem(R.id.action_share).setVisible(true);
                _shareActionProvider = (ShareActionProvider) menu.findItem(R.id.action_share).getActionProvider();
                _shareActionProvider.setShareIntent(getDefaultShareIntent());
            }
            else
            {
                menu.findItem(R.id.action_shuffle).setVisible(false);
                menu.findItem(R.id.action_favorite).setVisible(false);
                menu.findItem(R.id.action_find).setVisible(false);
                menu.findItem(R.id.action_share).setVisible(false);
            }
        }
        return true;
    }

    private Intent getDefaultShareIntent() {
        String htmlText = "<p>" + getString(R.string.shareText) + "<br><b><a href=\"" + _pager.getVisibleArticle().getUrl() + "\">" + _pager.getVisibleArticle().getName() + ":<br>" + _pager.getVisibleArticle().getUrl() + "</a></p>" + getString(R.string.shareSignature);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shareSubject));
        sendIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(htmlText).toString());
        sendIntent.putExtra(Intent.EXTRA_HTML_TEXT,htmlText);
        sendIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.shareTitle));
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        return sendIntent;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        return onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (_drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...
        switch (item.getItemId()){
            case R.id.action_search: //Search
                onSearchRequested();
                return true;
            case R.id.action_settings: //Settings
                Intent intent = new Intent(this,SettingsActivity.class).addFlags(FLAG_SETTINGS);
                startActivityForResult(intent,TYPE_SETTINGS);
                return true;
            case R.id.action_nightMode: //Nightmode toggle
                if(_mainPreferences.getBoolean("nightMode", false) == false) //Nightmode was off, turn it on!
                {

                    SharedPreferences.Editor _editor = _mainPreferences.edit();
                    _editor.putBoolean("nightMode", true);
                    _editor.commit();
                    item.setChecked(true);
                    return true;
                }
                else //Nightmode was off, turn it on!
                {
                    SharedPreferences.Editor _editor = _mainPreferences.edit();
                    _editor.putBoolean("nightMode",false);
                    _editor.commit();
                    item.setChecked(false);
                    return true;
                }
            case R.id.action_lockRotation: //Rotation settings
                if(!item.isChecked()) //Wasn't locked - lock it!
                {
                    SharedPreferences.Editor _editor = _mainPreferences.edit();
                    _editor.putBoolean("lockRotation", true);
                    _editor.commit();
                    item.setChecked(true);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                    return true;
                }
                else { //Locked - unlock it!
                    SharedPreferences.Editor _editor = _mainPreferences.edit();
                    _editor.putBoolean("lockRotation", false);
                    _editor.commit();
                    item.setChecked(false);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                    return true;
                }
            case R.id.action_shuffle: //Random
                getArticle("Random","http://tvtropes.org/pmwiki/randomitem.php",_pager.getCurrentItem());
                return true;
            case R.id.action_favorite: //Add to favorites
                addToFavorites(_pager.getVisibleArticle().getName(),_pager.getVisibleArticle().getUrl());
                return true;
            case R.id.action_find:
                _pager.getWebView(_pager.getCurrentItem()).showFindDialog(null,true);
                return true;
            case R.id.action_about: //About
                Intent _intent = new Intent(this,SettingsActivity.class).addFlags(FLAG_ABOUT);
                startActivity(_intent);
                return true;
            case R.id.action_share:
                return true;
            case R.id.action_exit:
                confirmExit();
                return true;
            default:
                Log.wtf(LOG_TAG, "An unregistered action menu was selected!");
                Log.wtf(LOG_TAG, "Item ID: " + item.getItemId());
                Log.wtf(LOG_TAG, "Item Title: " + item.getTitle());
                return false;
        }
    }

    /**
     * Intercept the "KeyDown" event
     * @param keyCode the key code.
     * @param event they key event triggered.
     * @return true if handled, false if not.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) //Intercept "Back" key
        {
            //Detect if current view is an article view
            if (_tabList.getCount() <= 0 || _pager.getCurrentItem() == -1)
            {
                confirmExit();
                return true;
            }
            //If the drawer is open, simple close it
            if (_drawerLayout.isDrawerOpen(_mainFrame))
            {
                _drawerLayout.closeDrawer(_mainFrame);
                return true;
            }
            int _current = _pager.getCurrentItem();
            ArticleFragment _visibleFragment = _fragmentAdapter.get(_current);
            if (_blockKey)
            {
                Log.d(LOG_TAG,"Animation pending, blocking 'Back' key!");
                return false;
            }
            if (_visibleFragment != null && _pager.getLoadingScreen(_current).getVisibility() == View.VISIBLE && _pager.getWebView(_current) != null && _pager.getWebView(_current).getVisibility() == View.VISIBLE) //If it's currently loading something AND there's an underlying webpage
            {
                _pager.getVisibleArticle().getArticleFetcher().cancel(true);
                _pager.getLoadingScreen(_pager.getCurrentItem()).setVisibility(View.GONE);
                invalidateTitle();
                return true;
            } else if (_visibleFragment != null && _pager.getLoadingScreen(_current).getVisibility() == View.VISIBLE && _pager.getWebView(_current) != null && _pager.getWebView(_current).getVisibility() == View.GONE) //If it's currently loading something but there's NO  underlying webpage
            {
                _pager.getVisibleArticle().getArticleFetcher().cancel(true);
                removeTab(_current);
                return true;
            } else if (_visibleFragment != null && _visibleFragment.getHistorySize() > 1) //There's way to go back
            {
                _visibleFragment.History.pop(); //Removes the last one, as it is the CURRENT one
                Article lastItem = _visibleFragment.History.peek();
                parseArticle (lastItem,false);
                return true;
            }
            else if (_visibleFragment != null && _visibleFragment.getHistorySize() == 1) //No way back, it's the last page.
            {
                removeTab(_pager.getCurrentItem());
                return true;
            }
            else
            {
                confirmExit();
                return true;
            }
        }
        //Nothing intercepted, call global handler.
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Processes search selections
     * @param requestCode -
     * @param resultCode - Checks if the request was successful or not
     * @param data - Contains the actual results.
     */
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK && (requestCode == TYPE_FAVORITES || requestCode == TYPE_READLATER || requestCode == TYPE_SEARCH || requestCode == TYPE_RECENT))
        {
            String _name = data.getStringExtra("name");
            String _url = data.getStringExtra("url");
            if (_url==null || _name==null)
            {
                Log.e(LOG_TAG,"One of the parameters returned by the activity is null!");
                Log.e(LOG_TAG,"_url=" + _url + "; _name=" + _name);
                return;
            }
            animatedNewTab(_name, _url, true);
        } else if (resultCode == RESULT_OK && requestCode == TYPE_SETTINGS)
        {
            Boolean restart = data.getBooleanExtra("restart",false);
            if (restart)
            {
                System.exit(0);
            }
        }
        else
            Log.i(LOG_TAG,"Activity returned error, see above");
    }

    /**
     * Parses a url from any form to a standard TVTropes form
     * @param url input url - regular or not
     * @return full TVTropes standard url
     */
    private String uriParse(String url)
    {
        String u = Uri.parse(url).getHost();
        if (u == null) //It's a relative link
        {
            url = "http://tvtropes.org/" + url; //Alter it to a global link
        }
        return url;
    }

    /**
     * Creates the context menu for links in articles
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        try{
            super.onCreateContextMenu(menu, v, menuInfo);
            android.view.MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.link_menu, menu);
            final String name = (String) menu.findItem(1).getTitle();
            final Uri url = Uri.parse(uriParse((String) menu.findItem(0).getTitle()));
            menu.findItem(0).setVisible(false);
            menu.findItem(1).setVisible(false);
            View headerView = getLayoutInflater().inflate(R.layout.header, null);
            TextView header = (TextView) headerView.findViewById(R.id.header);
            TextView subHeader = (TextView) headerView.findViewById(R.id.sub_header);
            boolean _isLocal;
            String u = url.getHost();
            if (u == null || u.equals("tvtropes.org")) //It's a relative or tvtropes link
            {
                _isLocal = true;
                header.setText(name);
            }
            else  //If it's not a TVTropes link
            {
                _isLocal = false;
                header.setText(getString(R.string.externalLink));
            }

            subHeader.setText(url.toString());

            menu.findItem(R.id.open_new).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(android.view.MenuItem item) {
                    animatedNewTab(name, url.toString(), _mainPreferences.getBoolean("moveNow", false));
                    return true;
                }});
            menu.findItem(R.id.open_external).setIntent(new Intent(Intent.ACTION_VIEW, url));
            menu.setHeaderView(headerView);
            menu.findItem(R.id.open_external).setVisible(!_isLocal);
            menu.findItem(R.id.open_new).setVisible(_isLocal);
            menu.findItem(R.id.link_fav).setVisible(_isLocal);
            menu.findItem(R.id.link_fav).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    addToFavorites(name,url.toString());
                    return true;
                }
            });
            menu.findItem(R.id.read_later).setVisible(_isLocal);
            menu.findItem(R.id.read_later).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    addToReadLater(name,url.toString());
                    return true;
                }
            });
        }
        catch (NullPointerException e){
            menu.clear();
            Log.e(LOG_TAG,"Tried to set a non-existing menu item.");
            return;
        } catch (Exception e)
        {
            menu.clear();
            Log.e(LOG_TAG, "Context menu requested but failed.");
            return;
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        _title = title.toString();
        ActionBar _actionBar = getActionBar();
        _actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        _actionBar.setDisplayShowTitleEnabled(true);
        _actionBar.setTitle(_title);
    }

    /**
     * Animated the process of addint a new tab by opening the drawer and closing it afterwards.
     * Unfortunately, the time delays for the animation are completely arbitrary
     */
    private void animatedNewTab(final String name, final String url, final boolean moveNow)
    {
        _drawerLayout.openDrawer(_mainFrame);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                newTab(name, url, moveNow);
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        _drawerLayout.closeDrawer(_mainFrame);
                        _blockKey = false;
                    }
                }, 200);
            }
        },300);
    }

    /**
     * Create a new tab and open a URL in it
     * @param name Name of the Article
     * @param url URL of the Article
     */
    public void newTab(String name, String url, boolean moveNow){
        _blockKey = true;

        //First draw divider if needed
        int _position = _tabListAdapter.getCount(); //Gets the physical position in the list to add the tab to
        if (_position == 0){
            View divider = findViewById(R.id.list_divider);
            if (divider != null)
                divider.setVisibility(View.VISIBLE);
        }

        //Now determine the new index of the article
        final int _newID = _tabListAdapter.getCount(); //The new index is equal to the old count because it starts at 0.
        Article _newArticle = new Article(name,url,_newID);
        ArticleFragment _newFragment = new ArticleFragment(_newArticle, _newID);
        _pager.setOffscreenPageLimit(_pager.getOffscreenPageLimit() + 1);
        _tabListAdapter.add(_newArticle);
        _tabListAdapter.notifyDataSetChanged();
        _fragmentAdapter.add(_newFragment);
        _pageAdapter.notifyDataSetChanged();
        getArticle(name, url, _fragmentAdapter.indexOf(_newFragment)); //_fragmentAdapter.indexOf(_newFragment) should technically be equal to _newID
        if (moveNow){
            if (_pager.getChildCount() == 1)
            {
                Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
                fadeIn.setAnimationListener(blockKeyListener);
                _newFragment.getView().startAnimation(fadeIn);
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        _pager.setCurrentItem(_newID);
                        _blockKey = false;
                    }
                }, fadeIn.getDuration());
            }
            else
            {
                final FrameLayout _oldTab =(FrameLayout) _pager.getChildAt(_pager.getCurrentItem()).findViewById(R.id.frame_layout);
                final FrameLayout _newTab =(FrameLayout) _pager.getChildAt(_newID).findViewById(R.id.frame_layout);
                final Animation animOut = AnimationUtils.loadAnimation(this,R.anim.slide_out_left);
                final Animation animIn = AnimationUtils.loadAnimation(this,R.anim.zoom_in);

                animOut.setAnimationListener(blockKeyListener);
                animIn.setAnimationListener(blockKeyListener);

                animOut.setFillAfter(false);
                _oldTab.startAnimation(animOut);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        _pager.setCurrentItem(_newID,false);
                        _newTab.startAnimation(animIn);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                _blockKey = false;
                            }
                        }, animIn.getDuration());
                    }
                }, animOut.getDuration());
            }
            _tabList.setItemChecked(_position, true);
            validateRotation();
            _blockKey = false;
        }
    }

    private void removeTab(final int index)
    {
        try
        {
            _blockKey = true;
            final Animation animOut = AnimationUtils.loadAnimation(this,R.anim.zoom_out);
            animOut.setAnimationListener(blockKeyListener);
            FrameLayout tabToRemove = (FrameLayout) _pager.getChildAt(index).findViewById(R.id.frame_layout);
            if (_pager.getCurrentItem() == index) //If the currently removed tab is displayed
            {
                tabToRemove.setForeground(getResources().getDrawable(R.drawable.trash));
                tabToRemove.setForegroundGravity(Gravity.CENTER);
                tabToRemove.startAnimation(animOut);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (_fragmentAdapter.size() > 1) //If the tab displayed is not the last one
                        {
                            if (index != 0) //Not removing the FIRST item
                                _pager.setCurrentItem(index-1,true); //Select the item BEFORE the current one
                            else //Removing the first item
                                _pager.setCurrentItem(index+1,true); //This should never be null because if it's the first AND the last item, it should not trigger
                        }
                        else //Removing the last displayed tab
                        {
                            View divider = findViewById(R.id.list_divider);
                            if (divider != null)
                                divider.setVisibility(View.GONE);
                        }
                        removeTabStep2(index);
                    }
                }, animOut.getDuration());
            }
            else
                removeTabStep2(index);
        }
        catch (IndexOutOfBoundsException ex)
        {
            Log.e(LOG_TAG, ex.getMessage() + "");
            return;
        }
    }

    private void removeTabStep2(int index)
    {
        _tabListAdapter.getItem(index).getArticleFetcher().cancel(true);
        _fragmentAdapter.remove(_fragmentAdapter.get(index));
        _pageAdapter.notifyDataSetChanged();
        _pager.setOffscreenPageLimit(_pager.getOffscreenPageLimit() - 1);
        _pager.invalidate();
        _tabListAdapter.remove(_tabListAdapter.getItem(index));
        _tabListAdapter.notifyDataSetChanged();
        _tabListAdapter.invalidateIndices();
        invalidateTitle();
        invalidateOptionsMenu();
        _blockKey = false;
    }

    private void getArticle(String name, String url, int index)
    {
        //Get Article
        if (url == null || name == null || index < 0)
        {
            Log.e(LOG_TAG,"One of the parameters passed to the method is null");
            Log.e(LOG_TAG,"name=" + name + "; url=" + url + "index=" + index);
            return;
        }
        url = uriParse(url);
        if (_pager.getCurrentItem() == index) //If the target tab is the currently displayed one
            setTitle(name);
        Log.d(LOG_TAG,"Getting Article #" + index + ": \"" + name + "\" (" + url + ")");
        ArticleFetcher af = new ArticleFetcher(index, this);
        _tabListAdapter.getItem(index).setArticleFetcher(af);
        af.execute(url, name);
    }

    public void parseArticle(Article _article, boolean addToHistory) {
        try{
            if (_article == null) throw new Exception("_article is null!");
            String _name = _article.getName();
            String _html = _article.getHtml();
            String _url = _article.getUrl();
            int _id = _article.getIndex();
            _tabListAdapter.replaceArticle(_id,_article);
            _tabListAdapter.notifyDataSetChanged();
            ArticleFragment _targetTab = _fragmentAdapter.get(_id); //Gets the fragment for the parsed tab
            if (addToHistory)
            {
                _targetTab.addToHistory(_article);
                addToRecent(_name, _url);
            }
            MyWebView _webView = _pager.getWebView(_id);
            _webView.setWebViewClient(new InternalWebViewClient(_id));
            _webView.getSettings().setSupportZoom(_mainPreferences.getBoolean("allowZoom", true));
            _webView.getSettings().setBuiltInZoomControls(_mainPreferences.getBoolean("zoomToolbar", true));
            _webView.getSettings().setDefaultTextEncodingName("iso-8859-1");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                Log.d(LOG_TAG,"Article loaded with scale: " + _webView.getScale());
            }
            if (_mainPreferences.getBoolean("nightMode",false))
                _webView.setBackgroundColor(Color.BLACK);
            else
                _webView.setBackgroundColor(Color.WHITE);
            _webView.loadDataWithBaseURL(null, _html, "text/html", "iso-8859-1", null);
            _webView.scrollTo(0,0);
            RelativeLayout _loadingScreen = _pager.getLoadingScreen(_id);
            _loadingScreen.setVisibility(View.GONE);
            _webView.setVisibility(View.VISIBLE);

            if (_pager.getVisibleArticle().getIndex() == _article.getIndex()) //If this is the currently shown tab
            {
                invalidateTitle();
                invalidateOptionsMenu();
            }
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG,"Unexpected error displaying article!");
            Log.e(LOG_TAG, e.getMessage() + "");
            return;
        }
    }

    private void invalidateTitle()
    {
        try{
            Article _article = _pager.getVisibleArticle();
            if (_article == null)
                if (_tabList.getChildCount() > 0 &&_tabList.getSelectedItem()!= null)
                    setTitle(((Article) _tabList.getSelectedItem()).getName());
                else
                    setTitle(R.string.app_name);
            if (_article.getSubPages() != null && _article.getSubPages().size() > 0) //There are subpages, display dropdown navigation
            {
                ActionBar _actionBar = getActionBar();
                _actionBar.setDisplayShowTitleEnabled(false);
                _actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                ArrayList<Article> itemList = _article.getSubPages();
                ArrayAdapter _adapter = new SubpageAdapter(getActionBar().getThemedContext(),R.layout.dropdown_item,android.R.id.text1,itemList);
                _actionBar.setListNavigationCallbacks(_adapter, new onNavigationListener());
            }
            else
                setTitle(_article.getName());
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "Error setting title - reverting to app name!");
            Log.e(LOG_TAG, e.getMessage() + "");
            setTitle(R.string.app_name);
        }
    }

    /**
     * Adds an article to the history. The items are unsorted, so new items are added at the end, and if the list is full (as determined by the maximum value) it replaces the first one.
     * @param _title Title of the new article to add
     * @param _url URL of the new article to add
     */
    private void addToRecent(String _title, String _url){
        try{
            SharedPreferences.Editor editor = _recentSettings.edit();
            int _recentCount = _recentSettings.getInt("recentCount", 0); //0 is always empty
            int _recentMax = Integer.valueOf(_mainPreferences.getString("recentMax","10")); //Index is between 1-10 (default)
            Time now = new Time();
            now.setToNow();
            String _time = now.toString();

            //First, check that it doesn't already exist
            for (int i=1;i<=_recentMax;i++){
                String recentUrl = _recentSettings.getString("recentUrl" + i, null);
                if (_title.equals(null)) //Got to end of populated list
                    break;
                if (_url.equals(recentUrl))
                {
                    Log.i(LOG_TAG,"Article already in history, updating time string");
                    editor.putString("recentTime" + i, _time);
                    editor.commit();
                    return;
                }
            }
            //If we reached here - it doesn't already exist
            int _newSlot = -1;
            if (_recentCount < _recentMax){ 	//If not all slots are filled
                _newSlot = _recentCount + 1;	//New slot it count + 1
                _recentCount++;
            }
            else //All slots are filled
            {
                String oldestTime = now.toString();
                //Find the oldest article and replace it
                for (int i=1;i<=_recentCount;i++){
                    if (_recentSettings.getString("recentTime" + i,null).compareTo(oldestTime) < 0)
                    {
                        _newSlot = i;
                        oldestTime =  _recentSettings.getString("recentTime" + i,null);
                    }
                }
            }
            if (_newSlot <= 0)
                throw new Exception("Couldn't determine number of recent pages");
            editor.putString("recentTitle" + _newSlot, _title);
            editor.putString("recentUrl" + _newSlot, _url);
            editor.putString("recentTime" + _newSlot, _time);
            editor.putInt("recentCount", _recentCount);
            editor.commit();
        }	catch (Exception e){
            Log.e(LOG_TAG, "Saving to history failed!");
            e.printStackTrace();
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
                String favUrl = _readLaterSettings.getString("rlUrl" + i, null);
                if (_url.equals(favUrl))
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

    /**
     * A static String function that gets the article's title from its URL - albeit not spaced.
     * @param url the article's URL to parse
     * @return the article's name
     */
    static String parseTitle(String url)
    {
        String[] splitUrl = url.split("/");
        return splitUrl[splitUrl.length - 1];
    }

    /**
     * A class for articles, containing Name, URL, and additional info
     */
    public class Article
    {
        private String _name;
        private String _url;
        private String _html;
        private int _index;
        private ArrayList<Article> _subPages;
        private ArticleFetcher _af;

        public Article(String name, String url, int index)
        {
            _name = name;
            _url = url;
            _index = index;
        }
        public Article(String name, String url, int index, String html)
        {
            _name = name;
            _url = url;
            _index = index;
            _html = html;
        }
        public Article(String name, String url, int index, String html, ArrayList<Article> subPages)
        {
            _name = name;
            _url = url;
            _index = index;
            _html = html;
            _subPages = subPages;
        }
        public void setSubPages(ArrayList<Article> subPages)
        {
            _subPages = subPages;
        }
        public ArrayList<Article> getSubPages()
        {
            return _subPages;
        }
        public void replace(Article newArticle)
        {
            _name = newArticle.getName();
            _url = newArticle.getUrl();
            _html = newArticle.getHtml();
            _subPages = newArticle.getSubPages();
        }
        public void updateIndex(int newIndex)
        {
            _index = newIndex;
            _af.updateIndex(newIndex);
        }

        public String getName()
        {
            return _name;
        }
        public String getUrl()
        {
            return _url;
        }

        public int getIndex()
        {
            return _index;
        }
        public String getHtml()
        {
            return _html;
        }
        public String toString()
        {
            return _name;
        }
        public void setName(String name)
        {
            _name = name;
        }
        public void setUrl(String url)
        {
            _url = url;
        }
        public void setHtml (String html)
        {
            _html = html;
        }
        public void setArticleFetcher (ArticleFetcher af)
        {
            _af = af;
        }
        public ArticleFetcher getArticleFetcher ()
        {
            return _af;
        }
    }

    public class onNavigationListener implements ActionBar.OnNavigationListener {

        @Override
        public boolean onNavigationItemSelected(int itemPosition, long itemId) {
            try{
                if (_blockKey)
                {
                    Handler myHandler = new Handler();
                    final int _itemPosition = itemPosition;
                    final long _itemId = itemId;
                    myHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onNavigationItemSelected(_itemPosition, _itemId);
                        }
                    }, 500);
                }
                Article _activeArticle = _pager.getVisibleArticle();
                String name = _activeArticle.getSubPages().get(itemPosition).getName();
                String url = _activeArticle.getSubPages().get(itemPosition).getUrl();
                if (_activeArticle.getUrl().equals(url)) //It's the already loaded page
                    return true;
                else
                    getArticle(name, url, _pager.getCurrentItem());
                return true;
            } catch (Exception ex)
            {
                Log.e(LOG_TAG, "Error occurred navigating to article: " + ex.getMessage());
                ex.printStackTrace();
                return false;
            }
        }
    }

    public class ActiveDrawerListener implements DrawerLayout.DrawerListener {

        @Override
        public void onDrawerSlide(View view, float v) {

        }

        @Override
        public void onDrawerOpened(View view) {

        }

        @Override
        public void onDrawerClosed(View view) {

        }

        @Override
        public void onDrawerStateChanged(int i) {

        }
    }

    public class TabListAdapter extends ArrayAdapter<Article>{
        Context _context;
        int _resource;

        public TabListAdapter(Context c, int resource) {
            super(c, resource);
            _context=c;
            _resource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.tab_item,parent,false);
            TextView tabText = (TextView) rowView.findViewById(R.id.tab_text);
            ImageButton closeButton = (ImageButton) rowView.findViewById(R.id.tabClose);
            tabText.setText(this.getItem(position).getName());
            closeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    final ListView parentView = _tabList;
                    final int index = parentView.getPositionForView(v); //Position of the INDEX position
                    final int position = parentView.getPositionForView(v) - parentView.getFirstVisiblePosition(); //Position is the VISUAL position
                    Article _article = (Article)parentView.getItemAtPosition(index); //Gets the article at the clicked position
                    Animation anim = AnimationUtils.loadAnimation(MainActivity.this, android.R.anim.slide_out_right);
                    anim.setDuration(500);
                    anim.setFillAfter(true);
                    parentView.getChildAt(position).startAnimation(anim);
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            removeTab(index);
                        }
                    }, anim.getDuration());
                }
            });
            return rowView;
        }

        //Replace an article item in the tab
        //This happens when the tab navigates to another page
        public void replaceArticle(int id, Article article)
        {
            getItem(id).replace(article);
            this.notifyDataSetChanged();
        }

        /**
         * Goes over the entire list and updates the article item with its actual index
         */
        public void invalidateIndices()
        {
            for (int i=0;i<this.getCount();i++)
            {
                this.getItem(i).updateIndex(i);
            }
        }
    }

    private class TabClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            try{
                _pager.setCurrentItem(position);
                _tabList.setItemChecked(position, true);
                _drawerLayout.closeDrawer(_mainFrame);
                invalidateTitle();
                invalidateOptionsMenu();
            } catch (Exception exception)
            {
                Log.e(LOG_TAG, "An error occured changing tabs!");
                Log.e(LOG_TAG, exception.getMessage());
            }
        }
    }
    private class SubpageAdapter extends ArrayAdapter
    {
        private int _width = 0;
        private List<Article> _articles;
        public SubpageAdapter(Context context, int resource, int textViewResourceId, List<Article> objects) {
            super(context, resource, textViewResourceId, objects);
            _articles = objects;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent){
            View row;
            LayoutInflater inflater=getLayoutInflater();
            row = inflater.inflate(R.layout.subpage_item, null);
            final Article _article = _articles.get(position);
            final TextView textView = (TextView) row.findViewById(android.R.id.text1);
            final ImageButton addToReadLater = (ImageButton) row.findViewById(R.id.addToReadLater);
            textView.setText(_article.getName());
            addToReadLater.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addToReadLater(_article.getName(), _article.getUrl());
                }
            });
            addToReadLater.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(getApplicationContext(),getString(R.string.readlater),Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            return row;
        }
    }

    /**
     * Fragment that appears in the "content_frame"
     */
    public static class ArticleFragment extends Fragment{
        public Stack<Article> History;

        public ArticleFragment(){
            //Empty constructor for fuck's sake.
        }

        public ArticleFragment (Article article, int index)
        {
            History = new Stack<Article>();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            //Set fragment local variables to interface
            Log.d(LOG_TAG,"Creating View...");

            View v = inflater.inflate(R.layout.tab_fragment,null);
            WebView _webView = (MyWebView) v.findViewById(R.id.web_view);
            _webView.getSettings().setJavaScriptEnabled(true);
            registerForContextMenu(_webView);
            return v;
        }

        public int getHistorySize()
        {
            return History.size();
        }
        public void addToHistory(Article article)
        {
            History.add(article);
        }
    }
    public class ArticlePagerAdapter extends FragmentStatePagerAdapter {
        FragmentManager _fm;
        ArrayList<ArticleFragment> _fragmentList;
        ArrayList<ArticleFragment.SavedState> _SavedState = new ArrayList<ArticleFragment.SavedState>();
        private FragmentTransaction _CurTransaction;

        public ArticlePagerAdapter(FragmentManager fm, ArrayList<ArticleFragment> fragmentList) {
            super(fm);
            _fm = fm;
            _fragmentList = fragmentList;
        }

        @Override
        public Fragment getItem(int i) {
            return _fragmentList.get(i);
        }

        @Override
        public int getCount() {
            return _fragmentList.size();
        }

        @Override
        public int getItemPosition(Object item) {
            ArticleFragment fragment = (ArticleFragment)item;
            int position = _fragmentList.indexOf(fragment);
            if (position >= 0) {
                return position;
            } else {
                return POSITION_NONE;
            }
        }

        /**
         * I have no idea how this works - I copied most of this from a custom class created by http://stackoverflow.com/users/325479/mikepenz - mentioned in http://stackoverflow.com/questions/9061325/fragmentpageradapter-is-not-removing-items-fragments-correctly
         */
        @Override
        public Object instantiateItem (ViewGroup container, int position)
        {
            if (_CurTransaction == null) {
                _CurTransaction = _fm.beginTransaction();
            }
            ArticleFragment fragment = (ArticleFragment) getItem(position);
            if (_SavedState.size() > position) {
                Fragment.SavedState fss = _SavedState.get(position);
                if (fss != null) {
                    try // DONE: Try Catch
                    {
                        fragment.setInitialSavedState(fss);
                    } catch (Exception ex) {
                        Log.w(LOG_TAG,"The fragment's initial saved state could not be saved!");
                    }
                }
            }
            while (_fragmentList.size() <= position) {
                _fragmentList.add(null);
            }
            _fragmentList.set(position, fragment);
            _CurTransaction.add(container.getId(), fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object)
        {
            ArticleFragment fragment = (ArticleFragment) object;

            if (_CurTransaction == null) {
                _CurTransaction = _fm.beginTransaction();
            }
            _CurTransaction.remove(fragment);
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            if (_CurTransaction != null) {
                _CurTransaction.commitAllowingStateLoss();
                _CurTransaction = null;
                _fm.executePendingTransactions();
            }
        }

    }

    /**
     * A class that downloads a page form the site, parses it and returns an article.
     */


    public class ArticleFetcher extends AsyncTask<String,String,Article>
    {
        private String _url;
        private String _name;
        private int _id;
        private Exception _e = null;
        private Context _context;

        public ArticleFetcher(int id, Context context)
        {
            _id = id;
            _context = context;
        }
        @Override
        protected Article doInBackground(String... args) {
            try{
                //This part of the code parses the cookie - thanks to Simon Strassl for this solution
                _url = args[0];
                _name = args[1];
                Document _doc;
                Article _returnArticle;
                if (isCancelled()) return null;
                publishProgress(getString(R.string.Connecting),_name);
                int _timeout = Integer.getInteger(_mainPreferences.getString("timeout","10"),10) * 1000;
                try{
                    Connection noCookie = Jsoup.connect(_url);
                    noCookie.timeout(_timeout);
                    Response _response = noCookie.execute();
                    Document noJS = _response.parse();
                    String cookieScript = noJS.head().getElementsByTag("script").first().html();
                    cookieScript = cookieScript.substring(cookieScript.lastIndexOf('}'));
                    Integer openParen = cookieScript.indexOf('(');
                    Integer closeParen = cookieScript.indexOf(')');
                    String cookieInfo = cookieScript.substring(openParen + 1, closeParen).replaceAll("'", "").replaceAll(" ", "");
                    Log.i("CookieInfo", cookieInfo);
                    String[] parts = cookieInfo.split(",");
                    String cName = parts[0];
                    String cIP = parts[1];
                    Log.i(LOG_TAG, "Cookie name: " + cName);
                    Log.i(LOG_TAG, "Cookie IP: " + cIP);
                    if (isCancelled()) return null;
                    publishProgress(getString(R.string.Loading),_name);
                    Response resp = Jsoup.connect(_url).timeout(_timeout).cookie(cName, cIP).execute();
                    Log.d(LOG_TAG,"Response Chatset:"+ resp.charset());
                    _url = resp.url().toString();
                    _doc = resp.parse();

                } catch (IndexOutOfBoundsException e)
                {
                    Log.i(LOG_TAG,"No cookie received, going native!");
                    if (isCancelled())
                        return null;
                    publishProgress(getString(R.string.Loading),_name);
                    Response resp = Jsoup.connect(_url).timeout(_timeout).execute();
                    _url = resp.url().toString();
                    Log.d(LOG_TAG,"Response Chatset:"+resp.charset());
                    _doc = resp.parse();
                }
                if (_doc == null) //In case no document was received
                {
                    _pager.getLoadingScreen(_id).setVisibility(View.GONE);
                    throw new Exception("No document was received.");
                }
                if (isCancelled()) return null;
                publishProgress(getString(R.string.Parsing),_name);
                //Now we parse the title
                String _taskTitle;
                if (_doc.getElementsByClass("article_title").isEmpty())
                {
                    if (_doc.title().contains(" - "))
                        _taskTitle = _doc.title().split(" - ")[0];
                    else
                        _taskTitle = _doc.title();
                } else {
                    Element _titleElement = _doc.getElementsByClass("article_title").first();
                   _taskTitle = _titleElement.text();
                }
                _name = _taskTitle; //Set the title to the fragment's internal variable
                if (isCancelled()) return null;
                publishProgress(getString(R.string.Parsing),_name);
                //Start parsing the actual article
                Element wikiText = _doc.getElementsByClass("page-content").first();
                if (wikiText == null) //In case it wasn't parsable
                    throw new Exception("Document did not parse correctly and is probably of an unsupported type!");

                //Set colors and adjust for nightmode//
                String mainColor = "black";
                String backColor = "white";
                String linkColor = "#0000FF";
                String folderStyle = "text-align:center; border:1px; border-style:solid; background-color: #EEF; border-color: #BBBBC3; border-radius:8px; padding:6px;";
                if (_mainPreferences.getBoolean("nightMode",false))
                {   //Nightmode on
                    mainColor = "white";
                    backColor = "black";
                    linkColor = "#33b5e5";
                    folderStyle = "text-align:center; border:1px; border-style:solid; background-color: black; border-color: #33b5e5; border-radius:8px; padding:6px;";
                }

                //Extract namespaces for subpages
                ArrayList<Article> _subPages = new ArrayList<Article>();
                _subPages.add(new Article(_name,_url,-1)); //Add self as the first item.
                Elements subpageList = _doc.getElementsByClass("main-body-links");
                if (subpageList != null && !subpageList.isEmpty())
                {
                    subpageList = subpageList.first().children();
                    for (Element subpageItem : subpageList){
                        if (subpageItem.tagName().equals("li") && subpageItem.attr("class").isEmpty())
                        {
                            String articleName = subpageItem.text().replace("&nbsp;","").trim();
                            Elements articleLink = subpageItem.getElementsByTag("a");
                            if (articleLink != null && articleLink.attr("class").contains("textcolor "))
                            {
                                String articleUrl;
                                if (articleLink != null && !articleLink.isEmpty())
                                 {
                                    articleUrl = articleLink.attr("href");
                                    Log.d(LOG_TAG, "Subpage Added!\nArticle: " + articleName + ".\nURL: " + articleUrl);
                                    _subPages.add(new Article(articleName, articleUrl, -1));
                                }
                            }
                        }
                    }
                }

                //Remove unwanted elements
                wikiText.select("ins").remove();
                wikiText.select("script").remove();
                wikiText.select("table").remove();
                wikiText.select("iframe").remove();
                wikiText.select(".pathholder").remove();
                wikiText.select(".folder").attr("style","display:none");

                //Add title
                wikiText.prepend("<h1>" + _taskTitle + "</h1>");

                //Set functions
                String jsFunction = "<script type=\"text/javascript\">\n\n" +
                        "function showSpoiler(tagID){\n" +
                        " document.getElementById(tagID).style.display=\"\";\n" +
                        " document.getElementById(tagID + 'a').style.display=\"none\";}\n" +
                        "function togglefolder(folderID){\n" +
                        " var current = document.getElementById(folderID).style.display;\n" +
                        " if (current == \"none\") document.getElementById(folderID).style.display = \"\";\n" +
                        " else document.getElementById(folderID).style.display = \"none\";}\n" +
                        "var last_toggle=\"none\";\n"+
                        "function toggleAllFolders(){\n" +
                        "	var divs = document.getElementsByTagName(\"div\");\n" +
                        "	for (var i=0; i<divs.length; i++){\n"+
                        "		if (divs[i].getAttribute(\"isfolder\")==\"true\")\n"+
                        "			if(last_toggle == \"block\")\n" +
                        "				divs[i].style.display=\"none\";\n" +
                        "			else\n" +
                        "				divs[i].style.display=\"block\";}\n" +
                        "	last_toggle=(last_toggle==\"block\"?\"none\":\"block\");}\n" +
                        "function toggleinline(element){\n"+
                        "	var divElement = element.getElementsByTagName(\"div\")[0];\n"+
                        "	if (divElement.style.display == \"none\") divElement.style.display = \"\";\n" +
                        "	else divElement.style.display = \"none\";}\n"+
                        "function togglenote(element){\n"+
                        "	var divElement = document.getElementById(element);\n" +
                        "	if (divElement.style.display == \"none\") divElement.style.display = \"\";\n" +
                        "	else divElement.style.display = \"none\";}\n"+
                        "function nightMode() {\n"+
                        "   document.body.style.color=\"white\";\n" +
                        "   document.body.style.backgroundColor=\"black\";\n" +
                        "   var links = document.getElementsByTagName(\"a\");\n" +
                        "   for (var i=0;i<links.length;i++){\n" +
                        "       if (links[i].getAttribute(\"onclick\") == null)\n" +
                        "           links[i].style.color=\"#33b5e5\";\n" +
                        "       else\n" +
                        "          links[i].style.color=\"black\";\n" +
                        "   }\n" +
                        "   var folders = document.getElementsByClassName(\"folderlabel\");\n" +
                        "   for (var i=0;i<folders.length;i++) {\n" +
                        "       folders[i].style.backgroundColor=\"black\";\n" +
                        "       folders[i].style.borderColor=\"#33b5e5\";\n" +
                        "   }\n" +
                        "}   \n" +
                        "function dayMode() {\n"+
                        "   document.body.style.color=\"black\";\n" +
                        "   document.body.style.backgroundColor=\"white\";\n" +
                        "   var links = document.getElementsByTagName(\"a\");\n" +
                        "   for (var i=0;i<links.length;i++){\n" +
                        "       if (links[i].getAttribute(\"onclick\") == null)\n" +
                        "          links[i].style.color=\"#0000FF\";\n" +
                        "       else\n" +
                        "          links[i].style.color=\"white\";\n" +
                        "   }\n" +
                        "   var folders = document.getElementsByClassName(\"folderlabel\");\n" +
                        "   for (var i=0;i<folders.length;i++) {\n" +
                        "       folders[i].style.backgroundColor=\"#EEF\";\n" +
                        "       folders[i].style.borderColor=\"#BBBBC3\";\n" +
                        "   }\n" +
                        "}   \n";
                if (_mainPreferences.getBoolean("reflowMargin",true))
                {
                    jsFunction +=
                            "function reflow(){\n" +
                                    "          document.body.style.width = (window.innerWidth - 15);\n"+
                                    "}" +
                                    "</script>";
                } else {
                    jsFunction +=
                            "function reflow(){\n" +
                                    "          document.body.style.width = (window.innerWidth);\n"+
                                    "}" +
                                    "</script>";
                }


                //Replace all spoiler tags with the new internal format - IF defined so by the settings
                if (_mainPreferences.getBoolean("showSpoilers", false) == false){ //If spoilers are hidden by default
                    Elements spoilers = wikiText.select(".spoiler");
                    int i = 0;
                    for (Element spoiler : spoilers){
                        String spoilerHtml = "<a id=\"" + i + "a\" onclick=\"showSpoiler('" + i + "')\" style=\"border-bottom: 1px solid #999999; color:" + backColor + ";\">" + spoiler.text() + "</a>";
                        spoiler.before(spoilerHtml);
                        spoiler.attr("id", "" + i + "");
                        spoiler.attr("style", "border-bottom: 1px solid #999999");
                        spoiler.attr("style","display:none");
                        i++;
                    }
                } else { //If spoilers are not to be hidden by default
                    if (_mainPreferences.getBoolean("highlightSpoilers", true)) //If spoilers are to be highlighted
                    {
                        Elements spoilers = wikiText.select(".spoiler");
                        for (Element spoiler : spoilers) {
                            String spoilerHtml = "<text style=\"border-bottom: 1px solid #999999;\">" + spoiler.text() + "</text>";
                            spoiler.html(spoilerHtml);
                        }
                    }
                }

                double defaultZoom = Double.valueOf(_mainPreferences.getString("defaultZoom", "100")) / 100;

                String head = "<head>" +
                        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=" + defaultZoom + ",minimum-scale=0.1,maximum-scale=5.0\"/>\n" +
                        "<style type=\"text/css\">\n" +
                        "div.folderlabel\n" +
                        "{\n" +
                        "   text-align:center;\n" +
                        "   border:1px;\n" +
                        "   border-style:solid;\n" +
                        "   background-color: #EEF;\n" +
                        "   border-color: #BBBBC3;\n" +
                        "   border-radius:8px;\n" +
                        "   padding:6px;\n" +
                        "}\n" +
                        ".indent {\n" +
                        "  margin-left:24px;\n" +
                        "}\n"+
                        ".folder li {\n" +
                        "  margin-botton: 3px;\n" +
                        "}\n";
                boolean tabletView = _mainPreferences.getBoolean("tabletView",false);
                Log.d(LOG_TAG,"Tabletmode: " + tabletView);
                if (tabletView){
                    head+=
                            "div.quoteright {\n" +
                                    "  position:relative;\n" +
                                    "  background-color:transparent;\n" +
                                    "  width:150px;\n" +
                                    "  margin:6px;\n" +
                                    "  padding:4px;\n" +
                                    "  font-size:smaller;\n" +
                                    "  float:right;\n" +
                                    "  clear:right;\n" +
                                    "  padding-bottom:0px;\n" +
                                    "  margin-bottom:0px;\n" +
                                    "  z-index:4;\n" +
                                    "}\n"+
                                    "img.embeddedimage {\n" +
                                    "  position:relative;\n" +
                                    "  float:left;\n" +
                                    "  clear:left;\n" +
                                    "  margin:6px;\n" +
                                    "  padding:4px;\n" +
                                    "  max-width:350px;\n" +
                                    "}\n"+
                                    "div.acaptionright {\n" +
                                    "  position:relative;\n" +
                                    "  background:transparent;\n" +
//                                    "  border:1px dotted #606060;\n" +
                                    "  padding:4px;\n" +
                                    "  padding-top:0px;\n" +
                                    "  margin:6px;\n" +
                                    "  margin-top:0px;\n" +
                                    "  width:150px;\n" +
                                    "  max-width:350px;\n" +
                                    "  text-align:center;\n" +
                                    "  float:right;\n" +
                                    "  clear:right;\n" +
                                    "  z-index:4;\n" +
                                    "}\n";
                } else { //PhoneView
                    head+=
                            "div.quoteright {\n" +
                                    "  position:relative;\n" +
                                    "  background-color:transparent;\n" +
                                    "  width:150px;\n" +
                                    "  margin:6px;\n" +
                                    "  padding:4px;\n" +
                                    "  font-size:smaller;\n" +
                                    "  padding-bottom:0px;\n" +
                                    "  margin-bottom:0px;\n" +
                                    "  z-index:4;\n" +
                                    "}\n"+
                                    "img.embeddedimage {\n" +
                                    "  position:relative;\n" +
                                    "  margin:6px;\n" +
                                    "  padding:4px;\n" +
                                    "  max-width:350px;\n" +
                                    "}\n"+
                                    "div.acaptionright {\n" +
                                    "  position:relative;\n" +
                                    "  background:transparent;\n" +
//                                    "  border:1px dotted #606060;\n" +
                                    "  padding:4px;\n" +
                                    "  padding-top:0px;\n" +
                                    "  margin:6px;\n" +
                                    "  margin-top:0px;\n" +
                                    "  width:150px;\n" +
                                    "  max-width:350px;\n" +
                                    "  text-align:center;\n" +
                                    "  z-index:4;\n" +
                                    "}\n";
                }
                head+="</style>" + jsFunction + "</head>";
                String mode = "dayMode()";
                if (_mainPreferences.getBoolean("nightMode",false))
                    mode="nightMode()";
                String finalHTML = "<html>" + head + "<body onload=\"" + mode + "\"><div id=\"contentRoot\">" + wikiText.html() + "</div></body></html>";
                if (isCancelled()) return null;
                //Return final result

                String filename = "output.html";

                FileOutputStream outputStream;

                try {
                    outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                    outputStream.write(finalHTML.getBytes());
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return new Article(_name,_url,_id,finalHTML, _subPages);
            }
            catch (UnknownHostException e)
            {
                Log.e(LOG_TAG, "Connection Error!");
                _e = e;
                return null;
            }
            catch (IOException e)
            {
                Log.e(LOG_TAG, "Connection Error!");
                _e = e;
                return null;
            }
            catch (Exception e)
            {
                Log.e(LOG_TAG, "Unexpected Error in document loading!");
                e.printStackTrace();
                _e = e;
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(String... message)
        {
            try
            {
                if (this.isCancelled())
                {
                    this.cancel(true);
                    return;
                }
                View _progressScreen = _pager.getLoadingScreen(_id);
                TextView _mainText = (TextView) _progressScreen.findViewById(R.id.txtLoadingTitle);
                TextView _subText = (TextView) _progressScreen.findViewById(R.id.txtLoadingData);
                _mainText.setText(message[0]);
                _subText.setText(message[1]);
            }
            catch (NullPointerException ex)
            {
                Log.e(LOG_TAG,"Could not retreive progress screen for updating!");
                Log.e(LOG_TAG,"Cancelled: " + this.isCancelled());
                Log.e(LOG_TAG,"ID: " + _id);
                return;
            }
        }

        @Override
        protected void onPreExecute()
        {
            View _loadingScreen = _pager.getLoadingScreen(_id);
            if (_loadingScreen == null)
            {
                Log.e(LOG_TAG,"Loading screen is null!");
                this.cancel(true);
                return;
            }
            TextView _mainText = (TextView) _loadingScreen.findViewById(R.id.txtLoadingTitle);
            TextView _subText = (TextView) _loadingScreen.findViewById(R.id.txtLoadingData);
            _mainText.setText(R.string.Loading);
            _subText.setText("");
            _loadingScreen.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Article article)
        {
            if (this.isCancelled())
            {
                Log.e(LOG_TAG,"Thread has cancelled!");
                return;
            }
            View _loadingScreen = _pager.getLoadingScreen(_id);
            if (_loadingScreen == null)
            {
                Log.e(LOG_TAG,"Could not find loading screen for article, canceling load!!");
                Log.e(LOG_TAG,"ID: " + _id);
                Log.e(LOG_TAG,"Cancelled: " + this.isCancelled());
                this.cancel(true);
                return;
            }
            _loadingScreen.setVisibility(View.GONE);
            if (article == null || _e != null)
            {
                String _message = getString(R.string.articleError);
                String _title = getString(R.string.error);
                if (_e != null && (_e.getClass() == IOException.class || _e.getClass() == UnknownHostException.class))
                {
                    _title = getString(R.string.connectionError);
                    _message = getString(R.string.timeout_error_message);
                }
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(_context);
                alertDialog.setTitle(_title);
                alertDialog.setMessage(_message);
                alertDialog.setIcon(R.drawable.ic_error_light);
                alertDialog.setNegativeButton(android.R.string.ok,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Article was not parsed, error triggered!
                        removeTab(_id);
                        return;
                    }
                });
                alertDialog.setPositiveButton(R.string.retry,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getArticle(_name,_url,_id);
                        return;
                    }
                });
                alertDialog.setNeutralButton(R.string.openExternal,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeTab(_id);
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(_url)));
                        return;
                    }
                });
                alertDialog.show();
                return;
            }
            parseArticle(article, true);
        }
        public void updateIndex(int index)
        {
            Log.d(LOG_TAG,"Article Fetcherr index updated from " + _id + " to " + index + ".");
            _id = index;
        }
    }

    /**
     * An internal class for handling and redirecting links.
     *  Any link pressed in the program will be redirected to this application.
     * @author Elad Avron
     */
    public class InternalWebViewClient extends WebViewClient {
        private int _id = -1;
        boolean scaleChangedRunnablePending = false;
        public InternalWebViewClient (int id)
        {
            _id = id;
        }
        @Override
        public void onScaleChanged(final WebView webView, float oldScale, float newScale) {
            if (scaleChangedRunnablePending || (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) || !_mainPreferences.getBoolean("kitkatReflow",true)) return;
            Log.d(LOG_TAG, "Changing scale: " + oldScale + " --> " + newScale);
            webView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    webView.evaluateJavascript("reflow()", null);
                    webView.setScrollX(0);
                    scaleChangedRunnablePending = false;
                }
            }, 500);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String _name = null;
            //Check if local link
            String _u = Uri.parse(url).getHost();
            if (_u == null) //It's a relative link
            {
                _name = url;
                url = "http://tvtropes.org/pmwiki/pmwiki.php/" + url; //Alter it to a global link
            }
            else if (!_u.equals("tvtropes.org")) //If it's not a TVTropes link
            {
                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }
            if (_name == null)
                _name = parseTitle(url);
            boolean newTab = _mainPreferences.getBoolean("newTab", false);
            if (newTab)
            {
                boolean moveNow = _mainPreferences.getBoolean("moveNow",false);
                newTab(_name,url,moveNow);
            } else {
                getArticle(_name, url, _id);
            }
            return true;
        }

        @Override
        public void onReceivedError (WebView view, int errorCode, String description, String failingUrl){
            String errorMessage = errorCode + ":<br>" + description + "<br>" + failingUrl;
            Log.e(LOG_TAG, "WebBrowser Error: " + description + "(" + errorCode + ")");
            view.loadData(errorMessage, "text/html", "ISO-8859-1");
        }
    }


    public static class MyViewPager extends ViewPager
    {
        public MyViewPager(Context context) {
            super(context);
        }
        public MyViewPager(Context context, AttributeSet attributeSet)
        {
            super(context,attributeSet);
        }

        public MyWebView getWebView(int id)
        {
            if (this.getChildCount() == 0)
                return null;
            return (MyWebView) this.getChildAt(id).findViewById(R.id.web_view);
        }
        public RelativeLayout getLoadingScreen(int id)
        {
            if (this.getChildCount() == 0)
                return null;
            return (RelativeLayout) this.getChildAt(id).findViewById(R.id.loading_layout);
        }
        public ArticleFragment getVisibleFragment()
        {
            if (this.getChildCount() == 0)
                return null;
            return _fragmentAdapter.get(this.getCurrentItem());
        }
        public Article getVisibleArticle()
        {
            if (this.getChildCount() == 0)
                return null;
            return _tabListAdapter.getItem(this.getCurrentItem());
        }
    }

    /**
     * A custom WebView controller
     */
    public static class MyWebView extends WebView{

        private WebViewClient _internalWebViewClient;

        public MyWebView(Context context) {
            super(context);
        }

        public MyWebView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyWebView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public void setWebViewClient(WebViewClient client) {
            super.setWebViewClient(client);
            _internalWebViewClient = client;
        }

        public WebViewClient getWebViewClient()
        {
            return _internalWebViewClient;
        }

        @Override
        protected void onCreateContextMenu(ContextMenu menu) {
            super.onCreateContextMenu(menu);
            HitTestResult result = getHitTestResult();
            if (result.getType() == HitTestResult.SRC_ANCHOR_TYPE )
            {
                String title = parseTitle(result.getExtra());
                menu.add(0, 0, 0, result.getExtra());
                menu.add(0, 1, 0, title);
            } else {
                Log.w(LOG_TAG,"Context menu requested for unknown type.");
                return;
            }
        }
    }
}