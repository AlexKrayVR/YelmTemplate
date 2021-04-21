package yelm.io.extra_delicate.database.converters;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import yelm.io.extra_delicate.main.model.Modifier;

public class ModifiersConverter {
    private static Gson gson = new Gson();

    @TypeConverter
    public static List<Modifier> stringToList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<List<Modifier>>() {}.getType();
        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String ListToString(List<Modifier> someObjects) {
        return gson.toJson(someObjects);
    }
}
