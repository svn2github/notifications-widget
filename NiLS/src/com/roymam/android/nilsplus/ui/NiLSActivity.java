package com.roymam.android.nilsplus.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazon.inapp.purchasing.BasePurchasingObserver;
import com.amazon.inapp.purchasing.GetUserIdResponse;
import com.amazon.inapp.purchasing.Item;
import com.amazon.inapp.purchasing.ItemDataResponse;
import com.amazon.inapp.purchasing.Offset;
import com.amazon.inapp.purchasing.PurchaseResponse;
import com.amazon.inapp.purchasing.PurchaseUpdatesResponse;
import com.amazon.inapp.purchasing.PurchasingManager;
import com.amazon.inapp.purchasing.Receipt;
import com.roymam.android.common.BackupRestorePreferenceFragment;
import com.roymam.android.common.SysUtils;
import com.roymam.android.common.util.IabHelper;
import com.roymam.android.common.util.IabResult;
import com.roymam.android.common.util.Inventory;
import com.roymam.android.common.util.Purchase;
import com.roymam.android.nilsplus.activities.StartupWizardActivity;
import com.roymam.android.nilsplus.activities.WhatsNewActivity;
import com.roymam.android.nilsplus.fragments.AboutPreferencesFragment;
import com.roymam.android.notificationswidget.BuildConfig;
import com.roymam.android.notificationswidget.SettingsManager;
import com.roymam.android.nilsplus.fragments.AppearancePreferencesFragment;
import com.roymam.android.nilsplus.fragments.MainPrefsFragment;
import com.roymam.android.nilsplus.fragments.ServicePreferencesFragment;
import com.roymam.android.nilsplus.fragments.WidgetSettingsFragment;
import com.roymam.android.notificationswidget.NotificationsWidgetProvider;
import com.roymam.android.notificationswidget.R;

import java.util.Map;

import static com.roymam.android.notificationswidget.SettingsManager.*;

public class NiLSActivity extends Activity
{
    private static final int UPGRADE_NILS_MENU_POS = 9;
    private CharSequence mTitle;
    private String[] mTitles;

    private ActionBarDrawerToggle
            mDrawerToggle;
    private Fragment fragment;
    private ServiceConnection mLicenseServiceConnection = null;
    private boolean mAmazonInAppAvailable = false;
    private boolean firstTime = true;

    public void replaceFragment(Fragment fragment)
    {
        this.fragment = fragment;
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack("Fragment")
                .commit();
    }

    private class DrawerListItemAdapater extends BaseAdapter
    {
        private final Context mContext;
        private final String[] mMenuItems;
        private final TypedArray mMenuIcons;

        DrawerListItemAdapater(Context context, int menuItemsId, int menuIconsId)
        {
            mContext = context;
            mMenuItems = context.getResources().getStringArray(menuItemsId);
            mMenuIcons = context.getResources().obtainTypedArray(menuIconsId);
        }

        @Override
        public int getCount()
        {
            return mMenuItems.length;
        }

        @Override
        public Object getItem(int position)
        {
            return mMenuItems[position];
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        private class ViewHolder
        {
            ImageView iconView;
            TextView titleView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder holder = null;

            if (convertView == null)
            {
                LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = li.inflate(R.layout.drawer_list_item, parent, false);
                holder = new ViewHolder();
                holder.titleView = (TextView) convertView.findViewById(R.id.item_text);
                holder.iconView = (ImageView) convertView.findViewById(R.id.item_icon);
                convertView.setTag(holder);
            }
            else
                holder = (ViewHolder) convertView.getTag();

            convertView.setVisibility(View.VISIBLE);
            holder.iconView.setImageResource(mMenuIcons.getResourceId(position, 0));
            holder.titleView.setText(mMenuItems[position]);

            // highlight upgrade button
            if (position == UPGRADE_NILS_MENU_POS)
            {
                // check if the app wasn't already upgraded
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if (prefs.getBoolean(SettingsManager.UNLOCKED, false))
                {
                    // return an empty view - so the user won't see the "upgrade" button
                    holder.titleView.setText(null);
                    holder.iconView.setImageDrawable(null);
                    convertView.setVisibility(View.GONE);
                }
                else
                {
                    holder.titleView.setTextColor(getApplicationContext().getResources().getColor(android.R.color.holo_green_dark));
                    holder.titleView.setTypeface(Typeface.DEFAULT_BOLD);
                }
            }
            else
            {
                holder.titleView.setTextColor(getApplicationContext().getResources().getColor(android.R.color.white));
                holder.titleView.setTypeface(Typeface.DEFAULT);
            }
            return convertView;
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id)
        {
            selectItem(position);
        }
    }

