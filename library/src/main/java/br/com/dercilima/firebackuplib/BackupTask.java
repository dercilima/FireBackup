package br.com.dercilima.firebackuplib;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import br.com.dercilima.firebackuplib.utils.FileUtil;
import br.com.dercilima.zipfileslib.ZipFiles;

public class BackupTask extends AsyncTask<Void, Exception, File> {

    public interface Callback {
        /**
         * Quando finaliza o backup com sucesso
         *
         * @param backupFile Arquivo de backup local
         */
        void onBackupSuccess(File backupFile);

        /**
         * Quando finaliza o Upload com sucesso (caso tenha sido configurado o upload do backup)
         *
         * @param backupUrl Url do Storage do Firebase
         */
        void onUploadSucess(Uri backupUrl);

        /**
         * Quando ocorre um erro em algum momento do backup ou do upload (se for o caso)
         *
         * @param error Erro ocorrido
         */
        void onBackupError(Exception error);
    }

    private WeakReference<Context> context;
    private Callback mCallback;

    // Diretório onde será armazenado os backups
    private File backupDirectory;

    // Nome do arquivo de backup. Ex.: "MeuBackup" resultará em um arquivo "MeuBackup.zip"
    private String backupName;

    // Preferências para backup
    private final HashMap<String, SharedPreferences> preferencesList = new HashMap<>();

    // Bancos de dados para backup
    private final List<String> dbList = new ArrayList<>();

    // Indica se faz upload do backup para o Firebase Storage
    private boolean uploadToStorage = false;

    // Indica o caminho onde o arquivo será armazenado no Firebase Storage
    private String uploadPath;

    // Indica se exclui o arquivo após upload para o Storage
    private boolean deleteBackupAfterUpload = false;


    public BackupTask(Context context) {
        this.context = new WeakReference<>(context);
    }

    @Override
    protected File doInBackground(Void... arg0) {

        try {

            // Criar uma pasta tmp para salvar os arquivos, para compactar todos juntos
            createTempDirectory();

            // Copiar os arquivos para a pasta temp
            copyFiles();

            // Zipar os arquivos
            return zipar();

        } catch (Exception e) {
            publishProgress(e);
            cancel(true);
        } finally {
            // Apagar a pasta temp
            deleteTempDir();
        }

        return null;
    }

    private File zipar() throws IOException {

        // Verificar se tem um nome de backup informado
        if (backupName == null || backupName.trim().isEmpty()) {
            // Gerar o nome
            backupName = "Backup_" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(new Date());
        }

        if (!backupName.endsWith(".zip") && !backupName.endsWith(".rar")) {
            backupName += ".zip";
        }

        // Arquivo zip
        final File fileZip = new File(getBackupDirectory(), backupName);

        // Zipar
        new ZipFiles(getListFiles(), fileZip).zip();

        return fileZip;
    }

    private List<File> getListFiles() {
        final List<File> files = new ArrayList<>();

        // Adicionar os arquivos de preferências
        for (String key : getPreferencesList().keySet()) {
            files.add(getTempFilePreferences(key));
        }

        // Adicionar os databases
        for (String databaseName : getDbList()) {
            files.add(getTempFileDatabase(databaseName));
        }

        return files;
    }

