package com.aditya.mynotes.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.aditya.mynotes.R;
import com.aditya.mynotes.activities.CreateNoteActivity;
import com.aditya.mynotes.adapters.NotesAdapter;
import com.aditya.mynotes.database.NotesDatabase;
import com.aditya.mynotes.entities.Note;
import com.aditya.mynotes.listeners.NotesListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {

    //hadneling note read or update functionality
    public static final int REQUEST_CODE_ADD_NOTE=1; //request code to add a new note
    public static final int REQUEST_CODE_UPDATE_NOTE=2; //request code to update note
    private int noteClickedPosition = -1;
    private static final int REQUEST_CODE_SHOW_NOTES = 3; //this request code will be used to display all notes

    //handeling delete operation when a note is long pressed in the recyclerView activity
    private int noteLongClickedPosition = -1;
    //calling the custom delete note alert dialog box
    private AlertDialog dialogDeleteNote;
    private Note alreadyAvailableNote;

    //calling the recyclerView adapter class
    private RecyclerView notesRecyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    //handeling developer dilog box
    ImageView imageDeveloperInfo;
    private AlertDialog developerDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView imageAddNoteMain = findViewById(R.id.imageAddNoteMain);
        imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open CreateNoteActivty on press of imageAddNoteMain
                Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                startActivityForResult(intent,REQUEST_CODE_ADD_NOTE);
                //activity transition animation
                overridePendingTransition(R.anim.slide_in_left, R.anim.nothing);
            }
        });

        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL)
        );

        noteList = new ArrayList<>();
        //this as a second parameter means mainactivity is the target for NotesListener Interface class
        notesAdapter = new NotesAdapter(noteList,this);
        notesRecyclerView.setAdapter(notesAdapter);

        //getNotes is the method that is called in oncreate method of an activity
        //this means the application has just started and we need to get all the notes and display it in the recyclerView to the user
        //that's the reason we are passing REQUEST_CODE_SHOW_NOTES while calling getNotes function

        //here handeling delte note operation by adding a parameter to the get notes function
        //the added parameter to the get notes function is (final boolean isNoteDeleted)
        /*
        here request code is REQUEST_CODE_SHOW_NOTES it means we are displaying all the notes from the database and therefore as a parameter
        we are passing isNoteDeleted we are passing false
         */
        getNotes(REQUEST_CODE_SHOW_NOTES,false);

        //hadeling search note function in the application
        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //here we will cancel the timer
                //hence stop the search note operation when the user is still typing in the input search editText field
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
               if(noteList.size()!=0)
               {
                  notesAdapter.searchNotes(s.toString());
               }
            }
        });

        //handeling developer dialog box
        imageDeveloperInfo = findViewById(R.id.imageDeveloperInfo);
        imageDeveloperInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeveloperDialogBox();
            }
        });
    }

    public void showDeveloperDialogBox()
    {
        //here inflate the dialog box
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        //java.lang.IllegalStateException: The specified child already has a parent. You must call removeView() on the child's parent first.
        //to solve the above error just pass the argument attachToRoot: false in the
        //{View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_note, findViewById(R.id.layoutDeleteNote),false);}
        View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.developer_info_layout, null);
        builder.setView(view);
        developerDialog = builder.create();
        if(developerDialog.getWindow()!=null)
        {
            developerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        view.findViewById(R.id.textOkDeveloper).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //dismiss the dialog box
                developerDialog.dismiss();
            }
        });
        developerDialog.show(); //showing showDeleteNoteDialog
    }

    //handeling the operation what should happen after a note is clicked in the recyclerView of the mainActivity
    //update note finction is handeled in this method
    //hadneling note read or update functionality
    @Override
    public void onNoteClicked(Note note, int position) {
         noteClickedPosition = position;
         //opening createNoteActivity when a note is clicked in the recyclerView in the main activity
        Intent intent = new Intent(getApplicationContext(),CreateNoteActivity.class);
        intent.putExtra("isVieworUpdate",true);
        intent.putExtra("note",note);
        startActivityForResult(intent,REQUEST_CODE_UPDATE_NOTE);
        overridePendingTransition(R.anim.slide_in_left, R.anim.nothing);
    }

    //handeling delete operation when a note is long pressed in the recyclerView in the main Activity
    public void onNoteLongClicked(Note note, int position)
    {
        noteLongClickedPosition = position;
        //delete the note when it is long pressed
        //now here we can write the logic for delete operation for the note from the database
        //replace already available note with note
        alreadyAvailableNote = note;
        showDeleteNoteDialog();
    }

    //handling delete note functionality
    //creating a delete dialog box
    public void showDeleteNoteDialog()
    {
        if(dialogDeleteNote == null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            //java.lang.IllegalStateException: The specified child already has a parent. You must call removeView() on the child's parent first.
            //to solve the above error just pass the argument attachToRoot: false in the
            //{View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_note, findViewById(R.id.layoutDeleteNote),false);}
            View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_note_long_press, null);
            builder.setView(view);
            dialogDeleteNote = builder.create();
            if(dialogDeleteNote.getWindow()!=null)
            {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteNoteLongPress).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //deleting note from the room database
                    @SuppressLint("StaticFieldLeak")
                    class DeleteNoteTask extends AsyncTask<Void,Void,Void>
                    {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            NotesDatabase.getDatabase(getApplicationContext()).noteDao().deleteNote(alreadyAvailableNote);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            Toast.makeText(getApplicationContext(),"Note Deleted!" , Toast.LENGTH_LONG).show();
                            //deleting note from recyclerView and refreshing adapter
                            noteList.remove(noteLongClickedPosition);
                            notesAdapter.notifyItemRemoved(noteLongClickedPosition);
                            dialogDeleteNote.dismiss();
                        }
                    }

                    new DeleteNoteTask().execute();

                }
            });

            view.findViewById(R.id.textCancelLongPress).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogDeleteNote.dismiss();
                }
            });

        }

        dialogDeleteNote.show(); //showing showDeleteNoteDialog

    }//showDeleteNoteDialog function closed


    //here handeling delte note operation by adding a parameter to the get notes function
    //the added parameter to the get notes function is (final boolean isNoteDeleted)
    private void getNotes(final int requestCode, final boolean isNoteDeleted) {
        //use async tasks to get notes from the database
        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {
            @Override
            protected List<Note> doInBackground(Void... voids) {
                //getting all the notes from the room database
                return NotesDatabase.getDatabase(getApplicationContext()).noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                Log.d("notes_list", notes.toString());
               //handeling view or update notes functionality
                //here we are adding all the notes from the database to the notesList and notify about the changes to the notesAdapters
                if(requestCode == REQUEST_CODE_SHOW_NOTES)
                {
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                }
                //here we are adding the newly created note to the noteList and notify about the changes to the notesAdapters then scroll to the 0th position or to the top of the list
                else if(requestCode == REQUEST_CODE_ADD_NOTE)
                {
                   noteList.add(0,notes.get(0));
                   notesAdapter.notifyItemInserted(0);
                   notesRecyclerView.smoothScrollToPosition(0);
                }
                //here we are removing the old note from the clicked position and adding a new updated note in the same position
                else if(requestCode == REQUEST_CODE_UPDATE_NOTE)
                {
                    noteList.remove(noteClickedPosition);

                    //handling delete operation of notes
                    if(isNoteDeleted)
                    {
                        //removing notes from the notes list and notifying changes to the adapters if isNoteDeleted true
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                    }
                    else
                    {
                        //updating notes if isNoteDeleted is false
                        //add the updated note in the same position where the old version of the note was present
                        noteList.add(noteClickedPosition,notes.get(noteClickedPosition));
                        //notifying the changes to the adapter of the recyclerView
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    }
                }
            }
        }
        new GetNotesTask().execute();
    }

    //refreshing the main activity after adding or updating notes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_ADD_NOTE && resultCode==RESULT_OK)
        {
            /*
            here we are calling getNotes from onActivityResult method of the activity and we checked the requestcode is for add note
            and the result is RESULT_OK this means a new note is being added from the Create Note Activity
            and it's result is being sent back to this main activity with the recyclerView
            that is why we are passing REQUEST_CODE_ADD_NOTE to getNotes method
             */

            //here handeling delte note operation by adding a parameter to the get notes function
            //the added parameter to the get notes function is (final boolean isNoteDeleted)
        /*
        here request code is REQUEST_CODE_ADD_NOTES it means we are adding a brand new note to the database and therefore as a parameter
        we are passing isNoteDeleted we are passing false
         */
            getNotes(REQUEST_CODE_ADD_NOTE,false);
        }
        else if(requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode==RESULT_OK)
        {
            if(data!=null)
            {
              /*
                 here we are calling getNotes from onActivityResult method of the activity and we checked the requestcode is for update note
                 and the result is RESULT_OK this means the note is already present. avaiable note is updated from CreateNoteActivity
                 and its result is sent back to this Activity
                 that is why we are passing REQUEST_CODE_UPDATE_NOTE inside the getNotes method
             */

                //here handeling delete note operation by adding a parameter to the get notes function
                //the added parameter to the get notes function is (final boolean isNoteDeleted)
        /*
        here request code is REQUEST_CODE_UPDATE_NOTES it means we are updating already available note from the database and
        therefore as a parameter we are passing data.getBooleanExtra("isNoteDeleted",false)
        data.getBooleanExtra("key",default boolean value) gets the boolean value sent by the intent in the CreateNoteActivity
         */
                getNotes(REQUEST_CODE_UPDATE_NOTE,data.getBooleanExtra("isNoteDeleted",false));
            }
        }
    }
}