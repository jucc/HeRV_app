package me.lifegrep.heart.model;

/**
 * Created with IntelliJ IDEA.
 * User: ju
 * Date: 3/30/13
 */

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LifeEvent {

    protected int user;
    protected String type;
    protected Date dt;
    protected Object value;

    public final static String fmtDateDB = "yyyy-MM-dd HH:mm:ss";

    protected LifeEvent() {}

    public LifeEvent(int user, String type, Object value) {
        this.user = user;
        this.type = type;
        this.value = value;
        this.dt = new Date();
    }

    public LifeEvent(int user, String type, Object value, Date dt) {
        this.user = user;
        this.type = type;
        this.value = value;
        this.dt = dt;
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
        event.put("dt", new SimpleDateFormat(fmtDateDB).format(this.getDt()));
        event.put("value", value);

        // build data with event and user
        data.put("event", event);
        data.put("user", this.user);

        return data;
    }


    /**
     * Converts the event to the csv format to be saved in a local file
     * Notice user won't be included to save space and bandwidth (it would be the same user for all
     * events in a local file, so the server can process the events and add the user when saving)
     * @return dt, event
     */
    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append(new SimpleDateFormat(fmtDateDB).format(this.getDt()));
        sb.append(",");
        sb.append(this.getValue());
        sb.append(",");
        sb.append(this.getType());
        return sb.toString();
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

    public int getUser() {
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

    public Object getValue() {
        return value;
    }
}
