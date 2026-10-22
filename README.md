# IGPosts — Story Studio

Aplicativo Android **offline** para transformar designs em imagens que simulam um post do Instagram compartilhado nos Stories. Interface e exportação são implementadas em Java nativo, sem serviços remotos.

## Funcionalidades

- **Prévia 9:16 em tempo real** e exportação PNG **1080 × 1920** para a galeria, em **Imagens / IGPosts**.
- Card branco central com cabeçalho, foto e nome de usuário, arte e legenda, inspirado na captura de referência.
- **Formatos de arte 1:1 e 3:4**; opções **Preencher** (recorta o excedente) e **Encaixar** (preserva a imagem inteira, podendo deixar faixas).
- Importação de imagens pela galeria Android, com correção de orientação EXIF.
- Nome de usuário, foto, legenda e hashtags **configurados manualmente**, salvos em armazenamento privado.
- **Vários perfis locais**, com criação, seleção e remoção.
- Fundo **desfocado a partir da própria arte**, controle de escurecimento, largura e posição vertical do cartão.
- Botão para abrir o PNG no editor de Stories do Instagram (se o aplicativo estiver instalado); caso contrário, usa a folha de compartilhamento do Android.
- Sem login, sem servidor, sem anúncios e **sem permissão de internet**.

## Como compilar

Requisitos: JDK 17, Android SDK Platform 35, Gradle 8.9.

No Android Studio, abra a pasta raiz do projeto e aguarde a sincronização do Gradle. Para gerar pelo terminal com Gradle instalado:

```sh
gradle :app:assembleDebug
```

Resultado: `app/build/outputs/apk/debug/app-debug.apk`.

O workflow [Android APK](.github/workflows/android.yml) compila automaticamente cada alteração enviada à branch `main` e oferece o APK na seção **Actions → Android APK → Artifacts → IGPosts-debug**. Também pode ser acionado manualmente por **Run workflow**.

> **Importante sobre atualizações:** o APK debug gerado em execuções diferentes do GitHub Actions pode receber assinaturas de debug diferentes. Se o Android recusar "Atualizar", desinstale a versão anterior ou configure uma chave de assinatura release permanente antes da distribuição.

## Como usar

1. Abra o aplicativo, toque em **Importar arte da galeria** e escolha o seu design.
2. Selecione a proporção **1:1** ou **3:4**, com recorte ou encaixe.
3. Edite o nome, escolha o avatar, adicione legenda e hashtags.
4. Ajuste largura/posição do cartão e o escurecimento do fundo.
5. Toque em **Salvar em 1080 × 1920** ou **Abrir no Story do Instagram**.

O layout é uma **simulação visual** da interface de compartilhamento, não uma publicação real do Instagram. A imagem exportada não possui link interativo para abrir um post, e pequenos detalhes visuais podem diferir de versões específicas do aplicativo Instagram. Projeto independente, não afiliado à Meta.

## Organização

- `EditorState.java`: configurações, perfis e persistência local por SharedPreferences.
- `StoryRenderer.java`: desenho unificado da prévia e do PNG, 1080 × 1920.
- `MainActivity.java`: editor, galeria, importação offline, exportação MediaStore e envio a Stories.

Suporta Android 10 (API 29) ou superior. Nenhuma dependência de interface de terceiros.
