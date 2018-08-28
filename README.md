# FireBackup

Biblioteca utilizada para lhe auxiliar na realização de backups dos dados do seu aplicativo Android, tais como: *SqliteDatabase*, *SharedPreferences*, entre outros. Além disso, você também pode fazer o upload do backup para o *Storage do Firebase*.

# Setup

Configure o **FireBackup** no seu projeto **Android** de forma simples e rápida.

## Gradle

    implementation 'com.github.dercilima:firebackup:0.0.3'

## Maven

    <dependency>
        <groupId>com.github.dercilima</groupId>
        <artifactId>zipfiles</artifactId>
        <version>0.0.3</version>
    </dependency>

## Como usar o FireBackup?

O **FireBackup** foi projetado para ser flexível e te atender da melhor forma possível. Você pode habilitar e desabilitar funcionalidades com simples chamadas de métodos. Então, vamos começar?

## Android Permissions

Se tratando de uma lib que fará a leitura e escrita de arquivos dentro do device Android, então, é necessário adicionar duas **permissões essenciais** no seu *AndroidManifest*, que são elas:

    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>

Caso você resolva configurar para fazer upload do backup para o Storage do Firebase, então, é necessário permitir o app acessar a internet. Para isso, adicione a seguinte permissão:

    <uses-permission android:name="android.permission.INTERNET"/>

# Fazer o backup local

Essa é a forma mais simples de se fazer o backup dos dados. Podemos usar a classe *BackupTask* para esse procedimento. Conforme exemplo abaixo:

    new BackupTask(this)
            .setCallback(this)
            .addDatabaseName(DbHelper.DATABASE_NAME)
            .addPreferences(Preferences.PREFERENCES_NAME, new Preferences(this).getPreferences())
            .execute((Void) null);

Com esse simples código acima, você consegue facilmente fazer o backup do seu banco de dados e do seu arquivo de preferências. Mas bem, o que significa cada chamada de método? Não se preocupe, é bem simples!

 - **setCallback()**
	 - Recebe um interface chamada *Callback* que está declarada dentro da classe *BackupTask*. *Callback* tem 3 métodos: onBackupSuccess, onUploadSucess e onBackupError.
	 - **onBackupSuccess** é chamado quando o arquivo de backup é criado sem erros, retornando um *File* com a localização do arquivo.
	 - **onUploadSucess** é chamado quando o upload do arquivo é concluído, retornando uma *Uri* com a localização do arquivo no *Storage do Firebase*.
	 - **onBackupError** é chamado quando ocorre algum erro no backup ou no upload do arquivo, retornando uma *Exception*.
 - **addDatabaseName()**
	 - Nome do arquivo de banco de dados. Geralmente, definido na classe que herda de *SQLiteOpenHelper*, para criação do banco de dados. Caso tenha mais de um banco de dados dentro do seu aplicativo, basta chamar esse método novamente. Esse método pode ser chamado quantas vezes for necessário.
 - **addPreferences()**
	 - Recebe dois parâmetros, o primeiro é o nome do seu arquivo de preferências, e o segundo, é uma instância de *SharedPreferences*, que gerencia as cujo nome foi informado no primeiro parâmetro. Da mesma forma que o método *addDatabaseName()*, esse métodos também pode ser chamado quantas vezes for necessário.

Opcionalmente, você também pode configurar o nome e o caminho onde será armazenado o arquivo de backup no device, usando os métodos **setBackupName()** e **setBackupDirectory()**. Por padrão, os arquivos de backup são armazenados dentro de uma pasta chamada *Backups* na raiz do *External Storage* do dispositivo. Veja a seguir:

    new BackupTask(this)  
            .setCallback(this)
            .setBackupName("MeuBackup")
            .setBackupDirectory(new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name)))
            .addDatabaseName(DbHelper.DATABASE_NAME)
            .addPreferences(Preferences.PREFERENCES_NAME, new Preferences(this).getPreferences())
            .execute((Void) null);

A chamada dos métodos acima fará com que o backup seja salvo em uma pasta com o nome do projeto (R.string.app_name), na raiz do *External Storage* com o nome *MeuBackup.zip*. Visto que não foi informado a extensão do arquivo, mas, por padrão, a extensão ".zip" já é adicionada automaticamente. Mas, fique a vontade para colocar sua extensão (.zip ou .rar), ambas funcionam bem.

