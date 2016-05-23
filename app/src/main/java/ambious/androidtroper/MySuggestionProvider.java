package ambious.androidtroper;

import android.content.SearchRecentSuggestionsProvider;

/**
 * A provider for the 'recent searches' suggestions
 */
public class MySuggestionProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "ambious.androidtroper.MySuggestionProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public MySuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}
