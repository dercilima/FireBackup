package br.com.dercilima.firebackuplib;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    private WeakReference<Context> context;

    // Preferências para backup
    private final Set<String> preferencesList = new HashSet<>();

    // Bancos de dados para backup
    private final Set<String> dbList = new HashSet<>();


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

    protected Set<String> getDbList() {
        return dbList;
    }

    protected Set<String> getPreferencesList() {
        return this.preferencesList;
    }

    protected void createTempDir() {

        final File tempDir = getTempDir();

        // Se existir, apaga
        deleteTempDir();

        // Criar diretório vazio
        if (tempDir.mkdirs()) {
            Log.i(getContext().getString(R.string.app_name), "Diretório \"" + tempDir + "\" criado!");
        }

    }

    protected void deleteTempDir() {

        final File tempDir = getTempDir();

        if (tempDir.exists()) {

            // Deletar todos os arquivos que estão dentro da pasta temp
            for (File f : tempDir.listFiles()) {
                if (f.delete()) {
                    Log.i(getContext().getString(R.string.app_name), "Arquivo \"" + f + "\" excluído!");
                }
            }

            // Deletar o diretório
            if (tempDir.delete()) {
                Log.i(getContext().getString(R.string.app_name), "Diretório \"" + tempDir + "\" excluído!");
            }
        }

    }

    protected abstract File getTempDir();

}