Para mais detalhes sobre as classes *[DbHelper](https://github.com/dercilima/FireBackup/blob/master/sample/src/main/java/br/com/dercilima/firebackup/db/DbHelper.java)* e *[Preferences](https://github.com/dercilima/FireBackup/blob/master/sample/src/main/java/br/com/dercilima/firebackup/prefs/Preferences.java)*, veja as implementações dentro do módulo *sample* do projeto.

# Backup + Upload para Storage do Firebase

Bem, para essa opção, é bom que você conheça o [Firebase](https://firebase.google.com/) e algumas de suas funcionalidades. Se não conhece ainda, não se preocupe, vou lhe acompanhar nesse processo.

## Setup Firebase

-  Primeiramente, é necessário criar uma conta no console do *Firebase*, isso pode ser feito através desse [link](https://console.firebase.google.com/).
- Após ter criado uma conta, e estar logado nela, você verá a opção para "**Adicionar projeto**". Na tela que abrir, informe o *Nome do projeto*. Aceite os *termos do controlador* e clique em **Criar projeto**.
- Projeto criado! O próximo passo é adicionar o Firebase ao seu projeto Android. Para isso, acesse [Adicionar o Firebase ao seu projeto do Android](https://firebase.google.com/docs/android/setup).

## Dependências

Continuando com a configuração do Firebase ao seu projeto, duas dependências são necessárias:

 - Storage - Para fazer o upload do backup
	> `implementation 'com.google.firebase:firebase-storage:16.0.1'`

 - Dynamic Links - Para encurtar a url do backup
	 > `implementation 'com.google.firebase:firebase-invites:16.0.1'`

## Upload

Para fazer o upload do backup gerado para o Storage do Firebase é muito simples. Veja o exemplo de código abaixo:

    new BackupTask(this)
            // ...
            .setUploadToStorage(true, "ano/mes") // Exemplo, armazenando os backups por ano e mês
            .execute((Void) null);

O método **setUploadToStorage()** recebe dois parâmetros, o primeiro habilita o *upload* do backup, e o segundo, indica o caminho onde o backup será salvo no *Storage*. Caso o caminho não seja informado, por padrão, os backups serão armazenados na raiz do *Storage*. Lembrando que, arquivos com o mesmo nome sempre serão substituídos pela versão mais recente.

> **Nota:** Por padrão, as regras de segurança para armazenamento no [Firebase Storage](https://firebase.google.com/docs/storage/?hl=pt-br), bloqueiam a leitura e escrita para usuários não autenticados. Se você quiser adicionar um nível a mais de segurança aos seus dados, consulte a página do [Firebase Authentication](https://firebase.google.com/docs/auth/?hl=pt-br) para saber mais.

Mas você pode permitir o armazenamento de dados no Firebase Storage para usuários não autenticados, apenas mudando as regras de segurança. Para isso, acesse o [console](https://console.firebase.google.com), selecione seu projeto e vá até a opção *Storage*, logo após clique na aba *Regras*. Altere conforme exemplo abaixo:

    service firebase.storage {
      match /b/{bucket}/o {
        match /{allPaths=**} {
          // allow read, write: if request.auth != null;
          allow read, write: if true; // Permitir leitura e escrita por usuários não autenticados
        }
      }
    }

## Encurtar a URL

O processo de upload do backup resulta em uma *url* que indica o local onde o arquivo foi armazenado no *Storage*. Porém, essa *url* fica bem extensa, e, seu tamanho pode ser ainda maior se o *path* de armazenamento no *Storage* e o nome do arquivo de backup for extensos. Mas, para melhorar o que já é bom, você pode facilmente habilitar a opção de encurtar a *url* com o [Firebase Dynamic Links](https://firebase.google.com/docs/dynamic-links/?hl=pt-br). Para isso, basta acessar o [console](https://console.firebase.google.com) do seu projeto no *Firebase*, navegar até a opção *Dynamic Links* e criar um domínio personalizado. Uma sugestão é usar o nome do próprio projeto, tentando não colocar um domínio muito extenso para não perder o intuito de encurtar a url.
Após configurado o Dynamic Link no console, basta usar o código abaixo para habilitar a opção de encurtar a url:

    new BackupTask(this)
            // ...
            .setShortenUrlWithDynamicLink(true, "seu_dominio.page.link")
            .execute((Void) null);

Bem, por hora é isso. Estou a disposição para qualquer dúvida ou sugestão.
Bons códigos!

## Developed By

* Derci Santos
 
&nbsp;&nbsp;&nbsp;**Email** - dercilima.si@gmail.com

## License

```
Copyright 2018 Derci Santos

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
