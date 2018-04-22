package com.smerkous.greengrass;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.smerkous.greengrass.services.GrassService;

import java.util.List;
import java.util.Map;

import static com.smerkous.greengrass.MainActivity.Log;

public class BlockAppActivity extends AppCompatActivity {
    private RelativeLayout layout;
    private String packageName = null;
    private static boolean runDatabase = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_app);

        try {
            packageName = getIntent().getStringExtra("package");
        } catch (NullPointerException ignored) {}


        if(packageName != null && packageName.length() > 0) {
            final PackageManager manager = getPackageManager();
            try {
                Glide.with(this)
                        .asDrawable()
                        .load(manager.getApplicationIcon(packageName))
                        .apply(new RequestOptions()
                                .fitCenter())
                        .into((ImageView) findViewById(R.id.block_icon));

                String appName = manager.getApplicationLabel(manager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString();
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

        //Log into firebase
        final FirebaseAuth auth = FirebaseAuth.getInstance();

        Log("Signing in!");
        FirebaseUser currentUser = auth.getCurrentUser();
        final Runnable toRun = new Runnable() {
            @Override
            public void run() {
                Log("Signed in!");
                FirebaseFirestore database = FirebaseFirestore.getInstance();
                database.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                        //.setHost("https://thegrassisgreen-40cb2.firebaseio.com/")
                        .setSslEnabled(true)
                        .setPersistenceEnabled(true)
                        .build());
                database.collection("green").document("apps").addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                        if(runDatabase) {
                            Log("Got app update!");
                            parseSnapShot(documentSnapshot);
                        }
                    }
                });
            }
        };

        if(currentUser != null) {
            Log("Already signed in!");
            toRun.run();
        } else {
            Log("Not logged in! Signing in...");
            auth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    toRun.run();
                }
            });
        }
    }

    void parseSnapShot(DocumentSnapshot snapshot) {
        final Map<String, Object> data = snapshot.getData();
        if (data != null) {
            if(data.containsKey(GrassService.user)) {
                try {
                    final List<String> blockedApps = (List<String>) data.get(GrassService.user);
                    if (!blockedApps.contains(packageName)) {
                        Log("The master removed the current app!");
                        finish();
                        runDatabase = false;
                    }
                } catch (NullPointerException ignored) {}
            } else {
                Log("User is not in the database!");
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        runDatabase = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        runDatabase = false;
    }
}
