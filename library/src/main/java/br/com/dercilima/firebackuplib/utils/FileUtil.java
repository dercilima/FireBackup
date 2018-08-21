package br.com.dercilima.firebackuplib.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import br.com.dercilima.firebackuplib.R;

public class FileUtil {

    /*
     * Copia arquivos de um local para o outro
     *
     * @param origem - Arquivo de origem
     *
     * @param destino - Arquivo de destino
     *
     * @param overwrite - Confirmação para sobrescrever os arquivos
     *
     * @throws IOException
     */
    public static boolean copyFile(Context context, File origem, File destino, boolean overwrite) throws IOException {

        if (destino.exists() && !overwrite) {
            Log.d(context.getString(R.string.app_name) + " - FileUtil", destino.getName() + " já existe, ignorando...");
            return false;
        }

        final FileInputStream fisOrigem = new FileInputStream(origem);
        final FileChannel fcOrigem = fisOrigem.getChannel();

        final FileOutputStream fisDestino = new FileOutputStream(destino);
        final FileChannel fcDestino = fisDestino.getChannel();

        // Copiando
        fcOrigem.transferTo(0, fcOrigem.size(), fcDestino);

        // Fechando
        fisOrigem.close();
        fisDestino.close();

        return true;
    }

}

