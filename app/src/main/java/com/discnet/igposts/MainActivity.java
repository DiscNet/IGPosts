package com.discnet.igposts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Editor 100% local, sem login e sem acesso à internet. */
public final class MainActivity extends Activity {
    private static final int REQUEST_ART = 10;
    private static final int REQUEST_AVATAR = 11;
    private static final int BG = 0xFF101116;
    private static final int PANEL = 0xFF1C1D24;
    private static final int WHITE = 0xFFF5F3F3;
    private static final int MUTED = 0xFFA6A4AD;
    private static final int ACCENT = 0xFFFF956D;

    private final StoryRenderer renderer = new StoryRenderer();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private EditorState state;
    private Bitmap artwork;
    private Bitmap avatar;
    private Bitmap backdrop;

    private StoryPreview preview;
    private TextView artworkLabel;
    private TextView avatarButton;
    private TextView squareChip;
    private TextView portraitChip;
    private TextView coverChip;
    private TextView containChip;
    private TextView saveButton;
    private TextView storyButton;
    private EditText usernameInput;
    private EditText captionInput;
    private EditText tagsInput;
    private Spinner profileSpinner;
    private ArrayAdapter<String> profileAdapter;
    private final ArrayList<String> profileNames = new ArrayList<>();
    private boolean binding;
    private boolean exporting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = EditorState.load(this);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
        loadStoredMedia();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets system = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(system.left, system.top, system.right, system.bottom);
                return insets;
            });
            root.requestApplyInsets();
        }

        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(22), dp(16), dp(18), dp(12));
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(76)));
        LinearLayout branding = column();
        top.addView(branding, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView brand = text("IGPOSTS.", 24, WHITE, true);
        brand.setLetterSpacing(-.035f);
        branding.addView(brand);
        TextView tagline = text("STORY STUDIO  /  OFFLINE", 10, ACCENT, true);
        tagline.setLetterSpacing(.16f);
        branding.addView(tagline);
        TextView export = button("SALVAR PNG", true);
        export.setTextSize(11);
        export.setOnClickListener(view -> export(false));
        top.addView(export, new LinearLayout.LayoutParams(dp(125), dp(42)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout content = column();
        content.setPadding(dp(19), dp(8), dp(19), dp(32));
        scroll.addView(content);

        sectionTitle(content, "PRÉ-VISUALIZAÇÃO", "Formato Story • 1080 × 1920");
        FrameLayout display = new FrameLayout(this);
        display.setBackground(round(0xFF23242B, 22));
        display.setPadding(dp(12), dp(13), dp(12), dp(13));
        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(-1, -2);
        displayParams.bottomMargin = dp(22);
        content.addView(display, displayParams);

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int width = Math.min(dp(285), screenWidth - dp(76));
        preview = new StoryPreview();
        FrameLayout.LayoutParams previewParams =
                new FrameLayout.LayoutParams(width, width * 16 / 9, Gravity.CENTER);
        display.addView(preview, previewParams);

        LinearLayout post = panel();
        content.addView(post, panelParams());
        sectionTitle(post, "01  /  PUBLICAÇÃO", "Imagem, enquadramento e texto");
        TextView importButton = button("＋   IMPORTAR ARTE DA GALERIA", false);
        importButton.setOnClickListener(view -> chooseImage(REQUEST_ART));
        post.addView(importButton, itemParams(49));
        artworkLabel = text("Nenhuma arte selecionada", 12, MUTED, false);
        artworkLabel.setPadding(dp(3), dp(4), 0, dp(15));
        post.addView(artworkLabel);

        fieldTitle(post, "PROPORÇÃO DO POST");
        LinearLayout ratioRow = row();
        post.addView(ratioRow, itemParams(44));
        squareChip = chip("1:1  ·  QUADRADO");
        portraitChip = chip("3:4  ·  RETRATO");
        ratioRow.addView(squareChip, weighted());
        ratioRow.addView(portraitChip, weighted());
        squareChip.setOnClickListener(v -> {
            state.ratio = "1:1";
            updateChips();
            refreshPreview();
        });
        portraitChip.setOnClickListener(v -> {
            state.ratio = "3:4";
            updateChips();
            refreshPreview();
        });

        fieldTitle(post, "ENQUADRAMENTO DA ARTE");
        LinearLayout modeRow = row();
        post.addView(modeRow, itemParams(44));
        coverChip = chip("PREENCHER");
        containChip = chip("ENCAIXAR");
        modeRow.addView(coverChip, weighted());
        modeRow.addView(containChip, weighted());
        coverChip.setOnClickListener(v -> {
            state.cropToFill = true;
            updateChips();
            refreshPreview();
        });
        containChip.setOnClickListener(v -> {
            state.cropToFill = false;
            updateChips();
            refreshPreview();
        });
        updateChips();

        fieldTitle(post, "LEGENDA (OPCIONAL)");
        captionInput = input("Escreva sua legenda…", true);
        captionInput.setMinLines(2);
        post.addView(captionInput, new LinearLayout.LayoutParams(-1, -2));
        fieldTitle(post, "HASHTAGS");
        tagsInput = input("#graphicdesign #designgrafico", true);
        tagsInput.setMinLines(2);
        post.addView(tagsInput, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout profilePanel = panel();
        content.addView(profilePanel, panelParams());
        sectionTitle(profilePanel, "02  /  PERFIL", "Dados manuais, salvos neste aparelho");

        fieldTitle(profilePanel, "PERFIL ATIVO");
        profileSpinner = new Spinner(this);
        profileSpinner.setBackground(round(0xFF2A2B32, 10));
        profileSpinner.setPadding(dp(10), 0, dp(10), 0);
        profilePanel.addView(profileSpinner, itemParams(48));
        profileAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, profileNames);
        profileSpinner.setAdapter(profileAdapter);
        rebuildProfileItems();
        profileSpinner.setSelection(state.selectedProfile);
        profileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (binding) return;
                state.selectedProfile = position;
                refreshProfileFields();
                refreshPreview();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        LinearLayout profileActions = row();
        profilePanel.addView(profileActions, itemParams(44));
        TextView addProfile = chip("＋  NOVO PERFIL");
        TextView deleteProfile = chip("REMOVER");
        profileActions.addView(addProfile, weighted());
        profileActions.addView(deleteProfile, weighted());
        addProfile.setOnClickListener(v -> createProfile());
        deleteProfile.setOnClickListener(v -> removeProfile());

        fieldTitle(profilePanel, "NOME DE USUÁRIO");
        usernameInput = input("seu_usuario", false);
        usernameInput.setSingleLine(true);
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        profilePanel.addView(usernameInput, itemParams(50));

        fieldTitle(profilePanel, "FOTO DE PERFIL");
        avatarButton = button("◉   ESCOLHER FOTO DE PERFIL", false);
        avatarButton.setOnClickListener(v -> chooseImage(REQUEST_AVATAR));
        profilePanel.addView(avatarButton, itemParams(49));

        LinearLayout appearance = panel();
        content.addView(appearance, panelParams());
        sectionTitle(appearance, "03  /  APARÊNCIA", "Ajustes para reproduzir o compartilhamento");
        addSlider(appearance, "LARGURA DO CARTÃO", 65, 94, state.cardPercent, value -> {
            state.cardPercent = value;
            refreshPreview();
        });
        addSlider(appearance, "POSIÇÃO VERTICAL", -350, 350, state.verticalOffset, value -> {
            state.verticalOffset = value;
            refreshPreview();
        });
        addSlider(appearance, "ESCURECIMENTO DO FUNDO", 0, 85, state.darkness, value -> {
            state.darkness = value;
            refreshPreview();
        });

        LinearLayout exportPanel = panel();
        content.addView(exportPanel, panelParams());
        sectionTitle(exportPanel, "04  /  EXPORTAÇÃO", "PNG de alta resolução, sem marcas d'água");
        saveButton = button("↓   SALVAR EM 1080 × 1920", true);
        saveButton.setOnClickListener(v -> export(false));
        exportPanel.addView(saveButton, itemParams(53));
        storyButton = button("↗   ABRIR NO STORY DO INSTAGRAM", false);
        storyButton.setOnClickListener(v -> export(true));
        LinearLayout.LayoutParams storyParams = itemParams(53);
        storyParams.topMargin = dp(10);
        exportPanel.addView(storyButton, storyParams);
        TextView notice = text("A exportação é uma imagem simulada. Ela não cria uma publicação real nem um link clicável. Tudo funciona sem login ou conexão.", 12, MUTED, false);
        notice.setPadding(dp(2), dp(12), dp(2), dp(2));
        exportPanel.addView(notice);

        binding = true;
        captionInput.setText(state.caption);
        tagsInput.setText(state.hashtags);
        usernameInput.setText(state.profile().username);
        binding = false;
        bindText(captionInput, value -> state.caption = value);
        bindText(tagsInput, value -> state.hashtags = value);
        bindText(usernameInput, value -> {
            String username = value.trim().replace(" ", "");
            state.profile().username = username;
            rebuildProfileItems();
        });
    }

    private void addSlider(LinearLayout parent, String label, int min, int max,
                           int initial, java.util.function.IntConsumer consumer) {
        LinearLayout titleRow = row();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(12);
        parent.addView(titleRow, lp);
        TextView heading = text(label, 11, MUTED, true);
        titleRow.addView(heading, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView number = text(String.valueOf(initial), 12, WHITE, true);
        titleRow.addView(number);
        SeekBar seek = new SeekBar(this);
        seek.setMax(max - min);
        seek.setProgress(initial - min);
        seek.setProgressTintList(ColorStateList.valueOf(ACCENT));
        seek.setThumbTintList(ColorStateList.valueOf(ACCENT));
        parent.addView(seek, itemParams(42));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int value = min + progress;
                number.setText(String.valueOf(value));
                consumer.accept(value);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
    }

    private void updateChips() {
        selectChip(squareChip, "1:1".equals(state.ratio));
        selectChip(portraitChip, "3:4".equals(state.ratio));
        selectChip(coverChip, state.cropToFill);
        selectChip(containChip, !state.cropToFill);
    }

    private void selectChip(TextView view, boolean selected) {
        view.setBackground(round(selected ? ACCENT : 0xFF303038, 10));
        view.setTextColor(selected ? BG : WHITE);
    }

    private void bindText(EditText input, java.util.function.Consumer<String> setter) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding) return;
                setter.accept(s.toString());
                refreshPreview();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void rebuildProfileItems() {
        if (profileAdapter == null) return;
        profileNames.clear();
        for (EditorState.Profile profile : state.profiles) {
            profileNames.add("@" + (profile.username.isEmpty() ? "usuario" : profile.username));
        }
        profileAdapter.notifyDataSetChanged();
    }

    private void refreshProfileFields() {
        binding = true;
        usernameInput.setText(state.profile().username);
        usernameInput.setSelection(usernameInput.length());
        binding = false;
        loadAvatar();
        state.save(this);
    }

    private void createProfile() {
        EditText nameInput = input("usuario", false);
        nameInput.setSingleLine(true);
        nameInput.setText("");
        FrameLayout frame = new FrameLayout(this);
        frame.setPadding(dp(16), dp(8), dp(16), dp(4));
        frame.addView(nameInput);
        new AlertDialog.Builder(this)
                .setTitle("Adicionar perfil")
                .setView(frame)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Criar", (dialog, which) -> {
                    String username = nameInput.getText().toString().trim().replace("@", "").replace(" ", "");
                    if (username.isEmpty()) username = "novo_perfil";
                    state.profiles.add(new EditorState.Profile(username, ""));
                    state.selectedProfile = state.profiles.size() - 1;
                    rebuildProfileItems();
                    profileSpinner.setSelection(state.selectedProfile);
                    refreshProfileFields();
                    refreshPreview();
                }).show();
    }

    private void removeProfile() {
        if (state.profiles.size() == 1) {
            toast("Mantenha pelo menos um perfil salvo.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Remover perfil?")
                .setMessage("O perfil será removido da lista local. A imagem original não será afetada.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Remover", (dialog, which) -> {
                    state.profiles.remove(state.selectedProfile);
                    state.selectedProfile = Math.max(0, state.selectedProfile - 1);
                    rebuildProfileItems();
                    profileSpinner.setSelection(state.selectedProfile);
                    refreshProfileFields();
                    refreshPreview();
                }).show();
    }

    private void chooseImage(int requestCode) {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("image/*");
        try {
            startActivityForResult(picker, requestCode);
        } catch (ActivityNotFoundException ex) {
            toast("Nenhum seletor de imagens disponível.");
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        if (requestCode != REQUEST_ART && requestCode != REQUEST_AVATAR) return;
        final Uri uri = data.getData();
        final boolean isArt = requestCode == REQUEST_ART;
        final int profileIndex = state.selectedProfile;
        toast("Importando imagem…");
        worker.execute(() -> {
            File file = null;
            try {
                file = copyToPrivateStorage(uri, isArt ? "arte_" : "perfil_");
                Bitmap decoded = decodeImage(file.getAbsolutePath(), isArt ? 2600 : 700);
                if (decoded == null) throw new IllegalArgumentException("Imagem não suportada");
                Bitmap newBackdrop = isArt ? StoryRenderer.blurBackdrop(decoded) : null;
                String path = file.getAbsolutePath();
                runOnUiThread(() -> {
                    if (isArt) {
                        artwork = decoded;
                        backdrop = newBackdrop;
                        state.artworkPath = path;
                        artworkLabel.setText("Arte importada  •  " + decoded.getWidth() + " × " + decoded.getHeight());
                    } else {
                        if (profileIndex >= state.profiles.size()) return;
                        state.profiles.get(profileIndex).avatarPath = path;
                        if (profileIndex == state.selectedProfile) {
                            avatar = decoded;
                        }
                    }
                    refreshPreview();
                    toast(isArt ? "Arte carregada." : "Foto de perfil salva.");
                });
            } catch (Exception error) {
                if (file != null) file.delete();
                runOnUiThread(() -> toast("Falha ao importar: " + error.getMessage()));
            }
        });
    }

    private void loadStoredMedia() {
        loadAvatar();
        if (state.artworkPath.isEmpty()) return;
        worker.execute(() -> {
            Bitmap decoded = decodeImage(state.artworkPath, 2600);
            Bitmap blurred = StoryRenderer.blurBackdrop(decoded);
            runOnUiThread(() -> {
                artwork = decoded;
                backdrop = blurred;
                if (decoded != null) {
                    artworkLabel.setText("Arte salva  •  " + decoded.getWidth() + " × " + decoded.getHeight());
                } else {
                    artworkLabel.setText("Importe uma arte para começar");
                }
                loadAvatar();
                preview.invalidate();
            });
        });
    }

    private void loadAvatar() {
        String path = state.profile().avatarPath;
        avatar = path.isEmpty() ? null : decodeImage(path, 700);
        if (avatarButton != null) {
            avatarButton.setText(avatar == null ? "◉   ESCOLHER FOTO DE PERFIL" : "✓   FOTO DE PERFIL CONFIGURADA");
        }
        if (preview != null) preview.invalidate();
    }

    private File copyToPrivateStorage(Uri uri, String prefix) throws Exception {
        File images = new File(getFilesDir(), "imagens");
        if (!images.exists() && !images.mkdirs()) throw new Exception("Armazenamento indisponível");
        File file = new File(images, prefix + System.currentTimeMillis() + ".img");
        try (InputStream source = getContentResolver().openInputStream(uri);
             OutputStream dest = new FileOutputStream(file)) {
            if (source == null) throw new Exception("Não foi possível abrir a imagem");
            byte[] bytes = new byte[32768];
            int n;
            long total = 0;
            while ((n = source.read(bytes)) != -1) {
                total += n;
                if (total > 40L * 1024 * 1024) throw new Exception("Limite de 40 MB por imagem");
                dest.write(bytes, 0, n);
            }
        } catch (Exception error) {
            file.delete();
            throw error;
        }
        return file;
    }

    private static Bitmap decodeImage(String path, int maxSide) {
        try {
            if (path == null || path.isEmpty()) return null;
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            if (bounds.outWidth < 1 || bounds.outHeight < 1) return null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            int sample = 1;
            while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > maxSide) sample *= 2;
            options.inSampleSize = sample;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap original = BitmapFactory.decodeFile(path, options);
            if (original == null) return null;
            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try {
                ExifInterface exif = new ExifInterface(path);
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL);
            } catch (Exception ignored) {
                return original;
            }
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: matrix.postRotate(90); break;
                case ExifInterface.ORIENTATION_ROTATE_180: matrix.postRotate(180); break;
                case ExifInterface.ORIENTATION_ROTATE_270: matrix.postRotate(270); break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.postScale(-1, 1); break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL: matrix.postScale(1, -1); break;
                default: return original;
            }
            return Bitmap.createBitmap(original, 0, 0,
                    original.getWidth(), original.getHeight(), matrix, true);
        } catch (Exception ex) {
            return null;
        }
    }

    private void refreshPreview() {
        state.save(this);
        if (preview != null) preview.invalidate();
    }

    private void export(boolean openStory) {
        if (exporting) return;
        if (artwork == null) {
            toast("Importe uma arte antes de exportar.");
            return;
        }
        exporting = true;
        if (saveButton != null) saveButton.setEnabled(false);
        if (storyButton != null) storyButton.setEnabled(false);
        final EditorState snapshot = EditorState.load(this);
        final Bitmap art = artwork;
        final Bitmap profileImage = avatar;
        final Bitmap blurred = backdrop;
        toast("Gerando PNG 1080 × 1920…");
        worker.execute(() -> {
            Uri savedUri = null;
            try {
                Bitmap image = Bitmap.createBitmap(StoryRenderer.WIDTH, StoryRenderer.HEIGHT, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(image);
                new StoryRenderer().draw(canvas, StoryRenderer.WIDTH, StoryRenderer.HEIGHT,
                        snapshot, art, profileImage, blurred);
                ContentValues values = new ContentValues();
                String filename = "IGPosts_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                        .format(new Date()) + ".png";
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/IGPosts");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
                savedUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (savedUri == null) throw new Exception("Não foi possível criar o arquivo");
                try (OutputStream out = getContentResolver().openOutputStream(savedUri)) {
                    if (out == null || !image.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        throw new Exception("Falha ao escrever o PNG");
                    }
                }
                ContentValues visible = new ContentValues();
                visible.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(savedUri, visible, null, null);
                image.recycle();
                Uri result = savedUri;
                runOnUiThread(() -> {
                    exporting = false;
                    saveButton.setEnabled(true);
                    storyButton.setEnabled(true);
                    toast("Imagem salva em Fotos / IGPosts.");
                    if (openStory) shareToInstagram(result);
                });
            } catch (Exception error) {
                if (savedUri != null) getContentResolver().delete(savedUri, null, null);
                runOnUiThread(() -> {
                    exporting = false;
                    saveButton.setEnabled(true);
                    storyButton.setEnabled(true);
                    toast("Erro ao exportar: " + error.getMessage());
                });
            }
        });
    }

    private void shareToInstagram(Uri imageUri) {
        Intent instagram = new Intent("com.instagram.share.ADD_TO_STORY");
        instagram.setDataAndType(imageUri, "image/png");
        instagram.putExtra(Intent.EXTRA_STREAM, imageUri);
        instagram.setPackage("com.instagram.android");
        instagram.setClipData(ClipData.newRawUri("IGPosts", imageUri));
        instagram.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(instagram);
        } catch (ActivityNotFoundException ex) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, imageUri);
            share.setClipData(ClipData.newRawUri("IGPosts", imageUri));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Compartilhar Story"));
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        state.save(this);
        worker.shutdown();
        super.onDestroy();
    }

    private final class StoryPreview extends View {
        private final Path clip = new Path();

        StoryPreview() {
            super(MainActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            clip.reset();
            clip.addRoundRect(0, 0, getWidth(), getHeight(),
                    dp(11), dp(11), Path.Direction.CW);
            int saved = canvas.save();
            canvas.clipPath(clip);
            renderer.draw(canvas, getWidth(), getHeight(), state, artwork, avatar, backdrop);
            canvas.restoreToCount(saved);
        }
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private LinearLayout row() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout panel() {
        LinearLayout view = column();
        view.setPadding(dp(17), dp(17), dp(17), dp(20));
        view.setBackground(round(PANEL, 17));
        return view;
    }

    private LinearLayout.LayoutParams panelParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(16);
        return lp;
    }

    private LinearLayout.LayoutParams itemParams(int heightDp) {
        return new LinearLayout.LayoutParams(-1, dp(heightDp));
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        lp.setMargins(dp(3), 0, dp(3), 0);
        return lp;
    }

    private void sectionTitle(LinearLayout parent, String title, String subtitle) {
        TextView heading = text(title, 13, WHITE, true);
        heading.setLetterSpacing(.08f);
        parent.addView(heading);
        TextView description = text(subtitle, 12, MUTED, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(3);
        lp.bottomMargin = dp(14);
        parent.addView(description, lp);
    }

    private void fieldTitle(LinearLayout parent, String title) {
        TextView heading = text(title, 10, MUTED, true);
        heading.setLetterSpacing(.075f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(15);
        lp.bottomMargin = dp(8);
        parent.addView(heading, lp);
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) view.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        return view;
    }

    private TextView chip(String value) {
        TextView view = text(value, 11, WHITE, true);
        view.setGravity(Gravity.CENTER);
        view.setBackground(round(0xFF303038, 10));
        return view;
    }

    private TextView button(String value, boolean accent) {
        TextView view = text(value, 12, accent ? BG : WHITE, true);
        view.setGravity(Gravity.CENTER);
        int fill = accent ? ACCENT : 0xFF32333C;
        GradientDrawable surface = round(fill, 12);
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), surface, null));
        view.setPadding(dp(7), 0, dp(7), 0);
        return view;
    }

    private EditText input(String hint, boolean multiline) {
        EditText view = new EditText(this);
        view.setTextSize(14);
        view.setTextColor(WHITE);
        view.setHintTextColor(0xFF85838C);
        view.setHint(hint);
        view.setGravity(multiline ? Gravity.TOP : Gravity.CENTER_VERTICAL);
        view.setPadding(dp(13), dp(12), dp(13), dp(12));
        view.setBackground(round(0xFF2B2C34, 11));
        if (multiline) view.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        return view;
    }

    private GradientDrawable round(int fill, int radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radiusDp));
        return shape;
    }

    private int dp(float value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
