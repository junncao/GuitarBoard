package com.example.guitarboard

import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guitarboard.ui.theme.PurpleGrey80
import kotlin.math.roundToInt

/**
 * Created by caojun.24 on 8/12/24
 * @author caojun.24@bytedance.com
 */
@Composable
fun GuitarFretboard(
    rootNote: String,
    chordType: String,
    chordNotes: List<Note>,
    onNoteClick: (Note) -> Unit
) {
    Log.d(
        "FretBoard",
        "recompose, root: $rootNote chordType:$chordType chordNotes:${chordNotes.toList()}"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(900.dp)
            .width(300.dp)
            .padding(vertical = 15.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fretWidth = size.height / 13
                    val stringSpacing = size.width / 7
                    val string = ((offset.x / stringSpacing).roundToInt() - 1).coerceIn(0, 5)
                    val fret = (offset.y / fretWidth)
                        .toInt()
                        .coerceIn(0, 12)
                    Log.d("StudyTest", "offset: $offset string: $string fret: $fret")
                    onNoteClick(Note(string, fret))
                }
            }
    ) {
        val fretWidth = size.height / 13
        val stringSpacing = size.width / 7

        // Draw strings
        for (i in 0 until 6) {
            drawLine(
                Color.Gray,
                start = Offset(stringSpacing * (i + 1), 0f),
                end = Offset(stringSpacing * (i + 1), size.height),
                strokeWidth = (6 - i) * 0.5f + 1f
            )
        }

        // Draw frets
        for (i in 1 until 13) {
            drawLine(
                Color.DarkGray,
                start = Offset(stringSpacing, fretWidth * i),
                end = Offset(stringSpacing * 6, fretWidth * i),
                strokeWidth = 5f
            )
            // Add position markers for specified frets
            if (i in fretPositionsToMark) {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = PurpleGrey80.toArgb()
                        textSize = 20.dp.toPx()
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.nativeCanvas.drawText(
                        i.toString(),
                        stringSpacing - 70f,  // Adjust this value to position the text
                        fretWidth * i + fretWidth / 1.7f,
                        paint
                    )
                }
            }
        }

        // Draw chord notes
        chordNotes.forEach { note ->
            val openNote = standardTuning[note.string]
            val noteIndex = (allNotes.indexOf(openNote) + note.fret) % 12
            val noteName = allNotes[noteIndex]
            val noteFunction = getNoteFunction(noteName, rootNote, chordType)

            val noteColor = when {
                noteFunction.endsWith("1") -> Color.Red
                noteFunction.endsWith("3") -> Color.Gray
                noteFunction.endsWith("5") -> Color.Cyan
                noteFunction.endsWith("7") -> Color.Green
                else -> Color.LightGray
            }
            drawCircle(
                noteColor,
                radius = 60f,
                center = Offset(
                    stringSpacing * (note.string + 1),
                    fretWidth * (note.fret + 0.5f),
                )
            )
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = Color.Black.toArgb()
                    textSize = 12.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(
                    "$noteName($noteFunction)",
                    stringSpacing * (note.string + 1),
                    fretWidth * (note.fret + 0.5f) + 10,
                    paint
                )
            }
        }
    }
}

enum class ViewMode(val label: String = "") {
    FRETBOARD("FretBoard"),
    FILL_CHORD("FillChord"),
    FIND_NOTES("FindNotes")
}

