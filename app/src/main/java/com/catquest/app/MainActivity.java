package com.catquest.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 100;
    private static final int TAKE_PHOTO = 101;

    private final int ink = Color.rgb(35, 26, 56);
    private final int purple = Color.rgb(111, 75, 220);
    private final int lavender = Color.rgb(239, 232, 255);
    private final int muted = Color.rgb(120, 111, 141);
    private LinearLayout root;
    private LinearLayout collection;
    private ImageView preview;
    private TextView status;
    private TextView catCount;
    private Button addButton;
    private Uri candidateUri;
    private Bitmap candidateBitmap;
    private final List<CatEntry> cats = new ArrayList<>();

    private static class CatEntry {
        String name;
        String uri;
        CatEntry(String name, String uri) { this.name = name; this.uri = uri; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(45, 31, 76));
        getWindow().setNavigationBarColor(Color.rgb(248, 246, 252));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        buildUi();
        loadCats();
        renderCats();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(248, 246, 252));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(32));
        scroll.addView(root);
        setContentView(scroll);

        // Hero header
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(20), dp(20), dp(22));
        hero.setBackground(gradient(Color.rgb(53, 37, 91), Color.rgb(105, 70, 195), 28));
        TextView brand = text("✦  CATQUEST CLUB", 12, Color.rgb(224, 211, 255), true);
        brand.setLetterSpacing(0.13f);
        hero.addView(brand);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(0, dp(15), 0, dp(4));
        LinearLayout titleStack = new LinearLayout(this);
        titleStack.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Every cat is\na new adventure.", 29, Color.WHITE, true);
        title.setLineSpacing(dp(1), 1f);
        titleStack.addView(title);
        TextView sub = text("Find your feline friends. Build your legend.", 13, Color.rgb(229, 220, 248), false);
        sub.setPadding(0, dp(9), 0, 0);
        titleStack.addView(sub);
        titleRow.addView(titleStack, new LinearLayout.LayoutParams(0, -2, 1));
        TextView mascot = text("🐈", 54, Color.WHITE, false);
        mascot.setGravity(Gravity.CENTER);
        titleRow.addView(mascot, new LinearLayout.LayoutParams(dp(76), dp(88)));
        hero.addView(titleRow);
        root.addView(hero);

        // Collection stats
        LinearLayout stats = new LinearLayout(this);
        stats.setGravity(Gravity.CENTER_VERTICAL);
        stats.setPadding(dp(16), dp(13), dp(16), dp(13));
        stats.setBackground(rounded(Color.WHITE, 20));
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(-1, -2);
        statsParams.setMargins(0, dp(14), 0, dp(18));
        root.addView(stats, statsParams);
        LinearLayout statCopy = new LinearLayout(this);
        statCopy.setOrientation(LinearLayout.VERTICAL);
        statCopy.addView(text("YOUR CATDEX", 10, muted, true));
        catCount = text("0 cats collected", 18, ink, true);
        catCount.setPadding(0, dp(3), 0, 0);
        statCopy.addView(catCount);
        stats.addView(statCopy, new LinearLayout.LayoutParams(0, -2, 1));
        TextView sparkle = text("✦", 28, purple, true);
        sparkle.setGravity(Gravity.CENTER);
        stats.addView(sparkle, new LinearLayout.LayoutParams(dp(42), dp(42)));

        sectionHeading("Find a new friend", "Pick a photo or snap a cat in the wild.");
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(12), 0, dp(14));
        Button gallery = button("＋  Gallery", purple);
        gallery.setOnClickListener(v -> pickImage());
        actions.addView(gallery, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button camera = button("◎  Camera", Color.rgb(64, 48, 94));
        camera.setOnClickListener(v -> takePhoto());
        LinearLayout.LayoutParams cameraParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        cameraParams.setMargins(dp(10), 0, 0, 0);
        actions.addView(camera, cameraParams);
        root.addView(actions);

        // Discovery card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(14));
        card.setBackground(rounded(Color.WHITE, 24));

        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackground(rounded(lavender, 18));
        preview.setClipToOutline(true);
        card.addView(preview, new LinearLayout.LayoutParams(-1, dp(215)));

        status = text("✨  Your next discovery goes here", 13, muted, false);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(8), dp(15), dp(8), dp(13));
        card.addView(status);

        addButton = button("Add discovery to Catdex  →", purple);
        addButton.setEnabled(false);
        addButton.setAlpha(0.45f);
        addButton.setOnClickListener(v -> nameCandidate());
        card.addView(addButton, new LinearLayout.LayoutParams(-1, dp(50)));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, 0, 0, dp(26));
        root.addView(card, cardParams);

        LinearLayout dexHeader = new LinearLayout(this);
        dexHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView dexTitle = text("Your Catdex", 23, ink, true);
        dexHeader.addView(dexTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView collectionTag = text("COLLECTION", 10, purple, true);
        collectionTag.setPadding(dp(10), dp(7), dp(10), dp(7));
        collectionTag.setBackground(rounded(lavender, 30));
        dexHeader.addView(collectionTag);
        root.addView(dexHeader);
        TextView dexSubtitle = text("A little love, one cat at a time.", 13, muted, false);
        dexSubtitle.setPadding(0, dp(5), 0, dp(13));
        root.addView(dexSubtitle);
        collection = new LinearLayout(this);
        collection.setOrientation(LinearLayout.VERTICAL);
        root.addView(collection);
        TextView footer = text("MADE FOR CAT PEOPLE  ♥", 10, Color.rgb(160, 151, 178), true);
        footer.setGravity(Gravity.CENTER);
        footer.setLetterSpacing(0.12f);
        footer.setPadding(0, dp(24), 0, 0);
        root.addView(footer);
    }

    private void sectionHeading(String title, String subtitle) {
        TextView heading = text(title, 21, ink, true);
        root.addView(heading);
        TextView detail = text(subtitle, 13, muted, false);
        detail.setPadding(0, dp(4), 0, 0);
        root.addView(detail);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) startActivityForResult(intent, TAKE_PHOTO);
        else Toast.makeText(this, "No camera app found.", Toast.LENGTH_SHORT).show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) return;
        try {
            if (requestCode == PICK_IMAGE && data.getData() != null) {
                candidateUri = data.getData();
                try {
                    getContentResolver().takePersistableUriPermission(candidateUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {}
                candidateBitmap = null;
                InputImage image = InputImage.fromFilePath(this, candidateUri);
                preview.setImageURI(candidateUri);
                checkCat(image);
            } else if (requestCode == TAKE_PHOTO && data.getExtras() != null) {
                Object value = data.getExtras().get("data");
                if (value instanceof Bitmap) {
                    candidateBitmap = (Bitmap) value;
                    File file = new File(getCacheDir(), "catquest-camera-" + System.currentTimeMillis() + ".jpg");
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        candidateBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out);
                    }
                    candidateUri = Uri.fromFile(file);
                    preview.setImageBitmap(candidateBitmap);
                    checkCat(InputImage.fromBitmap(candidateBitmap, 0));
                }
            }
        } catch (Exception e) {
            status.setText("Couldn't open that image. Please try another one.");
            Toast.makeText(this, "Image could not be opened.", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCat(InputImage image) {
        status.setText("🔎  Scanning for feline energy…");
        addButton.setEnabled(false);
        addButton.setAlpha(0.45f);
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS).process(image)
            .addOnSuccessListener(labels -> {
                float confidence = 0f;
                for (ImageLabel label : labels) {
                    String name = label.getText().toLowerCase();
                    if (name.equals("cat") || name.equals("tabby cat") || name.equals("kitten")) {
                        confidence = Math.max(confidence, label.getConfidence());
                    }
                }
                if (confidence >= 0.50f) {
                    status.setText("🐈  Cat detected! Give your new friend a name.");
                    addButton.setEnabled(true);
                    addButton.setAlpha(1f);
                } else {
                    candidateUri = null;
                    candidateBitmap = null;
                    preview.setImageDrawable(null);
                    status.setText("🙀  No cat detected. Try a clearer cat photo!");
                    Toast.makeText(this, "CatQuest only adds photos that look like cats.", Toast.LENGTH_LONG).show();
                }
            })
            .addOnFailureListener(e -> {
                status.setText("⚠️  Couldn't scan this image. Try again.");
                Toast.makeText(this, "Couldn't analyze image. Please try again.", Toast.LENGTH_SHORT).show();
            });
    }

    private void nameCandidate() {
        if (candidateUri == null && candidateBitmap == null) return;
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("e.g. Mochi, Sir Purrs-a-lot");
        new AlertDialog.Builder(this)
            .setTitle("Name your cat 🐾")
            .setView(name)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add to Catdex", (dialog, which) -> {
                String chosen = name.getText().toString().trim();
                if (chosen.isEmpty()) chosen = "Mystery Cat " + (cats.size() + 1);
                String uri = candidateUri == null ? "" : candidateUri.toString();
                cats.add(0, new CatEntry(chosen, uri));
                saveCats();
                renderCats();
                status.setText("✨  " + chosen + " joined your Catdex!");
                candidateUri = null;
                candidateBitmap = null;
                addButton.setEnabled(false);
                addButton.setAlpha(0.45f);
            }).show();
    }

    private void loadCats() {
        try {
            JSONArray array = new JSONArray(getPreferences(MODE_PRIVATE).getString("cats", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                cats.add(new CatEntry(obj.optString("name", "Cat"), obj.optString("uri", "")));
            }
        } catch (Exception ignored) {}
    }

    private void saveCats() {
        JSONArray array = new JSONArray();
        try {
            for (CatEntry cat : cats) {
                JSONObject obj = new JSONObject();
                obj.put("name", cat.name);
                obj.put("uri", cat.uri);
                array.put(obj);
            }
            getPreferences(MODE_PRIVATE).edit().putString("cats", array.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void renderCats() {
        collection.removeAllViews();
        if (catCount != null) catCount.setText(cats.size() + (cats.size() == 1 ? " cat collected" : " cats collected"));
        if (cats.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(25), dp(20), dp(25));
            empty.setBackground(rounded(Color.WHITE, 22));
            TextView icon = text("🐾", 32, purple, false);
            icon.setGravity(Gravity.CENTER);
            empty.addView(icon);
            TextView title = text("Your story starts here", 17, ink, true);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, dp(8), 0, dp(4));
            empty.addView(title);
            TextView caption = text("Collect your first cat to fill this space with cuteness.", 12, muted, false);
            caption.setGravity(Gravity.CENTER);
            empty.addView(caption);
            collection.addView(empty);
            return;
        }
        for (int i = 0; i < cats.size(); i++) {
            CatEntry cat = cats.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(12), dp(12), dp(12));
            row.setBackground(rounded(Color.WHITE, 20));

            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackground(rounded(lavender, 15));
            image.setClipToOutline(true);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(76), dp(76));
            row.addView(image, imageParams);
            if (!cat.uri.isEmpty()) {
                try {
                    Uri uri = Uri.parse(cat.uri);
                    if ("file".equals(uri.getScheme())) image.setImageBitmap(BitmapFactory.decodeFile(uri.getPath()));
                    else image.setImageURI(uri);
                } catch (Exception ignored) {}
            }

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setPadding(dp(13), 0, dp(6), 0);
            TextView name = text(cat.name, 17, ink, true);
            info.addView(name);
            TextView badge = text("✦  CATDEX DISCOVERY  #" + (i + 1), 9, purple, true);
            badge.setPadding(0, dp(6), 0, 0);
            info.addView(badge);
            row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            TextView heart = text("♥", 20, Color.rgb(239, 121, 158), true);
            row.addView(heart);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.setMargins(0, 0, 0, dp(10));
            collection.addView(row, rowParams);
        }
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable gradient(int start, int end, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String value, int color) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(8), 0, dp(8), 0);
        b.setBackground(rounded(color, 16));
        return b;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
