package com.roymam.android.common;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.roymam.android.notificationswidget.R;
import com.roymam.android.notificationswidget.SettingsActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

public class BackupRestorePreferenceFragment extends PreferenceFragment
{
    public static final String NILSPLUS_BACKUP_SERVICE = "com.roymam.android.nilsplus.NPService";
    public static final String BACKUP_SETTINGS = "com.roymam.android.nils.backup_settings";
    public static final String RESTORE_SETTINGS = "com.roymam.android.nils.restore_settings";

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.backup_restore);

        findPreference("backup").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                // Backup to a file
                if (saveSharedPreferencesToFile())
                    Toast.makeText(getActivity(), getActivity().getString(R.string.backup_success) + getActivity().getExternalFilesDir(null) + "/settings.dat", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getActivity(), getActivity().getString(R.string.backup_failed), Toast.LENGTH_SHORT).show();

                // Call Backup method of NiLS+
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(SettingsActivity.NILSPLUS_PACKAGE, NILSPLUS_BACKUP_SERVICE));
                intent.setAction(BACKUP_SETTINGS);
                getActivity().startService(intent);
                return true;
            }
        });

        findPreference("restore").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                // Restore from a file
                if (loadSharedPreferencesFromFile())
                    Toast.makeText(getActivity(), getActivity().getString(R.string.restore_success), Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getActivity(), getActivity().getString(R.string.restore_failed), Toast.LENGTH_SHORT).show();

                // Call Restore method of NiLS+
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(SettingsActivity.NILSPLUS_PACKAGE, NILSPLUS_BACKUP_SERVICE));
                intent.setAction(RESTORE_SETTINGS);
                getActivity().startService(intent);
                return true;
            }
        });
    }

    private boolean saveSharedPreferencesToFile()
    {
        File dst = new File(getActivity().getExternalFilesDir(null),"settings.dat");
        boolean res = false;
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(new FileOutputStream(dst));
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            output.writeObject(pref.getAll());
            res = true;
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (output != null)
                {
                    output.flush();
                    output.close();
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        return res;
    }

    @SuppressWarnings({ "unchecked" })
    private boolean loadSharedPreferencesFromFile()
    {
        File src = new File(getActivity().getExternalFilesDir(null),"settings.dat");
        boolean res = false;
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new FileInputStream(src));
            SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
            prefEdit.clear();
            Map<String, ?> entries = (Map<String, ?>) input.readObject();
            for (Map.Entry<String, ?> entry : entries.entrySet())
            {
                Object v = entry.getValue();
                String key = entry.getKey();

                if (v instanceof Boolean)
                    prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
                else if (v instanceof Float)
                    prefEdit.putFloat(key, ((Float) v).floatValue());
                else if (v instanceof Integer)
                    prefEdit.putInt(key, ((Integer) v).intValue());
                else if (v instanceof Long)
                    prefEdit.putLong(key, ((Long) v).longValue());
                else if (v instanceof String)
                    prefEdit.putString(key, ((String) v));
            }
            prefEdit.commit();
            res = true;
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        } catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (input != null)
                {
                    input.close();
                }
            } catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        return res;
    }
}

