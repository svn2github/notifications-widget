package com.roymam.android.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.roymam.android.nilsplus.CardPreferenceFragment;
import com.roymam.android.notificationswidget.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

public class BackupRestorePreferenceFragment extends CardPreferenceFragment
{
    private static String getBackupFilename(Context context)
    {
        return context.getExternalFilesDir(null) + "/settings.dat";
    }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the global_settings from an XML resource
        addPreferencesFromResource(R.xml.backup_restore);

        updateBackupInfo();

        findPreference("backup").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                // Backup to a file
                if (saveSharedPreferencesToFile(getActivity()))
                    Toast.makeText(getActivity(), getActivity().getString(R.string.backup_success) + getActivity().getExternalFilesDir(null) + "/settings.dat", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getActivity(), getActivity().getString(R.string.backup_failed), Toast.LENGTH_SHORT).show();
                updateBackupInfo();
                return true;
            }
        });

        findPreference("restore").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                // Restore from a file
                if (loadSharedPreferencesFromFile(getActivity()))
                    Toast.makeText(getActivity(), getActivity().getString(R.string.restore_success), Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getActivity(), getActivity().getString(R.string.restore_failed), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void updateBackupInfo()
    {
        String filename = getBackupFilename(getActivity());
        File f = new File(filename);

        if (f.exists())
        {
            //Time of when the file was last modified in microseconds
            Long lastModified = f.lastModified();
            findPreference("last_backup").setSummary(DateFormat.format("yyyy-MM-dd kk:mm", lastModified));
        }
        else
        {
            findPreference("last_backup").setSummary(R.string.not_available);
        }

        findPreference("backup_location").setSummary(filename);
    }

    public static boolean saveSharedPreferencesToFile(Context context)
    {
        File dst = new File(context.getExternalFilesDir(null),"settings.dat");
        boolean res = false;
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(new FileOutputStream(dst));
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
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
    public static boolean loadSharedPreferencesFromFile(Context context)
    {
        File src = new File(context.getExternalFilesDir(null),"settings.dat");
        boolean res = false;
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new FileInputStream(src));
            SharedPreferences.Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
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

