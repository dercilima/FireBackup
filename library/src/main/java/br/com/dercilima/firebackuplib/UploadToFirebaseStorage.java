package br.com.dercilima.firebackuplib;

import java.io.File;

public class UploadToFirebaseStorage {


    private void uploadBackup(File backup) {

        /*if (uiMode) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    if (dialog != null) {
                        dialog.setMessage("Fazendo upload do backup");
                    }
                }
            });
        }

        // Enviar o arquivo de backup para o Firebase
        final StorageReference backupsRef = FirebaseUtils.getBackupsReference(getContext());
        if (backupsRef != null) {
            backupsRef.getStorage().setMaxUploadRetryTimeMillis(5000);
            backupsRef.putFile(Uri.fromFile(backup))
                    .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful() && task.getException() != null) {
                                throw task.getException();
                            }
                            return backupsRef.getDownloadUrl();
                        }
                    })
                    .continueWithTask(new Continuation<Uri, Task<ShortDynamicLink>>() {
                        @Override
                        public Task<ShortDynamicLink> then(@NonNull Task<Uri> task) throws Exception {
                            if (!task.isSuccessful() && task.getException() != null) {
                                throw task.getException();
                            }
                            return FirebaseDynamicLinks.getInstance().createDynamicLink()
                                    .setLink(task.getResult())
                                    .setDynamicLinkDomain("sfaplus.page.link") // Configurado em Dynamic Links do Firebase
                                    .buildShortDynamicLink(ShortDynamicLink.Suffix.SHORT);
                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<ShortDynamicLink>() {
                        @Override
                        public void onComplete(@NonNull Task<ShortDynamicLink> task) {
                            closeDialog();
                            if (task.isSuccessful()) {
                                // Atualizar o nome do ultimo backup para o atual
                                prefs.setString(Preferencias.LAST_BACKUP_UPLOADED_URL, task.getResult().getShortLink().toString());
                                prefs.setString(Preferencias.DATE_OF_LAST_BACKUP_UPLOADED, com.maxx.maxxsfaplus.utils.DateFormat.yyyyMMdd_HHmmss.toString(new Date()));

                                // Retorna o link do backup
                                mCallback.onResult(task.isSuccessful() ? task.getResult().getShortLink().toString() : null);

                            } else {
                                if (uiMode) {
                                    Msg.error(getContext(), "Erro ao fazer upload do backup!");
                                }
                                Crashlytics.logException(task.getException());
                                Log.e(getContext().getString(R.string.app_name), "Erro ao enviar backup ao storage do firebase: e = [" + task.getException() + "]");
                                Logger.error(SendDataService.class, "Erro ao enviar backup ao storage do firebase! " + task.getException());
                            }

                            deleteBackupsLocal();
                        }
                    });
        }*/

    }

}
