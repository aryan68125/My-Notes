package com.aditya.mynotes.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.aditya.mynotes.R;
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

    //handling quick action menu
    //quick action add image
    public static final int REQUEST_CODE_SELECT_IMAGE=4;
    //storage permissions
    public static final int REQUEST_CODE_STORAGE_PERMISSION = 5;
    // now here we are declaring the daialog box to add a web url from quick action menu
    private AlertDialog dialogAddURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        //adding quick actions functionality
        //QUICK ACTIONS ADD NOTE
        findViewById(R.id.imageAddNote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open CreateNoteActivty on press of imageAddNoteMain
                Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                startActivityForResult(intent,REQUEST_CODE_ADD_NOTE);
                //activity transition animation
                overridePendingTransition(R.anim.slide_in_left, R.anim.nothing);
            }
        });

        //QUICK ACTIONS ADD IMAGE
        findViewById(R.id.imageAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if the permission hasnot been granted by ther user ask for the storage permission
                //checking for the storage permission
                if(ContextCompat.checkSelfPermission( getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                {
                    //ask for permission
                    ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_CODE_STORAGE_PERMISSION);
                }
                else //if the permission has already been granted by the user
                {
                    selectImage();
                }
            }
        });

        //QUICK ACTIONS ADD URL imageView button
        findViewById(R.id.imageAddWebLink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddUrlDialog();
            }
        });
    }

    //handling quick actions add image from the quick actions menu
    //this method will handle the image selection process in our notes application from the storage
    private void selectImage()
    {
        //logic for image selection from external storage
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(intent.resolveActivity(getPackageManager()) != null)
        {
            startActivityForResult(intent,REQUEST_CODE_SELECT_IMAGE);
        }
    }

    //handling quick image add from quick actions menu
    //permission for reading external storage
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length>0)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                selectImage();
            }
            else
            {
                Toast.makeText(this,"PERMISSION DENIED by the user ! ",Toast.LENGTH_LONG).show();
            }
        }
    }

    //handling quick actions add image
    //this method gets the directory of the selected image file
    private String getPathFromUri(Uri contentUri)
    {
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri,null,null,null,null);
        if (cursor==null)
        {
            filePath = contentUri.getPath();
        }
        else{
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
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
        //handling add image quick action in quick action menu
        else if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK)
        {
            if(data!=null)
            {
                Uri selectedImageUri = data.getData();
                if(selectedImageUri != null)
                {
                    try{

                        String selectedImagePath = getPathFromUri(selectedImageUri);
                        Intent intent = new Intent(getApplicationContext(),CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions",true);
                        intent.putExtra("quickActionType","image");
                        intent.putExtra("imagePath", selectedImagePath);
                        startActivityForResult(intent,REQUEST_CODE_ADD_NOTE);

                    }catch (Exception e)
                    {
                        Toast.makeText(this, e.getMessage(),Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    //handling add url from quick actions menu
    //this method will handel the add weblink dialogbox
    //it will show the dialog box when the add web clink is clicked in the miscellaneous layout
    private void showAddUrlDialog()
    {
        if(dialogAddURL == null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_add_url,findViewById(R.id.layoutAddUrlContainer));
            builder.setView(view);
            dialogAddURL = builder.create();
            if(dialogAddURL.getWindow() != null)
            {
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputURL);
            inputURL.requestFocus();
            view.findViewById(R.id.textAdd).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //checking if the input url editText is empty or not
                    if(inputURL.getText().toString().trim().isEmpty())
                    {
                        Toast.makeText(MainActivity.this,"Enter URL",Toast.LENGTH_LONG).show();
                    }
                    //Pattern here is android utility and not java regex util
                    else if(!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches())
                    {
                        Toast.makeText(MainActivity.this,"Enter valid URL",Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        dialogAddURL.dismiss();
                        Intent intent = new Intent(getApplicationContext(),CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions",true);
                        intent.putExtra("quickActionType","URL");
                        intent.putExtra("URL", inputURL.getText().toString());
                        startActivityForResult(intent,REQUEST_CODE_ADD_NOTE);
                    }
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogAddURL.dismiss();
                }
            });
        }
        dialogAddURL.show();
    }
}