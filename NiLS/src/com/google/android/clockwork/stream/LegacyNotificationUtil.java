package com.google.android.clockwork.stream;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;

import java.util.Arrays;
import java.util.Iterator;

public class LegacyNotificationUtil
{
    public static void addRemoteInputResultsToIntentForGmail(Intent paramIntent, Bundle paramBundle)
    {
        Bundle localBundle = new Bundle();
        Iterator localIterator = paramBundle.keySet().iterator();
        while (localIterator.hasNext())
        {
            String str = (String)localIterator.next();
            Object localObject = paramBundle.get(str);
            if ((localObject instanceof CharSequence)) {
                localBundle.putString(str, localObject.toString());
            }
        }
        paramIntent.setClipData(ClipData.newIntent("com.google.android.wearable.extras", new Intent().putExtras(localBundle)));
    }

    public static String getGroup(Notification paramNotification)
    {
        String str = NotificationCompat.getGroup(paramNotification);
        if (str != null) {
            return str;
        }
        Bundle localBundle = NotificationCompat.getExtras(paramNotification);
        if (localBundle != null) {
            return localBundle.getString("android.support.wearable.groupKey");
        }
        return null;
    }

    public static String getSortKey(Notification paramNotification)
    {
        String str = NotificationCompat.getSortKey(paramNotification);
        if (str != null) {
            return str;
        }
        Bundle localBundle = NotificationCompat.getExtras(paramNotification);
        if ((localBundle != null) && (localBundle.getString("android.support.wearable.groupKey") != null))
        {
            int i = localBundle.getInt("android.support.wearable.groupOrder");
            if (i != -1) {
                return getSortKeyForLegacyOrder(i);
            }
        }
        return null;
    }

    private static String getSortKeyForLegacyOrder(int paramInt)
    {
        Object[] arrayOfObject = new Object[1];
        arrayOfObject[0] = Long.valueOf(paramInt + 2147483648L);
        return String.format("%010d", arrayOfObject);
    }

    public static NotificationCompat.WearableExtender getWearableOptions(Notification paramNotification)
    {
        NotificationCompat.WearableExtender localWearableExtender = new NotificationCompat.WearableExtender(paramNotification);
        Bundle localBundle = NotificationCompat.getExtras(paramNotification);
        if (localBundle != null)
        {
            Parcelable[] arrayOfParcelable = localBundle.getParcelableArray("android.support.wearable.actions");
            if ((arrayOfParcelable != null) && (arrayOfParcelable.length > 0))
            {
                localWearableExtender.clearActions();
                int i = arrayOfParcelable.length;
                for (int j = 0; j < i; j++) {
                    localWearableExtender.addAction(parseLegacyWearableAction((Bundle)arrayOfParcelable[j]));
                }
            }
        }
        return localWearableExtender;
    }

    public static boolean isGroupSummary(Notification paramNotification)
    {
        if (NotificationCompat.isGroupSummary(paramNotification)) {}
        Bundle localBundle;
        do
        {
            localBundle = NotificationCompat.getExtras(paramNotification);
            if ((localBundle == null) || (localBundle.getString("android.support.wearable.groupKey") == null)) {
                return true;
            }
        } while (localBundle.getInt("android.support.wearable.groupOrder") == -1);
        return false;
    }

    private static RemoteInput parseLegacyRemoteInputBundle(Bundle paramBundle)
    {
        return new RemoteInput.Builder(paramBundle.getString("return_key")).setLabel(paramBundle.getString("label")).setChoices(paramBundle.getStringArray("choices")).setAllowFreeFormInput(paramBundle.getBoolean("allowFreeFormInput")).build();
    }

    private static RemoteInput[] parseLegacyRemoteInputBundles(Bundle[] paramArrayOfBundle)
    {
        RemoteInput[] arrayOfRemoteInput;
        if (paramArrayOfBundle == null) {
            arrayOfRemoteInput = null;
        }
        arrayOfRemoteInput = new RemoteInput[paramArrayOfBundle.length];
        for (int i = 0; i < paramArrayOfBundle.length; i++) {
            arrayOfRemoteInput[i] = parseLegacyRemoteInputBundle(paramArrayOfBundle[i]);
        }
        return arrayOfRemoteInput;
    }

    public static Bundle[] getBundleArrayFromBundle(Bundle paramBundle, String paramString)
    {
        Parcelable[] arrayOfParcelable = paramBundle.getParcelableArray(paramString);
        if (((arrayOfParcelable instanceof Bundle[])) || (arrayOfParcelable == null)) {
            return (Bundle[])arrayOfParcelable;
        }
        Bundle[] arrayOfBundle = (Bundle[]) Arrays.copyOf(arrayOfParcelable, arrayOfParcelable.length,android.os.Bundle[].class);
        paramBundle.putParcelableArray(paramString, arrayOfBundle);
        return arrayOfBundle;
    }

    private static NotificationCompat.Action parseLegacyWearableAction(Bundle paramBundle)
    {
        int i = paramBundle.getInt("icon");
        CharSequence localCharSequence = paramBundle.getCharSequence("title");
        PendingIntent localPendingIntent = (PendingIntent)paramBundle.getParcelable("action_intent");
        Bundle localBundle = (Bundle)paramBundle.getParcelable("extras");
        NotificationCompat.Action.Builder localBuilder = new NotificationCompat.Action.Builder(i, localCharSequence, localPendingIntent);
        if (localBundle != null)
        {
            localBuilder.addExtras(localBundle);
            RemoteInput[] arrayOfRemoteInput = parseLegacyRemoteInputBundles(getBundleArrayFromBundle(localBundle, "android.support.wearable.remoteInputs"));
            if (arrayOfRemoteInput != null)
            {
                int j = arrayOfRemoteInput.length;
                for (int k = 0; k < j; k++) {
                    localBuilder.addRemoteInput(arrayOfRemoteInput[k]);
                }
            }
        }
        return localBuilder.build();
    }
}
