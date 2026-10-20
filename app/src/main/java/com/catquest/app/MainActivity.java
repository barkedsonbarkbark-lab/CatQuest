package com.catquest.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 100;
    private static final int TAKE_PHOTO = 101;
    private final int ink = Color.rgb(33, 27, 52);
    private final int purple = Color.rgb(111, 74, 214);
    private LinearLayout root;
    private LinearLayout collection;
    private ImageView preview;
    private TextView status;
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
        buildUi();
        loadCats();
        renderCats();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(248, 245, 255));
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(28));
        scroll.addView(root);
        setContentView(scroll);

        TextView brand = text("🐾 CATQUEST", 14, purple, true);
        brand.setLetterSpacing(0.12f);
        root.addView(brand);
        TextView title = text("Your next cat\nadventure starts here.", 30, ink, true);
        title.setPadding(0, dp(10), 0, dp(6));
        root.addView(title);
        root.addView(text("Find cats in your photos and collect them in your Catdex.", 15, Color.rgb(100, 92, 119), false));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(18), 0, dp(8));
        Button gallery = button("＋ Choose photo");
        gallery.setOnClickListener(v -> pickImage());
        actions.addView(gallery, new LinearLayout.LayoutParams(0, dp(52), 1));
        Button camera = button("◎ Take photo");
        camera.setOnClickListener(v -> takePhoto());
        LinearLayout.LayoutParams cameraParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        cameraParams.setMargins(dp(8), 0, 0, 0);
        actions.addView(camera, cameraParams);
        root.addView(actions);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundColor(Color.WHITE);
        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackgroundColor(Color.rgb(237, 231, 249));
        card.addView(preview, new LinearLayout.LayoutParams(-1, dp(220)));
        status = text("Your cat preview will appear here ✨", 14, Color.rgb(100, 92, 119), false);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(12), 0, dp(8));
        card.addView(status);
        addButton = button("Add cat to Catdex");
        addButton.setEnabled(false);
        addButton.setAlpha(0.5f);
        addButton.setOnClickListener(v -> nameCandidate());
        card.addView(addButton, new LinearLayout.LayoutParams(-1, dp(48)));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, dp(10), 0, dp(24));
        root.addView(card, cardParams);

        TextView dexTitle = text("Your Catdex", 23, ink, true);
        root.addView(dexTitle);
        TextView dexSubtitle = text("Every discovery deserves a name.", 14, Color.rgb(100, 92, 119), false);
        dexSubtitle.setPadding(0, dp(4), 0, dp(12));
        root.addView(dexSubtitle);
        collection = new LinearLayout(this);
        collection.setOrientation(LinearLayout.VERTICAL);
        root.addView(collection);
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
                getContentResolver().takePersistableUriPermission(candidateUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
        status.setText("🔎 Checking for a cat…");
        addButton.setEnabled(false);
        addButton.setAlpha(0.5f);
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
                    status.setText("🐈 Cat detected! Looking good. Name your new discovery below.");
                    addButton.setEnabled(true);
                    addButton.setAlpha(1f);
                } else {
                    candidateUri = null;
                    candidateBitmap = null;
                    preview.setImageDrawable(null);
                    status.setText("Not quite a cat! Try a clearer photo with the cat in view.");
                    Toast.makeText(this, "CatQuest only adds photos that look like cats.", Toast.LENGTH_LONG).show();
                }
            })
            .addOnFailureListener(e -> {
                status.setText("Cat check failed. Check your connection and try again.");
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
                status.setText("Added " + chosen + " to your Catdex! ✨");
                candidateUri = null;
                candidateBitmap = null;
                addButton.setEnabled(false);
                addButton.setAlpha(0.5f);
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
        if (cats.isEmpty()) {
            TextView empty = text("No cats collected yet. Your first discovery is waiting! 🐱", 14, Color.rgb(100, 92, 119), false);
            empty.setPadding(dp(16), dp(18), dp(16), dp(18));
            empty.setBackgroundColor(Color.WHITE);
            collection.addView(empty);
            return;
        }
        for (CatEntry cat : cats) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(10), dp(10), dp(10));
            row.setBackgroundColor(Color.WHITE);
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(Color.rgb(237, 231, 249));
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(72), dp(72));
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
            info.setPadding(dp(12), 0, 0, 0);
            TextView name = text(cat.name, 17, ink, true);
            info.addView(name);
            TextView badge = text("CATDEX DISCOVERY  ·  #" + (cats.indexOf(cat) + 1), 10, purple, true);
            badge.setPadding(0, dp(5), 0, 0);
            info.addView(badge);
            row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.setMargins(0, 0, 0, dp(8));
            collection.addView(row, rowParams);
        }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(purple));
        return b;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
