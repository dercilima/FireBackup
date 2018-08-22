package br.com.dercilima.firebackuplib;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import br.com.dercilima.firebackuplib.utils.FileUtil;
import br.com.dercilima.zipfileslib.ZipFiles;

public class RestoreBackupTask extends AsyncTask<File, Exception, List<File>> {

    public interface OnRestoreCompleteListener {
        void onRestoreSucess();

        void onRestoreError(Exception e);
    }

    private WeakReference<Context> context;
    private ProgressDialog dialog;
    private OnRestoreCompleteListener mCallback;

    // Diretório onde buscará os backups para fazer o restore
    private File restoreDir;

    // Map de preferências. Pode-se ter n arquivos de preferências para restaurar
    private final Map<String, SharedPreferences> preferencesMap = new HashMap<>();

    // Nome do arquivo de banco de dados
    private final Set<String> dbList = new HashSet<>();

    public RestoreBackupTask(Context context) {
        this.context = new WeakReference<>(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = ProgressDialog.show(context.get(), context.get().getString(R.string.aguarde), context.get().getString(R.string.restaurando_backup), true, false);
    }

    @Override
    protected List<File> doInBackground(File... files) {

        try {

            if (files == null || files.length == 0) {

                /*
                 * Localizar os backups
                 */
                final List<File> backups = new ArrayList<>();

                // Verificar na pasta backups os arquivos .zip
                for (File f : getRestoreDir().listFiles()) {
                    if (f.isFile() && (f.getName().contains(".zip") || f.getName().contains(".rar"))) {
                        backups.add(f);
                    }
                }

                if (backups.isEmpty()) {
                    throw new FileNotFoundException(context.get().getString(R.string.msg_nenhum_backup_encontrado));
                }

                // Ordenar a lista de backups
                Collections.sort(backups, new Comparator<File>() {
                    @Override
                    public int compare(File o1, File o2) {
                        return o1.compareTo(o2);
                    }
                });

                // Retorna a lista de backups
                return backups;

            } else {

                /*
                 * Retaurar o backup recebido
                 */
                final File backupFile = files[0];

                // Criar uma pasta temp para salvar os arquivos descompactados
                createTempDir();

                // Extrair os arquivo para dentro da pasta temp
                extractFiles(backupFile);

                // Copiar os arquivos descompactados para as devidas pastas
                copyFiles();

                // Deletar o arquivo de backup
                if (backupFile.delete()) {
                    Log.d(context.get().getString(R.string.app_name), "Arquivo " + backupFile.getAbsolutePath() + " excluído!");
                }

            }

        } catch (Exception e) {
            // Informa o erro ao usuário
            publishProgress(e);
            // Se deu erro, cancela a task
            cancel(true);
        } finally {
            // Deletar o diretório temp
            deleteTempDir();
        }

        return null;
    }

    private void extractFiles(File fileZip) throws Exception {
        new ZipFiles(fileZip, getTempDir()).unzip();
    }

    private void createTempDir() {

        // Remover a pasta temp caso existir
        deleteTempDir();

        // Criar a pasta vazia
        if (getTempDir().mkdirs()) {
            Log.i(getClass().getSimpleName(), "Diretório \"" + getTempDir() + "\" criado!");
        }

    }

    private File getTempDir() {
        return new File(getRestoreDir(), "temp");
    }

    private void deleteTempDir() {

        final File tempDir = getTempDir();

        if (tempDir.exists()) {

            // Deletar todos os arquivos que estão dentro da pasta temp
            for (File f : tempDir.listFiles()) {
                if (f.delete()) {
                    Log.i(getClass().getSimpleName(), "Arquivo \"" + f + "\" excluído!");
                }
            }

            // Deletar o diretório
            if (tempDir.delete()) {
                Log.i(getClass().getSimpleName(), "Diretório \"" + tempDir + "\" excluído!");
            }
        }

    }

    private void copyFiles() throws IOException {

        // Copiar arquivo de preferencias
        copyPreferences();

        // Copiar arquivo do banco de dados
        copyDatabase();

    }

    private void copyPreferences() throws IOException {

        if (getPreferencesMap().isEmpty()) {
            return;
        }

        for (String preferencesName : getPreferencesMap().keySet()) {

            final File filePrefs = new File(getTempDir(), preferencesName + ".xml");

            if (!filePrefs.exists()) {
                throw new FileNotFoundException(context.get().getString(R.string.msg_arquivo_prefs_not_found));

            } else {

                // O restore do arquivo de configuração não é feito por meio de copia de arquivo.
                // O arquivo é exportado em forma de key/value e importado da mesma forma.
                loadSharedPreferencesFromFile(filePrefs, getPreferencesMap().get(preferencesName));

            }

        }

    }

    @SuppressWarnings("unchecked")
    private void loadSharedPreferencesFromFile(File filePrefs, SharedPreferences preferences) {

        ObjectInputStream input = null;

        try {

            input = new ObjectInputStream(new FileInputStream(filePrefs));

            // Instanciar o arquivo de preferências para Criar, caso não exista, o arquivo de preferências
            //Preferencias prefs = new Preferencias(context.get());

            // Limpar o arquivo de preferências, caso tenha alguma preferência salva, será apagada
            preferences.edit().clear().apply();

            // Ler as chaves e valores do arquivo de preferência que foi feito o backup
            Map<String, ?> entries = (Map<String, ?>) input.readObject();

            for (Entry<String, ?> entry : entries.entrySet()) {
                // Salvar a preferência
                set(preferences, entry.getKey(), entry.getValue());
            }

        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException("Erro ao carregar as preferências!", e);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void set(SharedPreferences preferences, String key, Object value) {
        if (value instanceof Boolean)
            preferences.edit().putBoolean(key, (Boolean) value).apply();
        else if (value instanceof Float)
            preferences.edit().putFloat(key, (Float) value).apply();
        else if (value instanceof Integer)
            preferences.edit().putInt(key, (Integer) value).apply();
        else if (value instanceof Long)
            preferences.edit().putLong(key, (Long) value).apply();
        else if (value instanceof String)
            preferences.edit().putString(key, ((String) value)).apply();
    }

    private void copyDatabase() throws IOException {

        if (!getDbList().isEmpty()) {

            for (String databaseName : getDbList()) {

                final File fileDatabase = new File(getTempDir(), databaseName);

                if (!fileDatabase.exists()) {
                    throw new FileNotFoundException(context.get().getString(R.string.msg_arquivo_db_not_found));
                }

                copyDatabase(fileDatabase);

            }

        } else {

            // Procurar os arquivos de banco de dados e copiar para a pasta
            for (File f : getTempDir().listFiles()) {
                if (f.isFile() && (f.getName().endsWith(".sqlite") || f.getName().endsWith(".db"))) {
                    copyDatabase(f);
                }
            }

        }

    }

    private void copyDatabase(File fileDatabase) throws IOException {

        // Criar o path de databases caso não exista
        final File dest = new File(context.get().getFilesDir(), "../databases");
        if (!dest.exists() && dest.mkdirs()) {
            Log.i(getClass().getSimpleName(), "Diretório \"" + dest + "\" criado!");
        }

        Log.d(getClass().getSimpleName(), "copyDatabase: from=[" + fileDatabase + "] to=[" + dest + "]");

        // Copiar
        FileUtil.copyFile(context.get(), fileDatabase, new File(dest, fileDatabase.getName()), true);
    }

    @Override
    protected void onProgressUpdate(Exception... values) {
        super.onProgressUpdate(values);
        dismissDialog();
        if (values != null && values.length > 0) {
            mCallback.onRestoreError(values[0]);
        }
    }

    @Override
    protected void onPostExecute(final List<File> result) {
        super.onPostExecute(result);
        dismissDialog();

        if (result == null) {
            mCallback.onRestoreSucess();
        } else {

            if (result.size() == 1) {

                // Se tiver apenas um backup, restaura sem abrir o dialog
                restartTask(result.get(0));

            } else {

                // Criar o adapter
                final List<String> list = new ArrayList<>();
                for (File file : result) {
                    list.add(file.getName());
                }
                final ArrayAdapter<String> adapter = new ArrayAdapter<>(context.get(), android.R.layout.select_dialog_singlechoice, list);

                // Criar o dialog para selecionar o backup a ser restaurado
                new AlertDialog.Builder(context.get())
                        .setTitle(R.string.selecione_o_backup)
                        .setNegativeButton(R.string.cancelar, null)
                        .setAdapter(adapter, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Executar novamente a task, passando o arquivo selecionado para descompactar
                                restartTask(result.get(list.indexOf(adapter.getItem(which))));
                            }
                        })
                        .create()
                        .show();

            }

        }

    }

    // Reinicia a task passando todos os dados da task atual + o file para descompactar
    private void restartTask(File file) {
        final RestoreBackupTask task = new RestoreBackupTask(context.get());
        task.setCallback(mCallback);
        task.setRestoreDir(restoreDir);
        for (String preferencesName : getPreferencesMap().keySet()) {
            task.addPreferences(preferencesName, getPreferencesMap().get(preferencesName));
        }
        for (String databaseName : getDbList()) {
            task.addDatabaseName(databaseName);
        }
        task.execute(file);
    }

    // Fecha o ProgressDialog
    @Override
    protected void onCancelled() {
        super.onCancelled();
        dismissDialog();
    }

    private void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    public RestoreBackupTask setCallback(OnRestoreCompleteListener callback) {
        this.mCallback = callback;
        return this;
    }

    private File getRestoreDir() {
        if (restoreDir == null) {
            throw new IllegalArgumentException("É necessário informar o diretório para restauração de backups!");
        }
        return restoreDir;
    }

    /**
     * Diretório para procurar os arquivos de backup
     *
     * @param restoreDir
     */
    public RestoreBackupTask setRestoreDir(File restoreDir) {
        if (restoreDir != null && restoreDir.isDirectory()) {
            this.restoreDir = restoreDir;
        } else {
            throw new IllegalArgumentException("Restore Directory precisa ser um diretório!");
        }
        return this;
    }

    private Map<String, SharedPreferences> getPreferencesMap() {
        return this.preferencesMap;
    }

    /**
     * Adiciona o nome do arquivo de preferências e uma instancia da classe
     * que gerencia essas preferências. Pode-se ter vários arquivos.
     *
     * @param preferencesName
     * @param preferences
     */
    public RestoreBackupTask addPreferences(String preferencesName, SharedPreferences preferences) {
        this.preferencesMap.put(preferencesName, preferences);
        return this;
    }

    private Set<String> getDbList() {
        return dbList;
    }

    /**
     * Nome do banco de dados dentro do backup
     *
     * @param databaseName
     */
    public RestoreBackupTask addDatabaseName(String databaseName) {
        this.dbList.add(databaseName);
        return this;
    }

}
