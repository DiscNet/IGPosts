# IGPosts — Story Studio

Aplicativo Android **offline** para transformar designs em imagens que simulam um post do Instagram compartilhado nos Stories. Também possui uma versão web que funciona diretamente no navegador, sem instalação.

## Versão web

Acesse **[IGPosts Web](https://discnet.github.io/IGPosts/)** para usar o editor no navegador. A versão web permite importar artes e avatares, configurar perfis, editar legenda e hashtags, ajustar enquadramento e aparência e exportar PNG em **1080 × 1920**. O processamento é feito localmente no navegador.

O site é publicado pelo workflow [Deploy IGPosts Web](.github/workflows/pages.yml) a partir da pasta `web/`. Para ativar a publicação na primeira vez, abra **Settings → Pages** no repositório e selecione **GitHub Actions** como Build and deployment source. Depois disso, cada alteração em `web/` na branch `main` dispara a publicação.

## Funcionalidades Android

- **Prévia 9:16 em tempo real** e exportação PNG **1080 × 1920** para a galeria, em **Imagens / IGPosts**.
- Card branco central com cabeçalho, foto e nome de usuário, arte e legenda, inspirado na captura de referência.
- **Formatos de arte 1:1 e 3:4**, com visualização alternativa do retrato em **IG 4:5** ou **3:4 completo**; opções **Preencher** ou **Encaixar**.
- Importação de imagens pela galeria Android, com correção de orientação EXIF.
- Nome de usuário, foto, legenda e hashtags configurados manualmente e salvos em armazenamento privado.
- **Vários perfis locais**, com criação, seleção e remoção.
- Fundo desfocado a partir da própria arte, controle de escurecimento, largura e posição vertical do cartão.
- Efeitos opcionais: vidro translúcido nas partes brancas do cartão e borda de vidro suave na arte.
- Botão para abrir o PNG no editor de Stories do Instagram (se instalado); caso contrário, usa a folha de compartilhamento do Android.
- Sem login, sem servidor, sem anúncios e sem permissão de internet.

## Como compilar

Requisitos: JDK 17, Android SDK Platform 35, Gradle 8.9.

No Android Studio, abra a pasta raiz do projeto e aguarde a sincronização do Gradle. Para gerar pelo terminal:

```sh
gradle :app:assembleDebug
```

Resultado: `app/build/outputs/apk/debug/app-debug.apk`.

## APK direto em GitHub Releases

A cada push na branch `main`, o workflow [Android APK and GitHub Release](.github/workflows/android.yml) compila o aplicativo e anexa automaticamente o APK a uma nova GitHub Release.

- **[Baixar APK mais recente](https://github.com/DiscNet/IGPosts/releases/latest/download/IGPosts.apk)**
- [Ver todas as versões](https://github.com/DiscNet/IGPosts/releases)

### Assinatura de atualização permanente (opcional)

Por padrão, quando ainda não existem secrets, o GitHub publica um APK de teste assinado com chave debug temporária. Para permitir atualizações sem reinstalação, configure os secrets `IGPOSTS_KEYSTORE_BASE64`, `IGPOSTS_STORE_PASSWORD`, `IGPOSTS_KEY_ALIAS` e `IGPOSTS_KEY_PASSWORD`. Nunca publique o arquivo keystore no repositório.

## Como usar no Android

1. Importe a arte da galeria.
2. Selecione a proporção **1:1** ou **3:4**, com recorte ou encaixe.
3. Edite o nome, escolha o avatar, adicione legenda e hashtags.
4. Ajuste a aparência do cartão.
5. Salve em 1080 × 1920 ou abra no Story do Instagram.

O layout é uma simulação visual da interface de compartilhamento, não uma publicação real do Instagram. Projeto independente, não afiliado à Meta.

## Organização

- `web/`: versão web estática para GitHub Pages.
- `EditorState.java`: configurações, perfis e persistência local Android.
- `StoryRenderer.java`: desenho da prévia e do PNG Android.
- `MainActivity.java`: interface, importação, exportação e integração com Stories.

Suporta Android 10 (API 29) ou superior.
