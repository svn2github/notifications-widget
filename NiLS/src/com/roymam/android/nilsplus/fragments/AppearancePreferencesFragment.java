package com.roymam.android.nilsplus.fragments;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.support.v7.graphics.Palette;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.TextView;

import com.roymam.android.common.BitmapUtils;
import com.roymam.android.common.ListPreferenceChangeListener;
import com.roymam.android.notificationswidget.NotificationData;
import com.roymam.android.notificationswidget.NotificationsService;
import com.roymam.android.notificationswidget.SettingsManager;
import com.roymam.android.nilsplus.ui.NiLSActivity;
import com.roymam.android.nilsplus.ui.NotificationAdapter;
import com.roymam.android.nilsplus.ui.theme.Theme;
import com.roymam.android.nilsplus.ui.theme.ThemeInfo;
import com.roymam.android.nilsplus.ui.theme.ThemeManager;
import com.roymam.android.notificationswidget.R;

import net.margaritov.preference.colorpicker.AlphaPatternDrawable;

import java.util.List;

public class AppearancePreferencesFragment extends NiLSPreferenceFragment implements Preference.OnPreferenceChangeListener, ActionBar.OnNavigationListener {
    private View mPreviewView;
    private Theme mTheme;
    private NotificationData mPreviewNotificationItem;
    private Handler mHandler;
    private CharSequence[] mThemes;
    private int mLastSelectedTheme = -1;
    private Context context;
    private ViewGroup mPreviewCard;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        context = getActivity();

        View v = inflater.inflate(R.layout.appearance_settings_view, null);
        ViewGroup previewContainer = (ViewGroup) v.findViewById(R.id.preview_container);
        mPreviewCard = new ScrollView(getActivity());
        createPreviewView();
        mPreviewCard.setPadding((int) getResources().getDimension(R.dimen.card_padding), (int) getResources().getDimension(R.dimen.card_padding), (int) getResources().getDimension(R.dimen.card_padding), 0);
        mPreviewCard.setBackgroundResource(R.drawable.card_background);
        previewContainer.addView(mPreviewCard, ViewGroup.LayoutParams.MATCH_PARENT, (int) getResources().getDimension(R.dimen.preview_height));
        Drawable gridbg = new AlphaPatternDrawable((int) (5 * getActivity().getResources().getDisplayMetrics().density));
        gridbg.setAlpha(64);
        mPreviewCard.setBackgroundDrawable(gridbg);
        mPreviewNotificationItem = new NotificationData();
        mPreviewNotificationItem.setIcon(((BitmapDrawable) getResources().getDrawable(R.drawable.ic_launcher)).getBitmap());
        mPreviewNotificationItem.setAppIcon(((BitmapDrawable) getResources().getDrawable(R.drawable.ic_launcher)).getBitmap());
        mPreviewNotificationItem.setTitle(getResources().getString(R.string.preview_title));
        mPreviewNotificationItem.setText(getResources().getString(R.string.preview_text));
        mPreviewNotificationItem.largeIcon = mPreviewNotificationItem.getIcon();
        Palette p = Palette.generate(((BitmapDrawable) getResources().getDrawable(R.drawable.ic_launcher)).getBitmap());
        if (p != null && p.getVibrantColor() != null)
            mPreviewNotificationItem.appColor = p.getVibrantColor().getRgb();

        updatePreview(false, true);

