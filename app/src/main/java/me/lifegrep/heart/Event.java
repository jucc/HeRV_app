package me.lifegrep.heart;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Event {

    protected String user;
    protected String type;
    protected Date dt;
    protected Map<String, String> values;

    public final static String fmt = "yyyy-MM-dd HH:mm:ss";

    protected Event() {}

    // Basic constructors (with and without date)

    public Event(String user, String type) {
        this.user = user;
        this.type = type;
        this.values = new HashMap<String, String>();
        this.dt = new Date();
    }

    public Event(String user, String type, Date dt) {
        this.user = user;
        this.type = type;
        this.values = new HashMap<String, String>();
        this.dt = dt;
    }

    // Constructors with a single string as value (will default to key="value")

    public Event(String user, String type, String value) {
        this.user = user;
        this.type = type;
        this.values = new HashMap<String, String>();
        this.values.put("value", value);
        this.dt = new Date();
    }

    public Event(String user, String type, String value, Date dt) {
        this.user = user;
        this.type = type;
        this.values = new HashMap<String, String>();
        this.values.put("value", value);
        this.dt = dt;
    }

    // Constructors with the value map already setup

    public Event(String user, String type, Map values) {
        this.user = user;
        this.type = type;
        this.values = values;
        this.dt = new Date();
    }

    public Event(String user, String type, Map values, Date dt) {
        this.user = user;
        this.type = type;
        this.values = values;
        this.dt = dt;
    }

    // encapsulating map behaviour

    public void addData(String key, String value) {
        this.values.put(key, value);
    }

    public String getData(String key) {
        return this.values.get(key);
    }

    /**
     * Converts the event to the json format expected in lifegrep
     * @return JSON =
     * {
     *   "event": {"dt": dt, "type": type, "value1": value1, ..., "valuen":valuen}
     *   "user": user
     * }
     * @throws JSONException
     */
    public JSONObject toJson () throws JSONException {

        //TODO get rid of JSONObject and use GSON or Jackson instead
        JSONObject data = new JSONObject();
        JSONObject event = new JSONObject();

        // build event with dt, type and value(s)
        event.put("type", this.type);
        event.put("dt", new SimpleDateFormat(fmt).format(this.getDt()));
        for (Map.Entry<String, String> entry: values.entrySet()) {
            event.put(entry.getKey(), entry.getValue().toString());
        }

        // build data with event and user
        data.put("event", event);
        data.put("user", this.user);

        return data;
    }

    @Override
    public String toString() {
        try {
            return this.toJson().toString();
        } catch (JSONException ex) {
            ex.printStackTrace();
            return "bad JSON format";
        }
    }

    public String getUser() {
        return user;
    }

    public String getType() {
        return type;
    }

    public Date getDt() {
        return dt;
    }

    public void setDt(Date dt) {
        this.dt = dt;
    }

    public Map<String, String> getValues() {
        return values;
    }
}