@Composable
fun MainScreen() {
    var selectedRoot by remember { mutableStateOf(allNotes[0]) }
    var selectedChordType by remember { mutableStateOf(chordTypes[0]) }
    var selectedMode by remember { mutableStateOf(modeTypes[0]) }

    val expandedMode = remember {
        mutableStateOf(false)
    }
    val expandedRoot = remember {
        mutableStateOf(false)
    }
    val expandedChordType = remember {
        mutableStateOf(false)
    }

    var giveNote by remember { mutableStateOf(internalCompleteMap.keys.first()) }
    var findNote by remember { mutableStateOf(internalCompleteMap.keys.filter { it != "1" }.random()) }
    val needFindNotes by remember {
        derivedStateOf {
            findNotesOnFretboard(calculateInternalNotes(selectedRoot, findNote))
        }
    }
    var onNoteClick: (Note) -> Unit = {}


    var foundNotes by remember { mutableStateOf(mutableStateListOf<Note>()) }

    val chordNotes = calculateChordNotes(selectedRoot, selectedChordType)
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            CustomDropdownMenu(
                label = "Mode",
                selectedValue = selectedMode.label,
                expand = expandedMode,
                onExpandedChange = { expandedMode.value = !expandedMode.value },
                dropdownContent = {
                    modeTypes.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            onClick = {
                                expandedMode.value = false
                                selectedMode = mode
                            }
                        )
                    }
                },
                modifier = Modifier
                    .width(150.dp)
                    .padding(end = 4.dp)
            )

            CustomDropdownMenu(
                label = "RootNote",
                selectedValue = selectedRoot,
                expand = expandedRoot,
                onExpandedChange = { expandedRoot.value = !expandedRoot.value },
                dropdownContent = {
                    allNotes.forEach { note ->
                        DropdownMenuItem(
                            text = { Text(note) },
                            onClick = {
                                expandedRoot.value = false
                                selectedRoot = note
                            }
                        )
                    }
                },
                modifier = Modifier
                    .width(100.dp)
                    .padding(end = 4.dp)
            )

            if (selectedMode != ViewMode.FIND_NOTES) {
                CustomDropdownMenu(
                    label = "Type",
                    selectedValue = selectedChordType,
                    expand = expandedChordType,
                    onExpandedChange = { expandedChordType.value = !expandedChordType.value },
                    dropdownContent = {
                        chordTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    expandedChordType.value = false
                                    selectedChordType = type
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .width(140.dp)
                        .padding(end = 4.dp)
                )
            } else {
                Text(
                    text = "we show $giveNote \nplease find $findNote",
                    modifier = Modifier
                        .clickable {
                            findNote = randomInternal()
                        }
                        .align(Alignment.CenterVertically)
                        .padding(start = 5.dp)
                )
            }
        }


        when (selectedMode) {
            ViewMode.FRETBOARD -> {
                GuitarFretboard(
                    selectedRoot,
                    selectedChordType,
                    findNotesOnFretboard(chordNotes),
                    onNoteClick = onNoteClick
                )
            }

            ViewMode.FILL_CHORD -> {
                val initialNotes = findNotesOnFretboard(chordNotes)
                foundNotes.clear()
                onNoteClick = { note ->
                    if (note in initialNotes && note !in foundNotes) {
                        foundNotes.add(note)
                        if (foundNotes.size == initialNotes.size) {
                            Toast.makeText(
                                context,
                                "You found all chord notes!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                GuitarFretboard(
                    selectedRoot,
                    selectedChordType,
                    foundNotes,
                    onNoteClick = onNoteClick
                )

            }

            ViewMode.FIND_NOTES -> {
                val initialNotes = findNotesOnFretboard(
                    calculateInternalNotes(selectedRoot, giveNote)
                )

                foundNotes.clear()
                foundNotes.addAll(initialNotes)
                val initialSize = initialNotes.size

                val currentNeedFindNotes by rememberUpdatedState(needFindNotes)
                onNoteClick = { note ->
                    if (note in currentNeedFindNotes && note !in foundNotes) {
                        foundNotes.add(note)
                        if (foundNotes.size == initialSize + currentNeedFindNotes.size) {
                            Toast.makeText(
                                context,
                                "You found all chord notes!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                GuitarFretboard(
                    selectedRoot,
                    selectedChordType,
                    foundNotes,
                    onNoteClick = onNoteClick
                )
            }
        }


    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun CustomDropdownMenu(
    label: String,
    selectedValue: String,
    expand: MutableState<Boolean>,
    onExpandedChange: (Boolean) -> Unit,
    dropdownContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    // Root Note Dropdown
    ExposedDropdownMenuBox(
        expanded = expand.value,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            enabled = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expand.value) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .onFocusChanged {
                    if (it.isFocused) {
                        keyboardController?.hide()
                    }
                },
        )
        ExposedDropdownMenu(
            expanded = expand.value,
            onDismissRequest = { expand.value = false },
            content = dropdownContent
        )
    }
}

var lastNote: String? = null

fun randomInternal(): String {
    // Get a list of keys excluding the previous result
    val availableKeys = internalCompleteMap.keys.filter { it != "1" && it != lastNote }

    // Return if no other keys are available
    if (availableKeys.isEmpty()) return lastNote ?: "1"  // Fallback to default if needed

    // Randomly select a key from the available keys
    val newNote = availableKeys.random()

    // Update lastNote to the newNote
    lastNote = newNote

    // Return the newly selected note
    return newNote
}
