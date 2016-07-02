package ambious.androidtroper;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

/**
 * Created by Elad on 13/09/13.
 */
public class SettingsActivity extends AppCompatActivity {
    private final int FLAG_SETTINGS = 0;
    private final int FLAG_ABOUT = 1;
    private SharedPreferences _mainPreferences;
    ActionBar _actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) { //OnCreate of the settings activity
        super.onCreate(savedInstanceState);
        _mainPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(_mainPreferences.getBoolean("nightMode",false) ? R.style.AppDark : R.style.AppLight);
        //Get Intent//
        Intent _intent = getIntent();

        setContentView(R.layout.settings_layout);

        //Setup Actiobar//
        Toolbar myToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(myToolbar);
        _actionBar = getSupportActionBar();
        _actionBar.setDisplayHomeAsUpEnabled(true);

        if (_intent == null || _intent.getFlags() == FLAG_SETTINGS)
        {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_frame, new SettingsFragment())
                    .commit();
        } else if (_intent.getFlags() == FLAG_ABOUT)
        {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_frame, new AboutFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()){
            case (android.R.id.home):
                finish();
                return true;
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    public static class AboutFragment extends Fragment {
        private final String LOG_TAG = "AndroidTroper";
        PackageInfo packageInfo;
        private SharedPreferences _mainPreferences;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            try{
                packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                _mainPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                Boolean _nightMode = _mainPreferences.getBoolean("nightMode",false);
                // Inflate the layout for this fragment
                View returnView = inflater.inflate(R.layout.about, container, false);
                returnView.setBackgroundColor(ContextCompat.getColor(getContext(),_nightMode ? android.R.color.black : android.R.color.white));
                Display display = getActivity().getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int width = size.x;
                int height = size.y;

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeResource(getResources(), R.drawable.logo, options);
                ImageView logoView = (ImageView) returnView.findViewById(R.id.logo);
                logoView.setImageBitmap(decodeSampledBitmapFromResource(getResources(), R.drawable.logo, width, height));

                TextView _version = (TextView) returnView.findViewById(R.id.versionString);
                TextView _build = (TextView) returnView.findViewById(R.id.build);
                TextView _gitHub = (TextView) returnView.findViewById(R.id.GitHub);
                _version.setText(getString(R.string.Version) + " " + packageInfo.versionName);
                _build.setText(getString(R.string.build) + " " + packageInfo.versionCode);
                ImageButton _ccImage = (ImageButton) returnView.findViewById(R.id.ccButton);
                _gitHub.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/eladavron/AndroidTroper2")));
                    }
                });
                _ccImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://creativecommons.org/licenses/by-nc-sa/3.0/")));
                    }
                });
                TextView _ccText = (TextView) returnView.findViewById(R.id.ccText);
                TextView _jsoup = (TextView) returnView.findViewById(R.id.jsoup);
                TextView _changeLog = (TextView) returnView.findViewById(R.id.changeLog);
                String _about = LoadFile("changelog", false);
                _changeLog.setText(_about);
                _jsoup.setMovementMethod(LinkMovementMethod.getInstance());
                _ccText.setMovementMethod(LinkMovementMethod.getInstance());
                return returnView;
            }
            catch (Exception e)
            {
                Log.e(LOG_TAG, "Failed loading \"About\" screen!");
                getActivity().finish();
                return null;
            }
        }

        private String LoadFile(String fileName, boolean loadFromRawFolder) {
            //Read text from a file in Assets
            try {
                //Create a InputStream to read the file into
                InputStream iS;
                Resources resources = getResources();
                if (loadFromRawFolder)
                {

                    //get the resource id from the file name
                    int rID = getResources().getIdentifier("ambious.androidtroper:raw/" + fileName, null, null);
                    //get the file as a stream
                    iS = resources.openRawResource(rID);
                }
                else
                {
                    //get the file as a stream
                    iS = resources.getAssets().open(fileName);
                }

                //create a buffer that has the same size as the InputStream
                byte[] buffer = new byte[iS.available()];
                //read the text file as a stream, into the buffer
                iS.read(buffer);
                //create a output stream to write the buffer into
                ByteArrayOutputStream oS = new ByteArrayOutputStream();
                //write this buffer to the output stream
                oS.write(buffer);
                //Close the Input and Output streams
                oS.close();
                iS.close();

                //return the output stream as a String
                return oS.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to load changelog!");
                return null;
            }
        }
    }
    public static class SettingsFragment extends PreferenceFragmentCompat {

        private final String LOG_TAG = "AndroidTroper";
        private SharedPreferences _recentSettings;
        private SharedPreferences _mainPreferences;
        private SharedPreferences _readLaterSettings;
        private SharedPreferences _favoriteSettings;

        SearchRecentSuggestions suggestions;

        @Override
        public void onCreate(Bundle savedInstanceState) { //Oncreate of the settings fragment (internal)
            _mainPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            getActivity().setTheme(_mainPreferences.getBoolean("nightMode",false) ? R.style.AppDark : R.style.AppLight);
            super.onCreate(savedInstanceState);

               // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            //Set up toolbar

            suggestions = new SearchRecentSuggestions(getContext(),MySuggestionProvider.AUTHORITY, MySuggestionProvider.MODE);

            //Denote that the settings have been entered at least once
            //This is neccesary to overcome the issue with auto-setting tablet mode if not set manually

            SharedPreferences.Editor editor = _mainPreferences.edit();
                    editor.putBoolean("tabletModeSet",true)
                    .apply();

            //Nightmode Settings//
            Preference _nightMode = findPreference("nightMode");
            if (_nightMode == null)
            {
                Log.e(LOG_TAG,"Could not find element NightMode");
                return;
            }
            _nightMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().equals("true")) {
                        getActivity().setTheme(R.style.AppDark);
                        getActivity().recreate();
                        return true;
                    } else if (newValue.toString().equals("false")) {
                        getActivity().setTheme(R.style.AppLight);
                        getActivity().recreate();
                        return true;
                    } else {
                        Log.e("LOG_TAG", "New settings for nightmode not validated correctly. Its result: " + newValue.toString());
                        return false;
                    }
                }
            });


            //Rotation Settings//
            CheckBoxPreference _lockRotation = (CheckBoxPreference)findPreference("lockRotation");
            if (_lockRotation == null)
            {
                Log.e(LOG_TAG,"Could not find element LockRotation");
                return;
            }
            _lockRotation.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().equals("true"))
                    {
                        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                        return true;
                    }
                    else if (newValue.toString().equals("false"))
                    {
                        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                        return true;
                    }
                    else
                    {
                        Log.e(LOG_TAG,"New settings value for rotation not validated correctly. Its result: " + newValue.toString());
                        return false;
                    }
                }
            });

            //Recent Articles Current/Clear//
            _recentSettings = getActivity().getSharedPreferences("recentSettings",0);
            Preference buttonClearRecent = findPreference("clearRecent");
            String recentSummary = getString(R.string.currentInList) + " " + _recentSettings.getInt("recentCount",0);
            buttonClearRecent.setSummary(recentSummary);
            buttonClearRecent.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(final Preference arg0) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                    alertDialog.setTitle(R.string.confirmTitle);
                    alertDialog.setMessage(R.string.confirmClearRecent);
                    alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = _recentSettings.edit();
                            editor.clear();
                            editor.putInt("recentCount",0);
                            editor.apply();
                            String newSummary = getString(R.string.currentInList) + " " +  _recentSettings.getInt("recentCount",0);
                            arg0.setSummary(newSummary);
                            Toast.makeText(getContext(), R.string.clearRecentSuccess, Toast.LENGTH_SHORT).show();
                        }
                    });
                    alertDialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    });
                    alertDialog.show();
                    return true;
                }
            });

            //Zoom Default
            EditTextPreference _zoomDefault = (EditTextPreference)findPreference("defaultZoom");
            _zoomDefault.setSummary(_zoomDefault.getText() + "%");
            _zoomDefault.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString() + "%");
                    return true;
                }
            });

            //Recent Articles Maximum//
            EditTextPreference _recentMax = (EditTextPreference)findPreference("recentMax");
            _recentMax.setSummary(_recentMax.getText());
            _recentMax.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    return true;
                }
            });

            //Recent Articles Sort
            final String[] _types = getResources().getStringArray(R.array.sortTypes);
            ListPreference _RecentSortType = (ListPreference)findPreference("recentSortDefault");
            int _RecentSortValue = Integer.valueOf(_mainPreferences.getString("recentSortDefault","2"));
            _RecentSortType.setSummary(_types[_RecentSortValue]);
            _RecentSortType.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(_types[Integer.valueOf(newValue.toString())]);
                    return true;
                }});

            //Favorites Sort
            ListPreference favSortDefault = (ListPreference)findPreference("favSortDefault");
            int _FavSortValue = Integer.valueOf(_mainPreferences.getString("favSortDefault","2"));
            favSortDefault.setSummary(_types[_FavSortValue]);
            favSortDefault.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(_types[Integer.valueOf(newValue.toString())]);
                    return true;
                }
            });

            //Favorites clear
            _favoriteSettings  = getActivity().getSharedPreferences("favoriteSettings",0);
            Preference buttonFavorites = findPreference("clearFavorites");
            String favSummary = getString(R.string.favoriteCount) + " " + _favoriteSettings.getInt("favCount",0);
            buttonFavorites.setSummary(favSummary);
            buttonFavorites.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(final Preference arg0) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                    alertDialog.setTitle(R.string.confirmTitle);
                    alertDialog.setMessage(R.string.confirmClearFavorites);
                    alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = _favoriteSettings.edit();
                            editor.clear();
                            editor.putInt("favCount",0);
                            editor.apply();
                            String newSummary = getString(R.string.favoriteCount) + " " +  _favoriteSettings.getInt("favCount",0);
                            arg0.setSummary(newSummary);
                            Toast.makeText(getContext(), R.string.clearFavoritesSuccess, Toast.LENGTH_SHORT).show();
                        }
                    });
                    alertDialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    });
                    alertDialog.show();
                    return true;
                }
            });


            //Read-Later Sort
            ListPreference rlSortDefault = (ListPreference)findPreference("rlSortDefault");
            int _rlValue = Integer.valueOf(_mainPreferences.getString("rlSortDefault","2"));
            rlSortDefault.setSummary(_types[_rlValue]);
            rlSortDefault.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(_types[Integer.valueOf(newValue.toString())]);
                    return true;
                }
            });

            //Read-Later clear
            _readLaterSettings = getActivity().getSharedPreferences("readlaterSettings",0);
            Preference buttonReadLater = findPreference("clearReadLater");
            String readLaterSummary = getString(R.string.rlCount) + " " + _readLaterSettings.getInt("rlCount",0);
            buttonReadLater.setSummary(readLaterSummary);
            buttonReadLater.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(final Preference arg0) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                    alertDialog.setTitle(R.string.confirmTitle);
                    alertDialog.setMessage(R.string.confirmClearReadLater);
                    alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = _readLaterSettings.edit();
                            editor.clear();
                            editor.putInt("rlCount",0);
                            editor.apply();
                            String newSummary = getString(R.string.rlCount) + " " +  _readLaterSettings.getInt("rlCount",0);
                            arg0.setSummary(newSummary);
                            Toast.makeText(getContext(), R.string.clearReadLaterSuccess, Toast.LENGTH_SHORT).show();
                        }
                    });
                    alertDialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    });
                    alertDialog.show();
                    return true;
                }
            });



            //Search Settings
            Preference clearSuggestionBox = findPreference("clearSuggestions");
            clearSuggestionBox.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                    alertDialog.setTitle(R.string.confirmTitle);
                    alertDialog.setMessage(R.string.confirmClearSuggestions);
                    alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            suggestions.clearHistory();
                            Toast.makeText(getContext(),R.string.clearSuggestionSuccess, Toast.LENGTH_SHORT).show();
                        }
                    });
                    alertDialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    });
                    alertDialog.show();
                    return true;
                }
            });

            //Adcaned Settings//
            EditTextPreference _timeout = (EditTextPreference)findPreference("timeout");
            _timeout.setSummary(_timeout.getText());
            _timeout.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    preference.setSummary(newValue.toString());
                    return true;
                }
            });

            Preference _backup = findPreference("backupSettings");
            _backup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    performBackup();
                    return true;
                }
            });
            _backup.setSummary(getString(R.string.backupSummary) + " "+ Environment.getExternalStorageDirectory() + "/AndroidTroper/Backup/");

            Preference _restore = findPreference("restoreSettings");
            _restore.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    restoreBackup();
                    return true;
                }
            });
        }

        @Override
        public void onDestroy()
        {
            Toast.makeText(getContext(),R.string.someChanges,Toast.LENGTH_LONG).show();
            super.onDestroy();
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            
        }

        private void performBackup() {
            try {
                File sd = Environment.getExternalStorageDirectory();
                File data = new File(getActivity().getApplicationInfo().dataDir);

                File dataDir = new File (data + "/shared_prefs/");
                File outDir = new File (sd + "/AndroidTroper/Backup/");
                outDir.mkdirs();

                if (sd.canWrite()) {
                    for (File file : dataDir.listFiles()){
                        File newFile = new File(outDir, file.getName());

                        if (file.exists()) {
                            FileChannel src = new FileInputStream(file).getChannel();
                            FileChannel dst = new FileOutputStream(newFile).getChannel();
                            dst.transferFrom(src, 0, src.size());
                            src.close();
                            dst.close();
                        }
                    }
                    Toast.makeText(getContext(),getString(R.string.backupSuccess) + " " + outDir.getCanonicalPath() ,Toast.LENGTH_LONG).show();
                } else {
                    Log.e(LOG_TAG,"Failed to write to storage!");
                    Toast.makeText(getContext(),getString(R.string.failedWriteSD),Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (Exception e) {
                Log.e(LOG_TAG,"Error occurred backing up settings!");
                Toast.makeText(getContext(),getString(R.string.backupError),Toast.LENGTH_LONG).show();
            }
        }

        private void restoreBackup()
        {
            if (!doesBackupExist())
            {
                Log.e(LOG_TAG,"Tried restoring without Backupe existing!");
                Toast.makeText(getContext(),getString(R.string.backupDoesNotExist),Toast.LENGTH_LONG).show();
                return;
            }
            try {
                File sd = Environment.getExternalStorageDirectory();
                File data = new File(getActivity().getApplicationInfo().dataDir);

                File dataDir = new File (data + "/shared_prefs/");
                File backupDir = new File (sd + "/AndroidTroper/Backup/");
                dataDir.mkdirs();

                if (dataDir.canWrite()) {
                    for (File file : backupDir.listFiles()){
                        File newFile = new File(dataDir, file.getName());

                        if (file.exists()) {
                            FileChannel src = new FileInputStream(file).getChannel();
                            FileChannel dst = new FileOutputStream(newFile).getChannel();
                            dst.transferFrom(src, 0, src.size());
                            src.close();
                            dst.close();
                        }
                    }
                    Intent result = new Intent();
                    result.putExtra("restart",true);
                    getActivity().setResult(RESULT_OK,result);
                    getActivity().finish();
                } else {
                    Log.e(LOG_TAG,"Failed to write to settings!");
                    Toast.makeText(getContext(),getString(R.string.failedWriteSettings),Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (Exception e) {
                Log.e(LOG_TAG,"Error occurred restoring settings!");
                Log.e(LOG_TAG,e.getMessage()+"");
                Toast.makeText(getContext(),getString(R.string.settingsRestoreFailed),Toast.LENGTH_LONG).show();
            }

        }

        private Boolean doesBackupExist()
        {
            File backupDir = new File (Environment.getExternalStorageDirectory() + "/AndroidTroper/Backup/");
            return backupDir.exists();
        }
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}