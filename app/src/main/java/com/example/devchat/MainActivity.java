package com.example.devchat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.example.devchat.model.MessageDTO;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore mFirestore;

    MessagesAdapter mAdapter;
    RecyclerView mRecyclerView;
    Toolbar toolbar;
    TextView mDisplayNameTV;
    ImageView mProfileIV;
    EditText mMessageET;
    ProgressBar sendingProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signinScreenIntent = new Intent(MainActivity.this, ProfileSetupActivity.class);
                startActivity(signinScreenIntent);
            }
        });

        mDisplayNameTV = findViewById(R.id.display_name_text);
        mProfileIV = findViewById(R.id.profile_image);
        mMessageET = findViewById(R.id.message_et);
        sendingProgress = findViewById(R.id.sending_progress);
        sendingProgress.setVisibility(View.INVISIBLE);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        mAdapter = new MessagesAdapter(mAuth.getCurrentUser().getUid());
        mRecyclerView = findViewById(R.id.chat_rv);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        FloatingActionButton fab = findViewById(R.id.send_btn);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(MainActivity.this);
                String text = mMessageET.getText().toString();
                if(!TextUtils.isEmpty(text)){
                    sendingProgress.setVisibility(View.VISIBLE);
                    MessageDTO message = new MessageDTO(currentUser.getUid(), currentUser.getDisplayName(), text);
                    sendMessage(message);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = mAuth.getCurrentUser();
        if(currentUser == null){
            Intent signinScreenIntent = new Intent(this, SigninActivity.class);
            startActivity(signinScreenIntent);
            finish();
        }else{
            if(currentUser.getDisplayName() == null || currentUser.getDisplayName().isEmpty()){
                Intent signinScreenIntent = new Intent(this, ProfileSetupActivity.class);
                startActivity(signinScreenIntent);
            }else{
                mDisplayNameTV.setText(currentUser.getDisplayName());
                Uri imageUrl = currentUser.getPhotoUrl();

                if(imageUrl != null){
                    Glide.with(this)
                            .load(imageUrl)
                            .into(mProfileIV);
                }

                startListeningForMessages();
            }
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_edit_profile) {
            Intent signinScreenIntent = new Intent(this, ProfileSetupActivity.class);
            startActivity(signinScreenIntent);
            return true;
        }else if (id == R.id.action_logout) {
            mAuth.signOut();
            Intent signinScreenIntent = new Intent(this, SigninActivity.class);
            startActivity(signinScreenIntent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startListeningForMessages() {
        mFirestore.collection("messages")
                .orderBy("dateSent")
                .addSnapshotListener(this, new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {

                        if(e != null){
                            //an error has occured
                        }else{
                            List<MessageDTO> messages = snapshots.toObjects(MessageDTO.class);
//                            ArrayList<MessageDTO> messages = new ArrayList<>();

//                            for (DocumentChange dc : snapshots.getDocumentChanges()) {
//                                if (dc.getType() == DocumentChange.Type.ADDED) {
////                                    Log.d(TAG, "New city: " + dc.getDocument().getData());
//                                    messages.add(dc.getDocument().toObject(MessageDTO.class));
//
//                                }
//                            }

                            mAdapter.setData(messages);
                            mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                        }
                    }
                });
    }

    private void sendMessage(MessageDTO message){
        mFirestore.collection("messages")
                .add(message)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        sendingProgress.setVisibility(View.INVISIBLE);
                        if(!task.isSuccessful()){
                            Toast.makeText(MainActivity.this, "Message sending failed", Toast.LENGTH_SHORT).show();
                        }else{
                            mMessageET.setText("");
                        }
                    }
                });
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
