package com.example.mynotesclouddb;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements NoteFragment.OnNoteListInteractionListener {

    boolean displayEditor = false;
    Note editingNote;
    ArrayList<Note> notes  = new ArrayList<>();;

    ListenerRegistration listenerRegistration;
    private static final String TAG = "Firebase Demo";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("onCreate", "Notes count: " + (notes != null ? notes.size() : 0));
        if (!displayEditor) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.container, NoteFragment.newInstance(notes), "list_note");
            ft.commit();
        } else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.container, EditNoteFragment.newInstance(editingNote.getContent()), "edit_note");
            ft.commit();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        listenerRegistration = db.collection("notes").orderBy("date", Query.Direction.ASCENDING).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e(TAG, "Error retrieving notes", e);
                    return;
                }
                notes.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Log.d(TAG, doc.getData().toString());
                    Note note = doc.toObject(Note.class);
                    notes.add(note);
                }
                NoteFragment listFragment = (NoteFragment) getSupportFragmentManager().findFragmentByTag("list_note");
                listFragment.updateNotes(notes);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.d("OnOptionsItemSelected", item.getTitle().toString());
        invalidateOptionsMenu();
        switch (item.getItemId()) {
            case R.id.action_new:
                editingNote = createNote();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.container, EditNoteFragment.newInstance(""), "edit_note");
                ft.addToBackStack(null);
                ft.commit();
                displayEditor = true;
                return true;
            case R.id.action_close:
                onBackPressed();
                displayEditor = false;
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d("onPrepareOptionsMenu", "" + menu.findItem(R.id.action_new).isVisible());
        menu.findItem(R.id.action_new).setVisible(!displayEditor);
        menu.findItem(R.id.action_close).setVisible(displayEditor);
        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public void onBackPressed() {
        displayEditor = !displayEditor;
        invalidateOptionsMenu();

        EditNoteFragment editFragment = (EditNoteFragment) getSupportFragmentManager().findFragmentByTag("edit_note");
        String content = null;
        if (editFragment != null) {
            Log.d("onBackPressed", "Saving...");
            content = editFragment.getContent();
            saveContent(editingNote, content);
        }
        super.onBackPressed();
        if (content != null) {
            saveContent(editingNote, content);
        }
    }

    @Override
    public void onNoteSelected(Note note) {
         Log.d(TAG, "onNoteSelected: "+ note.getContent());
        editingNote = note;
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        String content = note.getContent();
        ft.replace(R.id.container, EditNoteFragment.newInstance(content), "edit _note");
        ft.addToBackStack(null);
        ft.commit();

        displayEditor = !displayEditor;
        invalidateOptionsMenu();
    }


    private Note createNote() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Note note = new Note();
        note.setId(db.collection("notes").document().getId());
        return note;
    }

    private void saveContent(Note note, String content) {
        Log.d(TAG, "saveContent");
        if (note.getContent() == null || !note.getContent().equals(content)) {
            Log.d(TAG, "saveContent: 1st condition");
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            note.setDate(new Timestamp(new Date()));
            note.setContent(content);
            db.collection("notes").document(note.getId()).set(note);
        } else {
            Log.d(TAG, "notes: " + notes);
            NoteFragment listFragment = (NoteFragment) getSupportFragmentManager().findFragmentByTag("list_note");
            listFragment.updateNotes(notes);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        listenerRegistration.remove();
    }

}