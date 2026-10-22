# IGPosts — Story Studio

Aplicativo Android **offline** para transformar designs em imagens que simulam um post do Instagram compartilhado nos Stories. Interface e exportação são implementadas em Java nativo, sem serviços remotos.

## Funcionalidades

- **Prévia 9:16 em tempo real** e exportação PNG **1080 × 1920** para a galeria, em **Imagens / IGPosts**.
- Card branco central com cabeçalho, foto e nome de usuário, arte e legenda, inspirado na captura de referência.
- **Formatos de arte 1:1 e 3:4**, com visualização alternativa do retrato em **IG 4:5** (altura fiel ao compartilhamento do Instagram) ou **3:4 completo**; opções **Preencher** (recorta a arte) ou **Encaixar** (preserva a imagem, podendo exibir faixas).
- Importação de imagens pela galeria Android, com correção de orientação EXIF.
- Nome de usuário, foto, legenda e hashtags **configurados manualmente**, salvos em armazenamento privado.
- **Vários perfis locais**, com criação, seleção e remoção.
- Fundo **desfocado a partir da própria arte**, controle de escurecimento, largura e posição vertical do cartão.
- Efeitos opcionais e independentes: **vidro translúcido nas partes brancas do cartão** e **borda de vidro suave na arte**; desligados por padrão.
- Botão para abrir o PNG no editor de Stories do Instagram (se o aplicativo estiver instalado); caso contrário, usa a folha de compartilhamento do Android.
- Sem login, sem servidor, sem anúncios e **sem permissão de internet**.

## Como compilar

Requisitos: JDK 17, Android SDK Platform 35, Gradle 8.9.

No Android Studio, abra a pasta raiz do projeto e aguarde a sincronização do Gradle. Para gerar pelo terminal com Gradle instalado:

```sh
gradle :app:assembleDebug
```

Resultado: `app/build/outputs/apk/debug/app-debug.apk`.

## APK direto em GitHub Releases

A cada push na branch `main`, o workflow [Android APK and GitHub Release](.github/workflows/android.yml) compila o aplicativo e anexa automaticamente o APK a uma nova GitHub Release. Também pode ser executado manualmente por **Run workflow**. Pull requests apenas compilam e não geram Releases.

- **[Baixar APK mais recente](https://github.com/DiscNet/IGPosts/releases/latest/download/IGPosts.apk)**
- [Ver todas as versões](https://github.com/DiscNet/IGPosts/releases)
- O nome do arquivo baixado é sempre `IGPosts.apk` e a versão interna é `1.0.<número da execução>`.
- O artefato em Actions permanece apenas como cópia de diagnóstico.

### Assinatura de atualização permanente (opcional)

Por padrão, quando ainda não existem secrets, o GitHub publica um **APK de teste assinado com chave debug temporária**. Ele instala, mas o Android pode recusar atualizações futuras por divergência de assinatura. Para permitir instalações por **Atualizar**, gere uma chave release permanente e configure, no repositório, os secrets de Actions:

| Secret | Conteúdo |
| --- | --- |
| `IGPOSTS_KEYSTORE_BASE64` | Conteúdo completo do arquivo keystore codificado em Base64, em linha única |
| `IGPOSTS_STORE_PASSWORD` | Senha do keystore |
| `IGPOSTS_KEY_ALIAS` | Alias de assinatura |
| `IGPOSTS_KEY_PASSWORD` | Senha da chave privada |

**Nunca publique o arquivo de chave no repositório.** Guarde uma cópia segura: perdê-lo inviabiliza assinar atualizações da mesma instalação. Quando os quatro secrets estiverem configurados, o workflow gera e publica automaticamente um APK release assinado com essa chave. A migração de um APK debug antigo para o APK release exige uma única reinstalação.

### Ícone adaptativo

O ícone usa fundo em degradê malva/azul acinzentado de sangria total e um desenho minimalista de publicação sobre vidro suave, sem pretos puros. O Android aplica a máscara do launcher; não há moldura branca fixa. A versão monocromática permite ícones temáticos do Android 13 ou posterior.

## Como usar

1. Abra o aplicativo, toque em **Importar arte da galeria** e escolha o seu design.
2. Selecione a proporção **1:1** ou **3:4**, com recorte ou encaixe. Em Aparência, escolha **IG 4:5** para a altura mais fiel ao original, ou **3:4 completo** para exibir toda a altura da arte.
3. Edite o nome, escolha o avatar, adicione legenda e hashtags.
4. Ajuste largura/posição do cartão e o escurecimento do fundo.
5. Toque em **Salvar em 1080 × 1920** ou **Abrir no Story do Instagram**.

O layout é uma **simulação visual** da interface de compartilhamento, não uma publicação real do Instagram. A imagem exportada não possui link interativo para abrir um post, e pequenos detalhes visuais podem diferir de versões específicas do aplicativo Instagram. Projeto independente, não afiliado à Meta.

## Organização

- `EditorState.java`: configurações, perfis e persistência local por SharedPreferences.
- `StoryRenderer.java`: desenho unificado da prévia e do PNG, 1080 × 1920.
- `MainActivity.java`: editor, galeria, importação offline, exportação MediaStore e envio a Stories.

Suporta Android 10 (API 29) ou superior. Nenhuma dependência de interface de terceiros.