    private void deleteTempDir() {

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

    private void createTempDirectory() {

        final File tempDir = getTempDir();

        // Se existir, apaga
        deleteTempDir();

        // Criar diretório vazio
        if (tempDir.mkdirs()) {
            Log.i(getContext().getString(R.string.app_name), "Diretório \"" + tempDir + "\" criado!");
        }

    }

    private void copyFiles() throws IOException {
        if (getPreferencesList().isEmpty() && getDbList().isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo para fazer backup!");
        }
        copyPreferences();
        copyDatabase();
    }

    private void copyPreferences() throws IOException {

        // A forma de copiar as preferencias é diferente, não se pode copiar o
        // arquivo de preferência diretamente, pois o android não lê o arquivo
        // quando restaura o backup

        for (String key : getPreferencesList().keySet()) {

            ObjectOutputStream oos = null;

            try {

                oos = new ObjectOutputStream(new FileOutputStream(getTempFilePreferences(key)));
                oos.writeObject(getPreferencesList().get(key).getAll());

            } finally {

                if (oos != null) {
                    oos.flush();
                    oos.close();
                }

            }

        }

    }

    private void copyDatabase() throws IOException {
        for (String databaseName : getDbList()) {
            FileUtil.copyFile(getContext(), getFileDatabase(databaseName), getTempFileDatabase(databaseName), true);
        }
    }

    private File getTempFilePreferences(String fileName) {
        return new File(getTempDir(), getFilePreferences(fileName).getName());
    }

    private File getFilePreferences(String fileName) {
        return new File(getContext().getFilesDir(), "../shared_prefs/" + fileName + ".xml");
    }

    private File getTempFileDatabase(String databaseName) {
        return new File(getTempDir(), getFileDatabase(databaseName).getName());
    }

    private File getTempDir() {
        return new File(getBackupDirectory(), "temp");
    }

    private File getFileDatabase(String databaseName) {
        return getContext().getDatabasePath(databaseName);
    }

    @Override
    protected void onProgressUpdate(Exception... values) {
        super.onProgressUpdate(values);
        if (values != null && values.length > 0) {
            mCallback.onBackupError(values[0]);
        }
    }

    @Override
    protected void onPostExecute(File result) {
        super.onPostExecute(result);
        if (!isDeleteBackupAfterUpload()) {
            mCallback.onBackupSuccess(result);
        }
        if (isUploadToStorage()) {
            uploadBackup(result);
        }
    }

    private void uploadBackup(final File backup) {

        // Obter uma referência do storage do firebase
        final StorageReference backupRef = FirebaseStorage.getInstance()
                // Obter uma referência do Storage
                .getReference()
                // Indicar o caminho onde será armazenado o arquivo de backup
                .child(getUploadPath() != null ? getUploadPath() : "")
                // Nome do arquivo
                .child(backup.getName());

        // Adicionar um timeout de 5 segundos
        backupRef.getStorage().setMaxUploadRetryTimeMillis(5000);

        // Fazer o backup
        backupRef.putFile(Uri.fromFile(backup))
                .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful() && task.getException() != null) {
                            throw task.getException();
                        }
                        return backupRef.getDownloadUrl();
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (isDeleteBackupAfterUpload()) {
                            deleteBackupsLocal();
                        }
                        if (task.isSuccessful()) {
                            mCallback.onUploadSucess(task.getResult());
                        } else {
                            mCallback.onBackupError(task.getException());
                        }
                    }
                });

    }

    private Context getContext() {
        return context.get();
    }

    /**
     * Adiciona o nome do banco de dados para backup. Pode adicionar vários bancos de dados.
     *
     * @param databaseName Nome do banco de dados para backup
     */
    public BackupTask addDatabaseName(String databaseName) {
        dbList.add(databaseName);
        return this;
    }

    private List<String> getDbList() {
        return dbList;
    }

    /**
     * Adiciona o nome do arquivo de preferências para backup. Pode adicionar vários arquivos.
     *
     * @param name  Nome do arquivo de preferências
     * @param prefs SharedPreferences que gerencia a preferência cujo nome foi informado
     */
    public BackupTask addPreferences(String name, SharedPreferences prefs) {
        this.preferencesList.put(name, prefs);
        return this;
    }

    private HashMap<String, SharedPreferences> getPreferencesList() {
        return preferencesList;
    }

    public BackupTask setCallback(Callback callback) {
        this.mCallback = callback;
        return this;
    }

    /**
     * Informa o diretório onde serão armazenado os backups. Caso não seja informado, o
     * caminho padrão é o diretório "Backups" na raiz do ExternalStorage
     *
     * @param backupDirectory Diretório onde serão armazenado os backups
     */
    public BackupTask setBackupDirectory(File backupDirectory) {
        this.backupDirectory = backupDirectory;
        return this;
    }

    /**
     * Informa o nome do arquivo de backup. Não é necessário adicionar a extensão do arquivo, ex: .zip ou .rar
     *
     * @param backupName Nome do arquivo de backup
     */
    public BackupTask setBackupName(String backupName) {
        this.backupName = backupName;
        return this;
    }

    private File getBackupDirectory() {
        if (backupDirectory == null) {
            backupDirectory = new File(Environment.getExternalStorageDirectory(), "Backups");
        }
        if (!backupDirectory.exists()) {
            if (backupDirectory.mkdirs()) {
                Log.d(getClass().getSimpleName(), "Directory \"" + backupDirectory + "\" created!");
            }
        }
        return backupDirectory;
    }

    private boolean isUploadToStorage() {
        return uploadToStorage;
    }

    private String getUploadPath() {
        return uploadPath;
    }

    /**
     * Indica que após realizado o backup, será feito o upload para o Storage do Firebase
     * E, indica o caminho onde o arquivo será armazenado no Firebase Storage.
     * Ex.: nome_da_empresa/codigo_do_vendedor/ ...
     *
     * @param uploadToStorage Flag indicativa
     * @param uploadPath Caminho onde o arquivo será armazenado no Firebase Storage
     */
    public BackupTask setUploadToStorage(boolean uploadToStorage, String uploadPath) {
        this.uploadToStorage = uploadToStorage;
        this.uploadPath = uploadPath;
        return this;
    }

    private boolean isDeleteBackupAfterUpload() {
        return deleteBackupAfterUpload;
    }

    /**
     * Indica que após upload do backup, o arquivo será excluído.
     * Se deleteBackupAfterUpload == true, o método "onBackupSuccess" do callback não será chamado.
     *
     * @param deleteBackupAfterUpload Flag indicativa
     */
    public BackupTask setDeleteBackupAfterUpload(boolean deleteBackupAfterUpload) {
        this.deleteBackupAfterUpload = deleteBackupAfterUpload;
        return this;
    }

    /**
     * Excluí todos os arquivos de backup dentro do diretório de backup informado
     */
    private void deleteBackupsLocal() {
        for (File backup : getBackupDirectory().listFiles()) {
            if (backup.delete()) {
                Log.d(getContext().getString(R.string.app_name), " Arquivo de backup \"" + backup.getName() + "\" excluído!");
            }
        }
    }

}
