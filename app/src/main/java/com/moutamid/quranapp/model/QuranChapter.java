package com.moutamid.quranapp.model;

public class QuranChapter {
    // Instance variables
    public int index; // The chapter number
    public String title; // The chapter name in English

    // Constructor
    public QuranChapter(int index, String title) {
        this.index = index;
        this.title = title;
    }

    // Getter methods
    public int getIndex() {
        return index;
    }

    public String getTitle() {
        return title;
    }

    // toString method
    public String toString() {
        return "Chapter " + index + ": " + title;
    }
}
