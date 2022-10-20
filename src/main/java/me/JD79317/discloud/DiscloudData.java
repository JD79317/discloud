package me.JD79317.discloud;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
@Getter
@AllArgsConstructor
public final class DiscloudData {
    private final Map<String, List<String>> urlPath;
    private final String key;
    private final String iv;
    private final long timeStamp;
    @Nullable private final String folderName;
    private final List<String> directories;
    @Setter
    private long size;

    public static DiscloudData deserialize(String string) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(string);
        return new DiscloudData(
                (Map<String, List<String>>) jsonObject.get("files"),
                (String) jsonObject.get("key"), (String) jsonObject.get("iv"),
                (Long) jsonObject.get("timeStamp"),
                (String) jsonObject.get("folderName"),
                (List<String>) jsonObject.get("directories"),
                (long) jsonObject.getOrDefault("size", 0L));
    }

    public String serialize() {
        JSONObject object = new JSONObject();
        object.put("files", urlPath);
        object.put("key", key);
        object.put("iv", iv);
        object.put("timeStamp", timeStamp);
        if (folderName != null) {
            object.put("folderName", folderName);
        }
        if (!directories.isEmpty()) {
            object.put("directories", directories);
        }
        object.put("size", size);
        return object.toJSONString();
    }
}
