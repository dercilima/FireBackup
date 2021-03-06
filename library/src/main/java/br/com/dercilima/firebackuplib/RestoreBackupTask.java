package br.com.dercilima.firebackuplib;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import br.com.dercilima.firebackuplib.utils.FileUtil;
import br.com.dercilima.zipfileslib.ZipFiles;

public class RestoreBackupTask extends BaseTask<File, Exception, List<File>> {

    public interface OnRestoreCompleteListener {
        void onRestoreSucess();

        void onRestoreError(Exception e);
    }

    private ProgressDialog dialog;
    private OnRestoreCompleteListener mCallback;

    // Diretório onde buscará os backups para fazer o restore
    private File restoreDir;


    public RestoreBackupTask(Context context) {
        super(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = ProgressDialog.show(getContext(), getContext().getString(R.string.aguarde), getContext().getString(R.string.restaurando_backup), true, false);
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
                    throw new FileNotFoundException(getContext().getString(R.string.msg_nenhum_backup_encontrado));
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
                    Log.d(getContext().getString(R.string.app_name), "Arquivo " + backupFile.getAbsolutePath() + " excluído!");
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

    @Override
    protected File getTempDir() {
        return new File(getRestoreDir(), "temp");
    }

    private void copyFiles() throws IOException {

        // Copiar arquivo de preferencias
        copyPreferences();

        // Copiar arquivo do banco de dados
        copyDatabase();

    }

    private void copyPreferences() throws IOException {

        if (getPreferencesList().isEmpty()) {
            return;
        }

        for (String preferenceName : getPreferencesList()) {

            // Se for uma expressão
            if (preferenceName.contains("*")) {

                // Procurar os arquivos com a expressão
                for (File prefsFound : findFiles(preferenceName)) {
                    loadSharedPreferencesFromFile(prefsFound);
                }

            } else {

                // Se for o nome do arquivo
                final File filePrefs = new File(getTempDir(), preferenceName + (!preferenceName.endsWith(".xml") ? ".xml" : ""));

                if (!filePrefs.exists()) {
                    throw new FileNotFoundException(getContext().getString(R.string.msg_arquivo_prefs_not_found));
                } else {
                    loadSharedPreferencesFromFile(filePrefs);
                }

            }

        }

    }

    // O restore do arquivo de configuração não é feito por meio de copia de arquivo.

    // O arquivo é exportado em forma de key/value e importado da mesma forma.
    private void loadSharedPreferencesFromFile(File filePrefs) {

        ObjectInputStream input = null;

        try {

            input = new ObjectInputStream(new FileInputStream(filePrefs));

            String prefsName = filePrefs.getName();
            prefsName = prefsName.substring(0, prefsName.indexOf(".xml"));
            final SharedPreferences preferences = getContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE);

            // Limpar o arquivo de preferências, caso tenha alguma preferência salva, será apagada
            preferences.edit().clear().apply();

            // Ler as chaves e valores do arquivo de preferência que foi feito o backup
            Map<String, ?> entries = (Map<String, ?>) input.readObject();

            for (Entry<String, ?> entry : entries.entrySet()) {
                // Salvar a preferência
                if (entry.getValue() instanceof Boolean)
                    preferences.edit().putBoolean(entry.getKey(), (Boolean) entry.getValue()).apply();
                else if (entry.getValue() instanceof Float)
                    preferences.edit().putFloat(entry.getKey(), (Float) entry.getValue()).apply();
                else if (entry.getValue() instanceof Integer)
                    preferences.edit().putInt(entry.getKey(), (Integer) entry.getValue()).apply();
                else if (entry.getValue() instanceof Long)
                    preferences.edit().putLong(entry.getKey(), (Long) entry.getValue()).apply();
                else if (entry.getValue() instanceof String)
                    preferences.edit().putString(entry.getKey(), ((String) entry.getValue())).apply();
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
    private void copyDatabase() throws IOException {

        if (!getDbList().isEmpty()) {

            for (String databaseName : getDbList()) {

                // Verificar se o nome é uma expressão
                if (databaseName.contains("*")) {
                    // Encontra os arquivos que combinem com a expressão
                    for (File found : findFiles(databaseName)) {
                        copyDatabase(found);
                    }
                } else {
                    final File fileDatabase = new File(getTempDir(), databaseName);
                    if (!fileDatabase.exists()) {
                        throw new FileNotFoundException(getContext().getString(R.string.msg_arquivo_db_not_found));
                    }
                    copyDatabase(fileDatabase);
                }

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
        final File dest = getDatabasesDir();

        Log.d(getClass().getSimpleName(), "copyDatabase: from=[" + fileDatabase + "] to=[" + dest + "]");

        // Copiar
        FileUtil.copyFile(getContext(), fileDatabase, new File(dest, fileDatabase.getName()), true);
    }

    private Set<File> findFiles(String expression) {
        final Set<File> filesFound = new HashSet<>();

        if (!expression.contains("*")) {
            throw new IllegalArgumentException("Expressão mal formada!");
        }

        // Procurar por aquivos que combinem com a expressão
        for (File f : getTempDir().listFiles()) {
            if (f.isFile()) {
                if (expression.startsWith("*") && expression.endsWith("*")) {
                    if (f.getName().contains(expression.replace("*", ""))) {
                        filesFound.add(f);
                    }
                } else if (expression.startsWith("*")) {
                    if (f.getName().endsWith(expression.replace("*", ""))) {
                        filesFound.add(f);
                    }
                } else if (expression.endsWith("*")) {
                    if (f.getName().startsWith(expression.replace("*", ""))) {
                        filesFound.add(f);
                    }
                }
            }
        }

        return filesFound;
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
                final ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.select_dialog_singlechoice, list);

                // Criar o dialog para selecionar o backup a ser restaurado
                new AlertDialog.Builder(getContext())
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
        final RestoreBackupTask task = new RestoreBackupTask(getContext());
        task.setCallback(mCallback);
        task.setRestoreDir(getRestoreDir());
        for (String preferencesName : getPreferencesList()) {
            task.addPreferenceName(preferencesName);
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

    protected File getRestoreDir() {
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
        if (restoreDir == null) {
            throw new IllegalArgumentException("Restore Directory precisa ser informado!");
        }
        if (!restoreDir.isDirectory()) {
            throw new IllegalArgumentException("Restore Directory precisa ser um diretório!");
        }
        this.restoreDir = restoreDir;
        return this;
    }

    /**
     * Adiciona o nome do arquivo de preferências. Pode-se ter vários arquivos.
     *
     * @param preferenceName
     */
    public RestoreBackupTask addPreferenceName(String preferenceName) {
        getPreferencesList().add(preferenceName);
        return this;
    }

    /**
     * Nome do banco de dados dentro do backup
     *
     * @param databaseName
     */
    public RestoreBackupTask addDatabaseName(String databaseName) {
        getDbList().add(databaseName);
        return this;
    }


}
