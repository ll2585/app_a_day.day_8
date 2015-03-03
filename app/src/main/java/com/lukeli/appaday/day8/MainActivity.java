package com.lukeli.appaday.day8;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


public class MainActivity extends ActionBarActivity {
    TextView text;
    ArrayList<Word> words;
    ArrayList<Word> selected_words;
    int curIndex;
    boolean showingKorean;
    int wordsChosen = 7;
    String lastDownloadedString;
    private static final int SETTINGS_INFO = 1;
    boolean startKorean = true;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.word_text_view);

        // Allows use to track when an intent with the id TRANSACTION_DONE is executed
        // We can call for an intent to execute something and then tell use when it finishes
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FileService.TRANSACTION_DONE);

        // Prepare the main thread to receive a broadcast and act on it
        registerReceiver(downloadReceiver, intentFilter);

        if(savedInstanceState != null){
            words = savedInstanceState.getParcelableArrayList("DOWNLOADED_WORDS");
            curIndex = savedInstanceState.getInt("CURRENT_INDEX");
            selected_words = savedInstanceState.getParcelableArrayList("SAVED_WORDS");
            showingKorean = savedInstanceState.getBoolean("SHOWING_KOREAN");
            lastDownloadedString = savedInstanceState.getString("LAST_DOWNLOADED");

            startKorean = savedInstanceState.getBoolean("START_KOREAN");
            wordsChosen = savedInstanceState.getInt("WORDS_TO_GENERATE");

        }else {
            words = new ArrayList<Word>();
            selected_words = new ArrayList<Word>();
            curIndex = 0;
            showingKorean = true;
            lastDownloadedString = "Not downloaded yet.";
        }

        boolean preferencesSaved = getPreferences(Context.MODE_PRIVATE).getBoolean("PREFERENCES_SAVED", false);
        if(preferencesSaved){
            Set<String> wordSet = getPreferences(Context.MODE_PRIVATE).getStringSet("DOWNLOADED_WORDS", null);
            Set<String> selectedWordSet = getPreferences(Context.MODE_PRIVATE).getStringSet("SAVED_WORDS", null);
            words = arrayListFromSharedPreferences(wordSet);
            curIndex = getPreferences(Context.MODE_PRIVATE).getInt("CURRENT_INDEX", 0);
            selected_words = arrayListFromSharedPreferences(selectedWordSet);
            showingKorean = getPreferences(Context.MODE_PRIVATE).getBoolean("SHOWING_KOREAN", true);
            lastDownloadedString = getPreferences(Context.MODE_PRIVATE).getString("LAST_DOWNLOADED", null);

            wordsChosen = getPreferences(Context.MODE_PRIVATE).getInt("WORDS_TO_GENERATE", 7);
            startKorean = getPreferences(Context.MODE_PRIVATE).getBoolean("START_KOREAN", true);
        }

        ((TextView) findViewById(R.id.last_downloaded_text_view)).setText(lastDownloadedString);
        updateText();

    }

    private void updateText(){
        if(selected_words.size() > 0) {
            Word curWord = selected_words.get(curIndex);
            Log.d("DAY8", curWord.toString());
            String curSideShowing = showingKorean ? curWord.getKorean() : curWord.getDefinition();
            text.setText(curSideShowing);
        }
         ((TextView) findViewById(R.id.curCard)).setText(selected_words.size() > 0 ?
                 "Current Card: " + String.valueOf(curIndex+1) + "/" + String.valueOf(selected_words.size()) :
                "No cards generated");
        findViewById(R.id.next_button).setEnabled(selected_words.size() > 0 && curIndex != selected_words.size() - 1);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putParcelableArrayList("DOWNLOADED_WORDS", words);
        outState.putInt("CURRENT_INDEX", curIndex);
        outState.putParcelableArrayList("SAVED_WORDS", selected_words);
        outState.putBoolean("SHOWING_KOREAN", showingKorean);
        outState.putString("LAST_DOWNLOADED", lastDownloadedString);

        outState.putInt("WORDS_TO_GENERATE", wordsChosen);
        outState.putBoolean("START_KOREAN", startKorean);

        super.onSaveInstanceState(outState);
    }

    // 2. Will save key value pairs to SharedPreferences
    private void saveSettings(){

        // SharedPreferences allow you to save data even if the user kills the app
        // MODE_PRIVATE : Preferences shared only by your app
        // MODE_WORLD_READABLE : All apps can read
        // MODE_WORLD_WRITABLE : All apps can write
        // edit() allows us to enter key vale pairs
        SharedPreferences.Editor sPEditor = getPreferences(Context.MODE_PRIVATE).edit();

        Set<String> wordSet= new HashSet<String>();
        for (int i = 0; i < words.size(); i++) {
            wordSet.add(words.get(i).getJSONObject().toString());
        }

        sPEditor.putStringSet("DOWNLOADED_WORDS", wordSet);

        Set<String> selectedSet= new HashSet<String>();
        for (int i = 0; i < selected_words.size(); i++) {
            selectedSet.add(selected_words.get(i).getJSONObject().toString());
        }

        sPEditor.putStringSet("SAVED_WORDS", selectedSet);
        sPEditor.putInt("CURRENT_INDEX", curIndex);
        sPEditor.putBoolean("SHOWING_KOREAN", showingKorean);
        sPEditor.putBoolean("PREFERENCES_SAVED", true);
        sPEditor.putString("LAST_DOWNLOADED", lastDownloadedString);

        sPEditor.putInt("WORDS_TO_GENERATE", wordsChosen);
        sPEditor.putBoolean("START_KOREAN", startKorean);
        // Save the shared preferences
        sPEditor.commit();

    }

    // 2. Called if the app is forced to close
    @Override
    protected void onStop() {

        saveSettings();

        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){

        super.onActivityResult(requestCode, resultCode, data);

        // 3. Check that the intent with the id SETTINGS_INFO called here
        if(requestCode == SETTINGS_INFO){

            updateWithSettings();

        }

    }

    private void updateWithSettings(){

        // Shared key value pairs are here
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Check if the checkbox was clicked
        if(sharedPreferences.getBoolean("pref_text_bold", false)){
            // Set the text to bold
            text.setTypeface(null, Typeface.BOLD_ITALIC);
        } else {
            // If not checked set the text to normal
            text.setTypeface(null, Typeface.NORMAL);
        }

        // Get the value stored in the list preference or give a value of 16
        String textSizeStr = sharedPreferences.getString("pref_text_size", "16");

        // Convert the string returned to a float
        float textSizeFloat = Float.parseFloat(textSizeStr);

        // Set the text size for the EditText box
        text.setTextSize(textSizeFloat);


        startKorean = sharedPreferences.getBoolean("start_korean", true);
        wordsChosen = Integer.parseInt(sharedPreferences.getString("words_generated", "7"));

    }

    private ArrayList<Word> arrayListFromSharedPreferences(Set<String> set) {

        ArrayList<Word> items = new ArrayList<Word>();
        for (String s : set) {
            try {
                JSONObject jsonObject = new JSONObject(s);
                Word w = Word.parseFromJSON(jsonObject);

                items.add(w);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return items;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void downloadWords(View view) {

        // Create an intent to run the IntentService in the background
        Intent intent = new Intent(this, FileService.class);

        // Pass the URL that the IntentService will download from
        intent.putExtra("korean_url", "https://arcane-depths-3989.herokuapp.com/korean.txt");
        intent.putExtra("definition_url", "https://arcane-depths-3989.herokuapp.com/definitions.txt");

        // Start the intent service
        this.startService(intent);

    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        // Called when the broadcast is received
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.e("FileService", "Service Received");

            parseFileAndDisplay();

        }
    };

    public void parseFileAndDisplay(){

        ArrayList<String> koreanWords;
        ArrayList<String> definitions;
        koreanWords = readFileIntoArrayList("korean");
        definitions = readFileIntoArrayList("definition");

        for(int i = 0; i < koreanWords.size(); i++){
            String korean = koreanWords.get(i);
            String definition = definitions.get(i);
            Log.d("DEFS", definition);
            Log.d("WORDS", korean + " - " + definition);
            Word w = new Word(korean, definition);
            words.add(w);
        }

        TextView lastDownloadedTextView = (TextView) findViewById(R.id.last_downloaded_text_view);
        DateFormat dateFormat = new SimpleDateFormat("MMMM d yyyy HH:mm:ss");
        Date date = new Date();
        String currentTime = dateFormat.format(date);
        lastDownloadedString = "Last downloaded on " + currentTime;
        lastDownloadedTextView.setText(lastDownloadedString);
        generateWordsForFlashcards();

    }

    private ArrayList<String> readFileIntoArrayList(String fileName){
        ArrayList<String> temp = new ArrayList<String>();
        try {

            // Opens a stream so we can read from our local file
            FileInputStream fis = this.openFileInput(fileName);

            // Gets an input stream for reading data
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");

            // Used to read the data in small bytes to minimize system load
            BufferedReader bufferedReader = new BufferedReader(isr);

            // Read the data in bytes until nothing is left to read
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                temp.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return temp;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intentPreferences = new Intent(getApplicationContext(),
                    SettingsActivity.class);

            // 3. Start the activity and then pass results to onActivityResult
            startActivityForResult(intentPreferences, SETTINGS_INFO);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void flipCard(View view) {
        if(selected_words.size() > 0) {
            showingKorean = !showingKorean;
            updateText();
        }
    }

    public void nextCard(View view) {
        curIndex += 1;
        showingKorean = startKorean;
        updateText();
    }

    public void generateWords(View view) {
        generateWordsForFlashcards();
    }

    private void generateWordsForFlashcards(){
        selected_words = new ArrayList<Word>();
        Collections.shuffle(words);
        for (int i = 0; i < wordsChosen; i++) {
            // be sure to use Vector.remove() or you may get the same item twice
            selected_words.add(words.get(i));
        }
        curIndex = 0;
        showingKorean = startKorean;
        updateText();
    }
}
