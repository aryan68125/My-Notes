package com.aditya.mynotes.listeners;

import com.aditya.mynotes.entities.Note;

//this is an interface class which is responsible for reading data from the database when note in the main activity is clicked
//this class is called inside the noteAdapter class
//hadneling note read or update functionality
public interface NotesListener
{
     void onNoteClicked(Note note, int position);

     void onNoteLongClicked(Note note, int position);
}
