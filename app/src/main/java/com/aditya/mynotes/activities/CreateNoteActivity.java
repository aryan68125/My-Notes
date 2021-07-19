package com.aditya.mynotes.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aditya.mynotes.R;
import com.aditya.mynotes.database.NotesDatabase;
import com.aditya.mynotes.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText inputNoteTitle, inputNoteSubtitle, inputNoteText;
    private TextView textDateTime;

    private View viewSubtitleIndicator;

    private String selectedNoteColor;

    //request code for reading external storage
    private static final int REQUEST_CODE_STORAGE_PERMISSION=1;

    //request code for selecting image from the storage
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;

    //this is included in the logic for image selection to import image inside the note application
    private ImageView imageNote;

    //this variable will store the directory of the selected image file
    private String selectedImagePath;

    //handeling added weblink by the user
    private TextView textWebURL;
    private LinearLayout layoutWebURL;
    //dailog box for adding a weblink to the note
    private AlertDialog dialogAddURL;

    //hadneling note read or update functionality
    private Note alreadyAvailableNote;

    //handling delete note operation
    //calling the custom delete note alert dialog box
    private AlertDialog dialogDeleteNote;

    //handeling text to speech engine
    ImageView imageSpeak;
    //setting up text to speech listener
    TextToSpeech mtts;
    int everythingIsOKmttsIsGoodToGo = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);
        ImageView imageSave = findViewById(R.id.imageSave);
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator);

        //this is included in the logic for image selection to import image inside the note application
        imageNote = findViewById(R.id.imageNote);

        //handeling web link
        textWebURL = findViewById(R.id.textWebURL);
        layoutWebURL = findViewById(R.id.layoutWebURL);

        //back button
        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        inputNoteTitle = findViewById(R.id.inputNoteTitle);
        inputNoteSubtitle = findViewById(R.id.inputNoteSubtitle);
        inputNoteText = findViewById(R.id.inputNote);
        textDateTime = findViewById(R.id.textDateTime);

        textDateTime.setText(
                //"EEEE, dd MMMM yyyy HH:mm a" = Saturday 13 june 2021 21:13PM
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).
                        format(new Date())
        );

        imageSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 saveNote();
            }
        });

        selectedNoteColor = "#202020"; //default Note Color

        selectedImagePath = "";

        //hadneling note read or update functionality
        if(getIntent().getBooleanExtra("isVieworUpdate",false))
        {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        //here we will handle the weURL delete from notes operation
        findViewById(R.id.imageRemoveWebURL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textWebURL.setText(null);
                layoutWebURL.setVisibility(View.GONE);
            }
        });

        //here we will handle the image delete operation from notes
        findViewById(R.id.imageRemoveImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageNote.setImageBitmap(null);
                imageNote.setVisibility(View.GONE);
                findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
                selectedImagePath = "";
            }
        });

        //initating text to speech engine
        //calling the function speakNote() which handels and initates text to speech engine when the activity is first created
        speakNote();

        //handeling text to speech engine
        imageSpeak = findViewById(R.id.imageSpeak);
        //speak option will only be visible when you are viewing or updating already existing note
        if(alreadyAvailableNote!=null)
        {
            imageSpeak.setVisibility(View.VISIBLE);
        }

        imageSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //here text to speech engine logic will come
                //add text to speech engine
                if (everythingIsOKmttsIsGoodToGo == 1) {
                    String text = inputNoteText.getText().toString();
                    mtts.setPitch(1.1f); //setting up the pitch and speed of the speech in text to speech engine
                    mtts.setSpeechRate(1.1f);
                    //making text to speech engine to speek our entered text
                    //TextToSpeech.QUEUE_FLUSH = current txt is cancled to speak a new one
                    //TextToSpeech.QUEUE_ADD the next text is spoken after the previous text is finished
                    //mtts.speak(Passing the content of our editText, TextToSpeech.QUEUE_FLUSH,null);
                    mtts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });

        initMiscellaneous();
        setSubtitleIndicatorColor();
    }

    private void saveNote()
    {
      if(inputNoteTitle.getText().toString().trim().isEmpty())
      {
          Toast.makeText(this, "Note title Can't be empty!",Toast.LENGTH_LONG).show();
          return;
      }
      else if(inputNoteSubtitle.getText().toString().trim().isEmpty() && inputNoteText.getText().toString().trim().isEmpty())
      {
          Toast.makeText(this, "Note Can't be empty!",Toast.LENGTH_LONG).show();
          return;
      }
      final Note note = new Note();
      note.setTitle(inputNoteTitle.getText().toString());
      note.setSubtitle(inputNoteSubtitle.getText().toString());
      note.setNoteText(inputNoteText.getText().toString());
      note.setDateTime(textDateTime.getText().toString());
      note.setColor(selectedNoteColor);
      //handeling addition of an image in the note
      note.setImagePath(selectedImagePath );

      //handeling addition of the weblink to the note
        if(layoutWebURL.getVisibility() == View.VISIBLE)
        {
            note.setWebLink(textWebURL.getText().toString());
        }

        //hadneling note read or update functionality
        //set the id of the note in the Create Note Activity after clicking the note in the main activity and coming to this activity
        //the already present note will absolutely have a conflict in the NoteDao java class
        //but since we have set onConflictStrategy
        //This means if the id of the new note is already available in the database then it will be replaced with the new note and our note gets updated in the database
        if(alreadyAvailableNote!=null)
        {
            note.setId(alreadyAvailableNote.getId());//here we are setting the id of the note in the Create Note Activity
        }

      //room doesn't allow database operations on the main thread that's why we need to save our note using async task
        //saving note in the room database provided by google
        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void,Void,Void>
        {
            @Override
            protected Void doInBackground(Void... voids) {
                //inserting notes in the room database
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Intent intent = new Intent();
                setResult(RESULT_OK,intent);
                finish();
                overridePendingTransition(R.anim.nothing, R.anim.slide_out_right);
            }
        }

        new SaveNoteTask().execute();
    }

    //this method handles back button of android os
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.nothing, R.anim.slide_out_right);

    }

    //handelling layout miscellaneous
    public void initMiscellaneous()
    {
        //setting up the included layoutMiscellaneous in the CreateNote xml file
        final LinearLayout layoutMiscellaneous = findViewById(R.id.layoutMiscellaneous);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous);
        //setting up an on click Listener on the textMiscellaneous textView in the layoutMiscellaneous xml file
        layoutMiscellaneous.findViewById(R.id.textMiscellaneous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED)
                {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
                else
                {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        final ImageView imageColor1 = layoutMiscellaneous.findViewById(R.id.imageColor1);
        final ImageView imageColor2 = layoutMiscellaneous.findViewById(R.id.imageColor2);
        final ImageView imageColor3 = layoutMiscellaneous.findViewById(R.id.imageColor3);
        final ImageView imageColor4 = layoutMiscellaneous.findViewById(R.id.imageColor4);
        final ImageView imageColor5 = layoutMiscellaneous.findViewById(R.id.imageColor5);
        final ImageView imageColor6 = layoutMiscellaneous.findViewById(R.id.imageColor6);
        final ImageView imageColor7 = layoutMiscellaneous.findViewById(R.id.imageColor7);

        layoutMiscellaneous.findViewById(R.id.imageColor1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#202020";
                //set image programetically
                imageColor1.setImageResource(R.drawable.ic_done);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                imageColor6.setImageResource(0);
                imageColor7.setImageResource(0);
                //now setting image subtitle color
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.imageColor2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#FDD835";
                //set image programetically
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(R.drawable.ic_done);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                imageColor6.setImageResource(0);
                imageColor7.setImageResource(0);
                //now setting image subtitle color
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.imageColor3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#F44336";
                //set image programetically
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(R.drawable.ic_done);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                imageColor6.setImageResource(0);
                imageColor7.setImageResource(0);
                //now setting image subtitle color
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.imageColor4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#8E24AA";
                //set image programetically
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(R.drawable.ic_done);
                imageColor5.setImageResource(0);
                imageColor6.setImageResource(0);
                imageColor7.setImageResource(0);
                //now setting image subtitle color
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.imageColor5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#43A047";
                //set image programetically
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(R.drawable.ic_done);
                imageColor6.setImageResource(0);
                imageColor7.setImageResource(0);
                //now setting image subtitle color
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.imageColor6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#212B6A";
                //set image programetically
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                imageColor6.setImageResource(R.drawable.ic_done);
                imageColor7.setImageResource(0);
                //now setting image subtitle color
                setSubtitleIndicatorColor();
            }
        });

        layoutMiscellaneous.findViewById(R.id.imageColor7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedNoteColor = "#000000";
                //set image programetically
                imageColor1.setImageResource(0);
                imageColor2.setImageResource(0);
                imageColor3.setImageResource(0);
                imageColor4.setImageResource(0);
                imageColor5.setImageResource(0);
                imageColor6.setImageResource(0);
                imageColor7.setImageResource(R.drawable.ic_done);
                //now setting image subtitle color
                setSubtitleIndicatorColor();
            }
        });

        //hadneling note read or update functionality
        if(alreadyAvailableNote!=null && alreadyAvailableNote.getColor()!=null && !alreadyAvailableNote.getColor().trim().isEmpty())
        {
            switch(alreadyAvailableNote.getColor())
            {
                case "#FDD835":
                    layoutMiscellaneous.findViewById(R.id.imageColor2).performClick();
                    break;
                case "#F44336":
                    layoutMiscellaneous.findViewById(R.id.imageColor3).performClick();
                    break;
                case "#8E24AA":
                    layoutMiscellaneous.findViewById(R.id.imageColor4).performClick();
                    break;
                case "#43A047":
                    layoutMiscellaneous.findViewById(R.id.imageColor5).performClick();
                    break;
                case "#212B6A":
                    layoutMiscellaneous.findViewById(R.id.imageColor6).performClick();
                    break;
                case "#000000":
                    layoutMiscellaneous.findViewById(R.id.imageColor7).performClick();
                    break;
            }
        }

        //here is the code the will handle how add images to the note
        layoutMiscellaneous.findViewById(R.id.layoutAddImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               //setting the behaviour of the bottom sheet layout included in this current layout
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                //if the permission hasnot been granted by ther user ask for the storage permission
                //checking for the storage permission
                if(ContextCompat.checkSelfPermission( getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                {
                    //ask for permission
                    ActivityCompat.requestPermissions(CreateNoteActivity.this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_CODE_STORAGE_PERMISSION);
                }
                else //if the permission has already been granted by the user
                {
                    selectImage();
                }
            }
        });

        //here addition of a weblink to the note will be handeled
        layoutMiscellaneous.findViewById(R.id.layoutAddUrl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //setting the behaviour of the bottom sheet layout included in this current layout
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showAddUrlDialog();
            }
        });

        //here we are handeling delete note functionality
        //here we will only show the delete note option when the user is viewing or updating notes and not when user is creating a new note
        if(alreadyAvailableNote!=null)
        {
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //setting the behaviour of the bottom sheet layout included in this current layout
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    showDeleteNoteDialog();
                }
            });
        }
    }

    //this function will set the color of viewSubtitleIndicator view in the create note activity
    private void setSubtitleIndicatorColor()
    {
        GradientDrawable gradientDrawable = (GradientDrawable) viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

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

    //this method will handel the permission request code results
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

    //this method is included in the logic for importing images to our notes application
    // handeling result of the selected image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK)
        {
            //handeling the read operation of the image from the external memory of the device
            if(data!= null)
            {
                Uri selectedImageUri = data.getData();
                if(selectedImageUri !=null)
                {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        imageNote.setImageBitmap(bitmap);
                        imageNote.setVisibility(View.VISIBLE);
                        //getting the path of the selected image file
                        selectedImagePath = getPathFromUri(selectedImageUri);
                        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                    }
                    catch (Exception e)
                    {
                        Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

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

    //this method will handel the add weblink dialogbox
    //it will show the dialog box when the add web clink is clicked in the miscellaneous layout
    private void showAddUrlDialog()
    {
        if(dialogAddURL == null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
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
                        Toast.makeText(getApplicationContext(),"Enter URL",Toast.LENGTH_LONG).show();
                    }
                    //Pattern here is android utility and not java regex util
                    else if(!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches())
                    {
                        Toast.makeText(getApplicationContext(),"Enter valid URL",Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        textWebURL.setText(inputURL.getText().toString());
                        layoutWebURL.setVisibility(View.VISIBLE);
                        dialogAddURL.dismiss();
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

    //hadneling note read or update functionality
    private void setViewOrUpdateNote()
    {
        inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        inputNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
        inputNoteText.setText(alreadyAvailableNote.getNoteText());
        textDateTime.setText(alreadyAvailableNote.getDateTime());

        if(alreadyAvailableNote.getImagePath()!= null && !alreadyAvailableNote.getImagePath().trim().isEmpty())
        {
            try {
                imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
                imageNote.setVisibility(View.VISIBLE);
                findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                selectedImagePath = alreadyAvailableNote.getImagePath();
            }
            catch (Exception e)
            {
                Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
            }
        }

        if(alreadyAvailableNote.getWebLink()!=null && !alreadyAvailableNote.getWebLink().trim().isEmpty())
        {
            textWebURL.setText(alreadyAvailableNote.getWebLink());
            layoutWebURL.setVisibility(View.VISIBLE);
        }

    }

    //handling delete note functionality
    //creating a delete dialog box
    public void showDeleteNoteDialog()
    {
        if(dialogDeleteNote == null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            //java.lang.IllegalStateException: The specified child already has a parent. You must call removeView() on the child's parent first.
            //to solve the above error just pass the argument attachToRoot: false in the
            //{View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_note, findViewById(R.id.layoutDeleteNote),false);}
            View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_note, findViewById(R.id.layoutDeleteNote),false);
            builder.setView(view);
            dialogDeleteNote = builder.create();
            if(dialogDeleteNote.getWindow()!=null)
            {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteNote).setOnClickListener(new View.OnClickListener() {
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
                            Intent intent = new Intent();
                            intent.putExtra("isNoteDeleted",true);
                            setResult(RESULT_OK, intent);
                            finish();
                            overridePendingTransition(R.anim.nothing, R.anim.slide_out_right);
                        }
                    }

                    new DeleteNoteTask().execute();

                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogDeleteNote.dismiss();
                }
            });

        }

        dialogDeleteNote.show(); //showing showDeleteNoteDialog

    }//showDeleteNoteDialog function closed

    //text to speech engine function speak note's content
    public void speakNote()
    {
        //code related to text to speech engine
        //setting up text to speech engine
        mtts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    //checking if this set language method was successfull
                    int result = mtts.setLanguage(Locale.ENGLISH); //passing language to our text to speech engine if its initializaton is a success
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        //if there is a missing data or language not supported by the device then we will show an error message
                        Toast.makeText(getApplicationContext(), "Either the language is not supported by your device or the input field is empty", Toast.LENGTH_LONG).show();
                    } else {
                        //if there is no error and text to speech is successfully loaded then button is enabled
                        everythingIsOKmttsIsGoodToGo = 1;
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Initialization of text to speech engine failed!!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


}//main class closed