       return v;
    }

    private void createPreviewView()
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (mTheme != null && mTheme.notificationLayout != null)
            mPreviewView = inflater.inflate(mTheme.notificationLayout, null, false);
        else
            mPreviewView = inflater.inflate(R.layout.notification_row, null, false);

        mPreviewCard.removeAllViews();
        mPreviewCard.addView(mPreviewView);
    }

    private void updatePreview(final boolean reloadPrefs, boolean immediate)
    {
        if (immediate)
        {
            updatePreviewNow(reloadPrefs);
        }
        else
        {
            mHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    updatePreviewNow(reloadPrefs);
                }
            });
        }
    }

    private void updatePreviewNow(boolean reloadPrefs)
    {
        mPreviewView.setTag(null);
        NotificationAdapter.applySettingsToView(getActivity(), mPreviewView, mPreviewNotificationItem, 0, mTheme, true);
        if (reloadPrefs)
        {
            try
            {
                setPreferenceScreen(null);
                // refresh global_settings screen
                loadPreferences();
            } catch (Exception exp)
            {
                getActivity().recreate();
            }
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        loadPreferences();
        prepareThemes();
    }

    @Override
    public void onPause()
    {
        super.onPause();

        // ask notifications panel to redraw itself
        if (NotificationsService.getSharedInstance() != null)
            NotificationsService.getSharedInstance().refreshLayout(true);
    }

    private void loadPreferences()
    {
        mHandler = new Handler();
        // Load the global_settings from an XML resource
        addPreferencesFromResource(R.xml.appearance_preferences);

        // unlock premium features
        unlockFeatures();

        // set on change listener for all of the global_settings
        PreferenceGroup preferences = getPreferenceScreen();
        for(int i=0; i<preferences.getPreferenceCount(); i++)
        {
            Preference pref = preferences.getPreference(i);
            if (pref instanceof  PreferenceGroup)
                for (int j=0; j<((PreferenceGroup) pref).getPreferenceCount(); j++)
                {
                    Preference innerPref = ((PreferenceGroup) pref).getPreference(j);
                    innerPref.setOnPreferenceChangeListener(this);
                }
            else
                pref.setOnPreferenceChangeListener(this);
        }

        // load current theme
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String currTheme = prefs.getString(SettingsManager.THEME, SettingsManager.DEFAULT_THEME);

        if (!currTheme.equals(SettingsManager.DEFAULT_THEME))
            mTheme = ThemeManager.getInstance(getActivity()).loadTheme(currTheme);
        else
            mTheme = Theme.getDefault(getActivity());

        if (mTheme != null && mTheme.background != null)
            ((PreferenceGroup) getPreferenceScreen().findPreference("colors_category")).removePreference(findPreference(SettingsManager.MAIN_BG_COLOR));
        if (mTheme == null ||
            (mTheme.background == null && mTheme.previewBG == null && mTheme.textBG == null && mTheme.previewTextBG == null))
            ((PreferenceGroup) getPreferenceScreen().findPreference("colors_category")).removePreference(findPreference(SettingsManager.MAIN_BG_OPACITY));

    }

    private void prepareThemes()
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // load theme list
        CharSequence[] entries = getResources().getStringArray(R.array.themes_entries);
        CharSequence[] values = getResources().getStringArray(R.array.themes_values);
        String currTheme = prefs.getString(SettingsManager.THEME, SettingsManager.DEFAULT_THEME);

        // load available themes as entries in this list
        List<ThemeInfo> themes = ThemeManager.getInstance(getActivity()).getAvailableThemes();
        CharSequence[] newEntries = new CharSequence[entries.length+themes.size()];
        mThemes = new CharSequence[values.length+themes.size()];
        newEntries[0] = entries[0];
        mThemes[0] = values[0];
        int i = 1;
        int currThemeIndex = 0;
        for (ThemeInfo theme : themes)
        {
            newEntries[i] = theme.title;
            mThemes[i] = theme.packageName;
            if (theme.packageName.equals(currTheme))
                currThemeIndex = i;
            i++;
        }

        for(;i<newEntries.length;i++)
        {
            newEntries[i]=entries[i-themes.size()];
            mThemes[i]=values[i-themes.size()];
        }

        if (!prefs.getBoolean(SettingsManager.UNLOCKED, false))
            newEntries[i-1] = newEntries[i-1] + " (Premium)";

        ArrayAdapter<CharSequence> list = new ArrayAdapter<CharSequence> (getActivity(), R.layout.spinner_theme_select, android.R.id.text1, newEntries);
        list.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        getActivity().getActionBar().setListNavigationCallbacks(list, this);

        // select current theme
        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getActivity().getActionBar().setSelectedNavigationItem(currThemeIndex);
    }

    private void setMaxLines(int maxLines)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        TextView description = (TextView) mPreviewView.findViewById(R.id.notification_text);
        if (mTheme != null && mTheme.notificationLayout != null)
            description = (TextView) mPreviewView.findViewById(mTheme.customLayoutIdMap.get("notification_text"));

        if (prefs.getBoolean(SettingsManager.FIT_HEIGHT_TO_CONTENT, SettingsManager.DEFAULT_FIT_HEIGHT_TO_CONTENT))
            description.setMaxLines(maxLines);
        else
            description.setLines(maxLines);

        if (maxLines == -1)
        {
            description.setMaxLines(Integer.MAX_VALUE);
            description.setLines(1);
        }

        // hide it if zero
        if (maxLines == 0) description.setVisibility(View.GONE);
        else description.setVisibility(View.VISIBLE);

        //updatePreview(false, true);
    }

    private void setPreferenceSummaryFromValue(ListPreference preference, String value)
    {
        CharSequence[] entries = preference.getEntries();
        CharSequence[] values = preference.getEntryValues();

        for(int i=0; i<entries.length; i++)
        {
            if (values[i].equals(value))
                preference.setSummary(entries[i]);
        }
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue)
    {
        boolean reload = false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (preference instanceof  ListPreference)
        {
            ListPreferenceChangeListener listener = new ListPreferenceChangeListener(null, ((ListPreference) preference).getEntries(), ((ListPreference) preference).getEntryValues());
            listener.onPreferenceChange(preference, newValue);
        }

        if (preference.getKey().equals(SettingsManager.MAIN_BG_COLOR) ||
            preference.getKey().equals(SettingsManager.ICON_BG_COLOR) ||
            preference.getKey().equals(SettingsManager.PRIMARY_TEXT_COLOR)  ||
            preference.getKey().equals(SettingsManager.SECONDARY_TEXT_COLOR))
        {
            //reload = true;

            // apply background color also on alternate colors
            if (preference.getKey().equals(SettingsManager.MAIN_BG_COLOR))
                prefs.edit().putInt(SettingsManager.ALT_MAIN_BG_COLOR, (Integer) newValue).commit();
            if (preference.getKey().equals(SettingsManager.ICON_BG_COLOR))
                prefs.edit().putInt(SettingsManager.ALT_ICON_BG_COLOR, (Integer) newValue).commit();
        }
        else if (preference.getKey().equals(SettingsManager.MAX_TEXT_LINES))
        {
            // set max lines for description field
            int maxLines = Integer.parseInt((String) newValue);
            setMaxLines(maxLines);
            return true;
        }
        else if (preference.getKey().equals(SettingsManager.VERTICAL_ALIGNMENT))
        {
            // recreate the view
            Context context = getActivity().getApplicationContext();
            Intent serviceIntent = new Intent(context, NotificationsService.class);
            context.stopService(serviceIntent);
            context.startService(serviceIntent);
            return true;
        };

        // update preview after change ends
        updatePreview(reload, false);
        return true;
    }

    public void setTheme(String newTheme)
    {
        if (!newTheme.equals(SettingsManager.DEFAULT_THEME))
            mTheme = ThemeManager.getInstance(getActivity()).loadTheme(newTheme);
        else
            mTheme = Theme.getDefault(getActivity());

        // update colors from theme
        getPreferenceScreen().getEditor().putInt(SettingsManager.PRIMARY_TEXT_COLOR, mTheme.titleColor).
                putInt(SettingsManager.SECONDARY_TEXT_COLOR, mTheme.textColor).
                putInt(SettingsManager.MAIN_BG_COLOR, mTheme.bgColor).
                putInt(SettingsManager.ICON_BG_COLOR, mTheme.iconBGColor).
                putInt(SettingsManager.ALT_MAIN_BG_COLOR, mTheme.altBgColor).
                putInt(SettingsManager.ALT_ICON_BG_COLOR, mTheme.altIconBGColor).
                putInt(SettingsManager.ICON_SIZE, BitmapUtils.pxToDp((int) mTheme.iconSize)).
                putInt(SettingsManager.TITLE_FONT_SIZE, BitmapUtils.pxToSp((int) mTheme.titleFontSize)).
                putInt(SettingsManager.TEXT_FONT_SIZE, BitmapUtils.pxToSp((int) mTheme.textFontSize)).
                                         putString(SettingsManager.THEME, newTheme).commit();

        createPreviewView();

        // update preview
        updatePreview(true, true);
        //loadPreferences();
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId)
    {
        ((TextView) getActivity().findViewById(R.id.theme_desc)).setText(R.string.notification_panel);

        // do not change theme when selected in the first time (it will reset appearance settings if so)
        if (mLastSelectedTheme > -1)
        {
            String newTheme = mThemes[itemPosition].toString();

            if (newTheme.equals("more"))
            {
                // go to the play store and look for more
                String playstoreUrl = "market://search?q=\"NiLS Theme\"&c=apps";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(playstoreUrl));
                startActivity(browserIntent);
                getActivity().getActionBar().setSelectedNavigationItem(mLastSelectedTheme);
                return false;
            }
            else
            {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

                if (prefs.getBoolean(SettingsManager.UNLOCKED, false))
                {
                    setTheme(newTheme);
                }
                else
                {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.unlock_all_features)
                            .setMessage(R.string.unlock_all_features_dialog)
                            .setPositiveButton(R.string.purchase, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    ((NiLSActivity) getActivity()).requestUnlockApp();
                                }
                            })
                            .setNegativeButton(R.string.later, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    getActivity().getActionBar().setSelectedNavigationItem(mLastSelectedTheme);
                    return false;
                }
            }
        }
        mLastSelectedTheme = itemPosition;
        return true;

    }
}
