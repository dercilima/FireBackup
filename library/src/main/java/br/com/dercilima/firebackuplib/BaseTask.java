package br.com.dercilima.firebackuplib;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;

public abstract class BaseTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    private WeakReference<Context> context;

    public BaseTask(Context context) {
        this.context = new WeakReference<>(context);
    }

    protected Context getContext() {
        return context.get();
    }

    protected File getAppDataDir() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return getContext().getDataDir();
        } else {
            return new File(getContext().getFilesDir(), "../");
        }
    }

    protected File getDatabasesDir() {
        final File dbDir = new File(getAppDataDir(), "databases");
        checkIfExists(dbDir);
        return dbDir;
    }

    protected File getPreferencesDir() {
        final File dbDir = new File(getAppDataDir(), "shared_prefs");
        checkIfExists(dbDir);
        return dbDir;
    }

    protected void checkIfExists(File path) {
        if (path != null) {
            if (!path.exists() && path.mkdirs()) {
                Log.i(getClass().getSimpleName(), "Directory \"" + path + "\" created!");
            }
        }
    }

}
