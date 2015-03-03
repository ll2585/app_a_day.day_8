package com.lukeli.appaday.day8;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

public class Word implements Parcelable {
    private static final String KEY_WORD = "korean";
    private static final String KEY_DEFINITION = "definition";


    private String korean;
    private String definition;

    public String getKorean() {
        return korean;
    }

    public void setKorean(String korean) {
        this.korean = korean;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public Word(String korean, String definition){
        setKorean(korean);
        setDefinition(definition);
    }

    public Word(){

    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle bundle = new Bundle();

        bundle.putString(KEY_WORD, korean);
        bundle.putString(KEY_DEFINITION, definition);
        dest.writeBundle(bundle);
    }

    public static final Parcelable.Creator<Word> CREATOR = new Creator<Word>() {

        @Override
        public Word createFromParcel(Parcel source) {
            // read the bundle containing key value pairs from the parcel
            Bundle bundle = source.readBundle();
            // instantiate a person using values from the bundle
            return new Word(
                    bundle.getString(KEY_WORD),
                    bundle.getString(KEY_DEFINITION));
        }

        @Override
        public Word[] newArray(int size) {
            return new Word[size];
        }

    };

    public JSONObject getJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put(KEY_WORD, this.korean);
            obj.put(KEY_DEFINITION, this.definition);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
    public String toString() {
       return this.korean + " - " + this.definition;
    }

    public static Word parseFromJSON(JSONObject jsonObject) {
        try {
            String word = jsonObject.getString(KEY_WORD);
            String def = jsonObject.getString(KEY_DEFINITION);
            return new Word(word, def);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

}
