package com.aditya.mynotes.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.aditya.mynotes.entities.Note;

import java.util.List;

//this class is responsible for all the query for the database
//basically it is acting s a bridge between Note entities and Note database class
@Dao
public interface NoteDao {

    //here the notes will be read via id in descending order
    @Query("SELECT * FROM notes ORDER BY id DESC")
    List<Note> getAllNotes();

    //if there are any note present with same id then it should replace the old note with the new one
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNote(Note note);

    @Delete
    void deleteNote(Note note);

}