    @Override
    public void setTitle(CharSequence title)
    {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // show welcome wizard if it's first run
        int installedVer = prefs.getInt("installed_version", -1);
        boolean npSuggested = prefs.getBoolean("np_suggested", false);
        Log.d("NiLS", "installedVer:"+installedVer);

        if (installedVer >= 0) firstTime = false;

        if (installedVer < 400 && installedVer >= 0 && !npSuggested) // if user upgraded from v1.4 or lower
        {
            // disable further panel suggestions
            prefs.edit().putBoolean("np_suggested", true).commit();

            boolean nilsfpInstalled = false;
            try {
                getPackageManager().getApplicationInfo("com.roymam.android.nilsplus", 0);
                nilsfpInstalled = true;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            if (!nilsfpInstalled && isWidgetPlaced())
            {
                // suggest user to enable notifications panel
                Intent intent = new Intent(this, StartupWizardActivity.class);
                intent.putExtra(StartupWizardActivity.EXTRA_SUGGEST_NOTIFICATIONS_PANEL, true);
                startActivity(intent);
                finish();
                return;
            }
            else
            {
                // the widget is not used or fp is installed so show the welcome screen
                showWelcomeTutorial();
                return;
            }
        }
        else // any other upgrade
        {
            if (showWhatsNew())
                return;
        }

        setContentView(R.layout.activity_main);

        mTitles = getResources().getStringArray(R.array.prefs_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new DrawerListItemAdapater(this, R.array.prefs_array, R.array.prefs_icons_array));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // set event for open/close drawer
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_navigation_drawer, R.string.drawer_open, R.string.drawer_close)
        {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view)
            {
                super.onDrawerClosed(view);
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to `onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle(R.string.title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        if (savedInstanceState == null)
        {
            selectItem(0);
            mDrawerList.setSelection(0);
            mTitle = mTitles[0];
        }
        else
        {
            fragment = getFragmentManager().getFragment(savedInstanceState, "fragment");
        }

        initIAP();
   }

    private void showWelcomeTutorial()
    {
        Intent intent = new Intent(this, StartupWizardActivity.class);
        intent.putExtra(StartupWizardActivity.EXTRA_FIRST_TIME, true);
        startActivity(intent);
        finish();
    }

    public static int getCurrentVersion(Context context)
    {
        // get current version
        try
        {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e)
        {
            Log.wtf("NiLS", "cannot find version number");
            e.printStackTrace();
            return 0;
        }
    }

    private boolean showWhatsNew()
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int installedVer = prefs.getInt("installed_version", -1);
        int currentVer = getCurrentVersion(this);

        if (currentVer > installedVer && installedVer != -1) {
            startActivity(new Intent(this, WhatsNewActivity.class));
        }
        else if (installedVer == -1) {
            // if this the first installation - store version number and show welcome tutorial
            prefs.edit().putInt("installed_version", currentVer).commit();
            showWelcomeTutorial();
            return true;
        }
        return false;
    }

    private static final int APPEARANCE_PAGE_INDEX = 2;
    private static final int WIDGET_PAGE_INDEX = 4;

    /** Swaps fragments in the main content view */
    private void selectItem(int position)
    {
        // set fragment title
        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getActionBar().setDisplayShowTitleEnabled(true);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        setTitle(mTitles[position]);

        // Create a new fragment and specify the planet to show based on position
        switch (position)
        {
            case 0:
                fragment = new MainPrefsFragment();
                break;
            case 1:
                fragment = new PrefsGeneralFragment();
                break;
            case 2:
                fragment = new AppearancePreferencesFragment();
                getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                getActionBar().setDisplayShowTitleEnabled(false);
                break;
            case 3:
                fragment = new ServicePreferencesFragment();
                break;
            case 4:
                if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(WIDGET_PRESENT, false) || isWidgetPlaced())
                {
                    fragment = new WidgetSettingsFragment();
                }
                else
                {
                    fragment = new HowToAddWidgetFragment();
                }
                break;
            case 5:
                fragment = new PrefsAppSpecificFragment();
                break;
            case 6:
                fragment = new BackupRestorePreferenceFragment();
                break;
            case 7:
                fragment = new PrefsContactFragment();
                break;
            case 8:
                fragment = new AboutPreferencesFragment();
                break;
            case 9:
                requestUnlockApp();
                return;
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    private boolean isWidgetPlaced()
    {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this, NotificationsWidgetProvider.class);
        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
        return (widgetIds.length > 0);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        if (fragment != null)
        {
            if (!(fragment instanceof PrefsPersistentNotificationsFragment))
                getFragmentManager().putFragment(outState, "fragment", fragment);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        if (mDrawerLayout != null) {
            // If the nav drawer is open, hide action items related to the content view
            boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);

            menu.findItem(R.id.show_welcome).setVisible(!drawerOpen);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (!firstTime) {
            boolean isServiceRunning = SysUtils.isServiceRunning(getApplicationContext());
            if (!isServiceRunning) {
                Intent intent = new Intent(this, StartupWizardActivity.class);
                startActivity(intent);
                finish();
            }
        }
        if (mAmazonInAppAvailable)
            PurchasingManager.initiateGetUserIdRequest();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null)
            mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null)
            mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle != null) {
            if (mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }

            // Handle other action bar items...
            switch (item.getItemId()) {
                case android.R.id.home:
                    // app icon in action bar clicked; go home
                    getFragmentManager().popBackStack();
                    if (getFragmentManager().getBackStackEntryCount() == 1)
                        mDrawerToggle.setDrawerIndicatorEnabled(true);
                    return true;
                case R.id.show_welcome:
                    showWelcomeTutorial();
                    return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /** IAP Stuff **/
    private static final String SKU_UPGRADE = "com.roymam.android.nils.unlock_all";
    private IabHelper mHelper;
    private boolean mIAPAvailable;

    private void showDialog(String title, String message)
    {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // continue
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private boolean isPackageInstalled(String packagename)
    {
        PackageManager pm = getApplicationContext().getPackageManager();
        try
        {
            pm.getPackageInfo(packagename, PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException e)
        {
            return false;
        }
    }

    private void initIAP()
    {
        boolean isAmazonBuild = BuildConfig.FLAVOR.equals("amazon");
        boolean isDebugBuild = BuildConfig.BUILD_TYPE.equals("debug");

        if (isDebugBuild)
        {
            // debug build - there is no need to initilize in-app purchase mechanism
            return;
        }
        else if (isAmazonBuild)
        {
            PurchasingManager.registerObserver(new BasePurchasingObserver(getApplicationContext()) {
                public String currentUserID;

                @Override
                public void onSdkAvailable(boolean isSandboxMode) {
                    Log.d("NiLS", "onSdkAvailable isSandboxMode:"+isSandboxMode);
                    mAmazonInAppAvailable = true;
                }

                @Override
                public void onGetUserIdResponse(GetUserIdResponse response) {
                    if (response.getUserIdRequestStatus() == GetUserIdResponse.GetUserIdRequestStatus.SUCCESSFUL) {
                        currentUserID = response.getUserId();
                        Log.d("NiLS", "onGetUserIdResponse success:"+currentUserID);
                        PurchasingManager.initiatePurchaseUpdatesRequest(Offset.BEGINNING);
                    }
                    else
                    {
                        Log.d("NiLS", "onGetUserIdResponse failed");
                    }
                }

                @Override
                public void onPurchaseResponse(PurchaseResponse response) {
                            final PurchaseResponse.PurchaseRequestStatus status = response.getPurchaseRequestStatus();
                            Log.d("NiLS", "onPurchaseResponse status:" + status.name());
                            if (status == PurchaseResponse.PurchaseRequestStatus.SUCCESSFUL ||
                                status == PurchaseResponse.PurchaseRequestStatus.ALREADY_ENTITLED)
                                {
                                    Receipt receipt = response.getReceipt();
                                    Item.ItemType itemType = receipt.getItemType();
                                    String sku = receipt.getSku();
                                    String purchaseToken = receipt.getPurchaseToken();

                                    if (sku.equals(SKU_UPGRADE))
                                        upgradeNow();
                                }
                            else if (status == PurchaseResponse.PurchaseRequestStatus.FAILED)
                            {
                                // unknown error - show dialog message
                                showDialog(getString(R.string.unlock_all_features), getString(R.string.upgrade_failed_message)+"\n"+status.toString());
                            }
                }

                @Override
                public void onItemDataResponse(ItemDataResponse itemDataResponse) {

                }

                @Override
                public void onPurchaseUpdatesResponse(PurchaseUpdatesResponse response) {
                    switch (response.getPurchaseUpdatesRequestStatus()) {
                        case SUCCESSFUL:
                            // Check for revoked SKUs
                            for (final String sku : response.getRevokedSkus()) {
                                Log.v("NiLS", "Revoked Sku:" + sku);
                                if (sku.equals(SKU_UPGRADE)) {
                                    Log.d("NiLS", "onPurchaseUpdatesResponse revoke found. downgrading");
                                    downgradeNow();
                                }
                            }

                            // Process receipts
                            for (final Receipt receipt : response.getReceipts()) {
                                switch (receipt.getItemType()) {
                                    case ENTITLED: // Re-entitle the customer
                                        if (receipt.getSku().equals(SKU_UPGRADE)) {
                                            Log.d("NiLS", "onPurchaseUpdatesResponse entitled found. upgrading");
                                            upgradeNow();
                                        }
                                        break;
                                }
                            }
                            break;

                        case FAILED:
                            Log.d("NiLS", "onPurchaseUpdatesResponse failed");
                            // Provide the user access to any previously persisted entitlements.
                            break;
                    }
                }
            });
        }
        else {
            // init google play billing
            String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAi5Qtf07QZPyzuTa0H1M8Uiz+vHPa3f491xDzkeaCYmnGU6nN8simZ6/TXdxd6NRqjkafM8p/HuDku9nNApl4R3NzpTg+2Y/ifX7nkXO/o7AZyN3PArJ/oiATNjXQGHn5RzDykaKu3JZXa7+Yin3L8zCNzymP0W3SCk0i4AMFBPkMXaj7SwNsmmrn2hNaNPVImfFtdIgUvP5DqJ2nzAE5fyAvj3+e+BdhqreDjmFEhwOhRUm1Cnz2ZjzsnQ/qcwOlPYcHRfzTkra5aXwfUKb5h4YxMPIVhtDTCr42bVvowBXF91TfCJIpPsuPKxrf15+PF/jJyMUKkJWc9KwHaRB0FwIDAQAB";

            // compute your public key and store it in base64EncodedPublicKey
            mHelper = new IabHelper(this, base64EncodedPublicKey);
            mIAPAvailable = false;
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess()) {
                        // Oh noes, there was a problem.
                        Log.d("NiLS", "Problem setting up In-app Billing: " + result);
                    } else {
                        // Hooray, IAB is fully set up!
                        mIAPAvailable = true;
                        mHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                            @Override
                            public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                                if (result.isFailure()) {
                                    // handle error
                                    Log.d("NiLS", "IAP Failed:" + result.getResponse());
                                } else {
                                    // does the user have the premium upgrade?
                                    if (inv.hasPurchase(SKU_UPGRADE) && inv.getPurchase(SKU_UPGRADE).getPurchaseState() == 0) {
                                        upgradeNow();
                                    } else {
                                        // check if NiLS Unlocker is installed
                                        if (isPackageInstalled("com.roymam.android.nilsplus")) {
                                            checkNiLSPlusLicense();
                                        } else {
                                            // if not - downgrade immediate
                                            downgradeNow();
                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    /** Command to the service to request a license */
    static final int MSG_REQUEST_LICENSE = 1;
    static final int MSG_LICENSED = 2;
    static final int MSG_UNLICENSED = 3;
    private Messenger mService;

    final Messenger mMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // import settings if not already imported
            importNiLSFPPreferences();

            // upgrade or downgrade the app according to NiLS Plus response
            switch (msg.what) {
                case MSG_LICENSED:
                    upgradeNow();
                    break;
                case MSG_UNLICENSED:
                    downgradeNow();
                    break;
            }
        }
    });

    private void checkNiLSPlusLicense()
    {
        // if so - request a license
        Intent licenseService = new Intent();
        licenseService.setComponent(new ComponentName("com.roymam.android.nilsplus", "com.roymam.android.nilsplus.LicenseService"));
        mLicenseServiceConnection = new ServiceConnection()
        {
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                mService = new Messenger(service);
                try {
                    // request a license from NiLSPlus app
                    Message msg = Message.obtain(null, MSG_REQUEST_LICENSE);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e)
                {
                }
            }

            public void onServiceDisconnected(ComponentName className)
            {
                mService = null;
            }
        };
        try
        {
            bindService(licenseService, mLicenseServiceConnection, Context.BIND_AUTO_CREATE);
        }
        catch(Exception exp)
        {
            Log.d("NiLS","cannot communicate with NiLS Unlocker");
            exp.printStackTrace();
        }
    }

    private void importNiLSFPPreferences()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (!prefs.getBoolean("prefs_imported", false))
        {
            Context nilsfpCtx = null;
            try
            {
                nilsfpCtx = createPackageContext("com.roymam.android.nilsplus", 0);
                SharedPreferences nilsfpPrefs = nilsfpCtx.getSharedPreferences("shared_preferences", Context.MODE_WORLD_READABLE);

                // copy global_settings
                Map<String, ?> map = nilsfpPrefs.getAll();
                for (String key : map.keySet())
                {
                    Object x = map.get(key);
                    if (x instanceof Boolean)
                        prefs.edit().putBoolean(key, (Boolean) x).commit();
                    else if (x instanceof Integer)
                        prefs.edit().putInt(key, (Integer) x).commit();
                    else if (x instanceof Float)
                        prefs.edit().putFloat(key, (Float) x).commit();
                    else if (x instanceof Long)
                        prefs.edit().putLong(key, (Long) x).commit();
                    else if (x instanceof String)
                        prefs.edit().putString(key, (String) x).commit();
                }
                prefs.edit().putBoolean("prefs_imported", true).commit();

                // handling old lock screen detection values
                if (prefs.getString(SettingsManager.LOCKSCREEN_APP, SettingsManager.DEFAULT_LOCKSCREEN_APP).equals("auto")) {
                    prefs.edit().putString(SettingsManager.LOCKSCREEN_APP,
                            prefs.getString("lockscreenapp_auto", SettingsManager.DEFAULT_LOCKSCREEN_APP)).commit();
                }
                if (prefs.getString(SettingsManager.LOCKSCREEN_APP, SettingsManager.DEFAULT_LOCKSCREEN_APP).equals("android")) {
                    prefs.edit().putString(SettingsManager.LOCKSCREEN_APP, SettingsManager.DEFAULT_LOCKSCREEN_APP).commit();
                }

                Toast.makeText(getApplicationContext(), "Preferences and license information has been imported successfully from NiLS Floating Panel", Toast.LENGTH_LONG).show();
            }
            catch (PackageManager.NameNotFoundException e)
            {
                // NiLS FP not available - do not do anything
            }
        }
    }

    public void requestUnlockApp()
    {
        boolean isDebugBuild = BuildConfig.BUILD_TYPE.equals("debug");

        if (isDebugBuild)
        {
            upgradeNow();
        }
        else if (mAmazonInAppAvailable)
        {
            PurchasingManager.initiatePurchaseRequest(SKU_UPGRADE);
        }
        else if (mIAPAvailable && mHelper != null)
        {
            mHelper.launchPurchaseFlow(this, SKU_UPGRADE, 10001, new IabHelper.OnIabPurchaseFinishedListener()
            {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info)
                {
                    if (result.isFailure())
                    {
                        if (result.getResponse() == 7)
                            // already owned
                            upgradeNow();
                        else if (result.getResponse() == -1005)
                        {
                            // user canceled - do nothing
                        }
                        else
                        {
                            // unknown error - show dialog message
                            showDialog(getString(R.string.unlock_all_features), getString(R.string.upgrade_failed_message)+"\n"+result);
                        }
                    }
                    else if (info.getSku().equals(SKU_UPGRADE))
                    {
                        upgradeNow();
                    }
                }
            }, "");
        }
        else
        {
            showDialog(getString(R.string.unlock_all_features), getString(R.string.upgrade_failed_message)+"\n"+"Google Play in-app billing is not available.");
        }
    }

    private void upgradeNow()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // save the upgrade state and update the UI
        if  (!prefs.getBoolean(SettingsManager.UNLOCKED, false))
        {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(SettingsManager.UNLOCKED, true).commit();
            recreate();
        }
    }

    private void downgradeNow()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // save the upgrade state and update the UI
        if  (prefs.getBoolean(SettingsManager.UNLOCKED, false))
        {
            prefs.edit().putBoolean(SettingsManager.UNLOCKED, false).commit();
            recreate();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d("NiLS", "onActivityResult(" + requestCode + "," + resultCode + "," + data);

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data))
        {
            // not handled, so handle it ourselves
            super.onActivityResult(requestCode, resultCode, data);
        }
        else
        {
            Log.d("NilS", "onActivityResult handled by IABUtil.");
        }
    }

    public void onClick(View v)
    {
        if (fragment != null && fragment instanceof ViewClickable)
        {
            ((ViewClickable) fragment).onClick(v);
        }
    }

    public ActionBarDrawerToggle getDrawerToggle()
    {
        return mDrawerToggle;
    }

    @Override
    protected void onDestroy()
    {
        if (mHelper != null) mHelper.dispose();
        if (mLicenseServiceConnection != null) unbindService(mLicenseServiceConnection);
        super.onDestroy();
    }

}