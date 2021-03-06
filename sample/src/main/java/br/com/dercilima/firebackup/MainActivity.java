package br.com.dercilima.firebackup;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.io.File;

import br.com.dercilima.firebackup.db.DbHelper;
import br.com.dercilima.firebackup.prefs.Preferences;
import br.com.dercilima.firebackuplib.BackupTask;
import br.com.dercilima.firebackuplib.RestoreBackupTask;

public class MainActivity extends AppCompatActivity implements BackupTask.Callback, View.OnClickListener, RestoreBackupTask.OnRestoreCompleteListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Criar o banco de dados
        new DbHelper(this).getWritableDatabase();

        // Criar as preferências
        new Preferences(this).putString("my_key", "my_value");

        findViewById(R.id.backup_button).setOnClickListener(this);
        findViewById(R.id.restore_button).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.backup_button:

                new BackupTask(this)
                        .setCallback(this)
                        .setBackupDirectory(new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name)))
                        .setBackupName("MeuBackup")
                        .addDatabaseName(DbHelper.DATABASE_NAME)
                        .addPreferenceName(Preferences.PREFERENCES_NAME)
                        .setUploadToStorage(true, "backups")
                        .setDeleteBackupAfterUpload(true)
                        .setShortenUrlWithDynamicLink(true, "firebackup.page.link")
                        .execute((Void) null);

                break;

            case R.id.restore_button:

                new RestoreBackupTask(this)
                        .setCallback(this)
                        .setRestoreDir(new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name)))
                        .addDatabaseName(DbHelper.DATABASE_NAME)
                        .addPreferenceName(Preferences.PREFERENCES_NAME)
                        .execute();

                break;

            default:
                break;
        }
    }

    @Override
    public void onBackupSuccess(File backupFile) {
        showMessage("Backup efetuado com sucesso!\n\n" + backupFile);
    }

    @Override
    public void onUploadSucess(Uri backupUrl) {
        showMessage("Backup e Upload realizados com sucesso!\n\n" + backupUrl);
    }

    @Override
    public void onBackupError(Exception error) {
        showMessage("Erro no backup!\n\n" + error);
    }

    @Override
    public void onRestoreSucess() {
        showMessage("Backup restaurado com sucesso!");
    }

    @Override
    public void onRestoreError(Exception e) {
        showMessage("Erro no restore!\n\n" + e);
    }

    private void showMessage(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Info")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .create()
                .show();
    }
}
