package com.smerkous.greengrass;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

public class BlockAppActivity extends AppCompatActivity {
    private GridLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_app);

        String packageName = null;
        String appName = null;

        try {
            packageName = getIntent().getStringExtra("package");
            appName = getIntent().getStringExtra("name");
        } catch (NullPointerException ignored) {}


        if(packageName != null && packageName.length() > 0) {
            final PackageManager manager = getPackageManager();
            try {
                Glide.with(this)
                        .asDrawable()
                        .load(manager.getApplicationIcon(packageName))
                        .apply(new RequestOptions()
                                .override(256, 256)
                                .fitCenter())
                        .into((ImageView) findViewById(R.id.block_icon));

                TextView imageGrass = findViewById(R.id.block_grass);
                String blockText = appName + " has been blocked";
                imageGrass.setText(blockText);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        layout = findViewById(R.id.block_layout);
        Glide.with(this)
                .asDrawable()
                .load(R.drawable.grass_meadow)
                .apply(RequestOptions.centerCropTransform())
                .into(new SimpleTarget<Drawable>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                layout.setBackground(resource);
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }
}
