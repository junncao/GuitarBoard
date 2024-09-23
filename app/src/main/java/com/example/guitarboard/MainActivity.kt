package com.example.guitarboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.guitarboard.ui.theme.ComposeAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeAppTheme {
                Surface() {
                    MainScreen()
                }
            }
        }
    }

}

// Standard tuning of a guitar
val standardTuning = listOf("E", "A", "D", "G", "B", "E")

// All notes in music
val allNotes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "Bb", "B")

// Chord types
val chordTypes = listOf("Major", "Minor", "Major 7", "Minor 7", "Dominant 7", "Dim7", "Minor7b5")

val modeTypes = listOf(ViewMode.FRETBOARD, ViewMode.FILL_CHORD, ViewMode.FIND_NOTES, ViewMode.FIND_SCALE)


val scaleMap = mapOf(
    "Major" to listOf(0, 2, 4, 5, 7, 9, 11),
    "Minor" to listOf(0, 2, 3, 5, 7, 8, 10),
    "MinorHarmonic" to listOf(0, 2, 3, 5, 7, 8, 11),
    "MajorBlues" to listOf(0, 2, 3, 4, 7, 9),
    "MinorBlues" to listOf(0, 3, 5, 6, 7, 10),
)

// Chord intervals (semitones from root)
val chordMap = mapOf(
    "Major" to listOf(0, 4, 7),
    "Minor" to listOf(0, 3, 7),
    "Major 7" to listOf(0, 4, 7, 11),
    "Minor 7" to listOf(0, 3, 7, 10),
    "Dominant 7" to listOf(0, 4, 7, 10),
    "Dim7" to listOf(0, 3, 6, 9),
    "Minor7b5" to listOf(0, 3, 6, 10)
) + scaleMap



val internalCompleteMap = mapOf(
    "1" to 0,    // 根音
    "b2" to 1,   // 小二度
    "2" to 2,    // 大二度
    "b3" to 3,   // 小三度
    "3" to 4,    // 大三度
    "4" to 5,    // 完全四度
    "b5" to 6,   // 减五度
    "5" to 7,    // 完全五度
    "b6" to 8,   // 小六度
    "6" to 9,    // 大六度
    "b7" to 10,  // 小七度
    "7" to 11    // 大七度
)
val reversedInternalMap = mapOf(
    0 to "1",    // 根音
    1 to "b2",   // 小二度
    2 to "2",    // 大二度
    3 to "b3",   // 小三度
    4 to "3",    // 大三度
    5 to "4",    // 完全四度
    6 to "b5",   // 减五度
    7 to "5",    // 完全五度
    8 to "b6",   // 小六度
    9 to "6",    // 大六度
    10 to "b7",  // 小七度
    11 to "7"    // 大七度
)
val fretPositionsToMark = listOf(0, 1, 3, 5, 7, 9, 12)


data class Note(val string: Int, val fret: Int)

fun getNoteFunction(note: String, rootNote: String, chordType: String): String {
    val noteIndex = allNotes.indexOf(note)
    val rootIndex = allNotes.indexOf(rootNote)
    val interval = (noteIndex - rootIndex + 12) % 12

    return reversedInternalMap[interval]!!
}

fun calculateChordNotes(root: String, chordType: String): List<String> {
    val rootIndex = allNotes.indexOf(root)
    return chordMap[chordType]?.map { interval ->
        allNotes[(rootIndex + interval) % 12]
    } ?: emptyList()
}

fun calculateInternalNotes(root: String, internalType: String): List<String> {
    val rootIndex = allNotes.indexOf(root)
    return internalCompleteMap[internalType]?.let { interval ->
        allNotes[(rootIndex + interval) % 12]
    }?.let { listOf(it) } ?: emptyList()
}

fun calculateScaleNotes(root: String, scaleType: String): List<String> {
    val rootIndex = allNotes.indexOf(root)
    return scaleMap[scaleType]?.map { interval ->
        allNotes[(rootIndex + interval) % 12]
    } ?: emptyList()
}

fun findNotesOnFretboard(chordNotes: List<String>): List<Note> {
    val notes = mutableListOf<Note>()
    standardTuning.forEachIndexed { stringIndex, openNote ->
        val openNoteIndex = allNotes.indexOf(openNote)
        for (fret in 0..12) {
            val currentNote = allNotes[(openNoteIndex + fret) % 12]
            if (currentNote in chordNotes) {
                notes.add(Note(stringIndex, fret))
            }
        }
    }
    return notes
}