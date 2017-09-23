package me.lifegrep.heart.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DailyActivity {

    protected int user;
    protected Date dt;
    protected String type;      // START or STOP

    protected String posture;
    protected String activityName;

    //TODO switch to Enum
    // POSTURE VALUES
    public final static String PT_MOVE = "moving";
    public final static String PT_STAND = "standing";
    public final static String PT_SIT = "sitting";
    public final static String PT_LIE = "lying down";

    public final static String fmtDateDB = "yyyy-MM-dd HH:mm:ss";

    protected DailyActivity() {}

    public DailyActivity(int user, String type, String activityName) {
        this.user = user;
        this.type = type;
        this.activityName = activityName;
        this.dt = new Date();
    }

    public DailyActivity(int user, String type, String activityName, String posture) {
        this.user = user;
        this.type = type;
        this.activityName = activityName;
        this.posture = posture;
        this.dt = new Date();
    }

    public DailyActivity(int user, String type, String activityName, String posture, Date dt) {
        this.user = user;
        this.type = type;
        this.activityName = activityName;
        this.posture = posture;
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

        // build event with dt, type and activityName(s)
        event.put("dt", new SimpleDateFormat(fmtDateDB).format(this.getDt()));
        event.put("type", this.type);
        event.put("posture", posture);
        event.put("activityName", activityName);

        // build data with event and user
        data.put("user", this.user);
        data.put("event", event);

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
        sb.append(this.getActivityName());
        sb.append(",");
        sb.append(this.getType());
        sb.append(",");
        sb.append(this.getPosture());
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

    public String getPosture() { return posture; }

    public Object getActivityName() {
        return activityName;
    }
}
