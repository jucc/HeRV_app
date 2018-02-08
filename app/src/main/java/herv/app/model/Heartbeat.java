package herv.app.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Heartbeat implements Event {

    protected String user;
    protected Date dt;
    protected List<Integer> intervals; // more than one beat can be registered in the same second
    protected Integer heartrate;

    public final static String fmtDateDB = "yyyy-MM-dd HH:mm:ss";

    protected Heartbeat() {}

    public Heartbeat(String user, List<Integer> intervals) {
        this.user = user;
        this.intervals = intervals;
        this.dt = new Date();
    }

    public Heartbeat(String user, List<Integer> intervals, Date dt) {
        this.user = user;
        this.intervals = intervals;
        this.dt = dt;
    }

    public Heartbeat(List<Integer> intervals, Integer heartrate) {
        this.intervals = intervals;
        this.heartrate = heartrate;
        this.dt = new Date();
    }

    /**
     * Converts the event to the json format with beats from each second condensed
     * @return JSON = { "dt": dt, "user": userID, "intervals": [RR1, .. RRN] }
     */
    public JSONObject toJson() throws JSONException {

        JSONArray rr = new JSONArray();
        for (Integer interval : this.intervals ) {
            rr.put(interval);
        }

        //TODO get rid of JSONObject and use GSON or Jackson instead
        JSONObject event = new JSONObject();
        if (this.user != null) {
            event.put("user", this.user);
        }
        JSONObject heartbeat = new JSONObject();
        heartbeat.put("dt", new SimpleDateFormat(fmtDateDB).format(this.getDt()));
        heartbeat.put("intervals", rr);
        event.put("beat", heartbeat);
        return event;
    }

    /**
     * Converts the event to the csv format to be saved in a local file
     * User won't be included to save space and bandwidth (it would be the same user for all
     * events in a local file, so the server can process the events and add the user when saving)
     * @return list of arrays, one for each heartbeat found (usually 1 or 2)
     */
    public String toCSV() {
        String dt = new SimpleDateFormat(fmtDateDB).format(this.getDt());
        if (this.getIntervals() == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.intervals.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(dt);
            sb.append(", ");
            sb.append(this.intervals.get(i));
        }
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

    /**
     * String formatted to be shown on activity screen
     * @return
     */
    public String toScreenString() {

        StringBuilder data = new StringBuilder();
        if (this.heartrate != null)  {
            data.append("Heart Rate: ");
            data.append(heartrate);
            data.append("\n");
        }
        if (this.intervals != null) {
            data.append("Intervals: ");
            for (Integer beat : this.intervals) {
                data.append(beat);
                data.append(" ");
            }
        }
        return data.toString();
    }

    public String getUser() { return this.user; }

    public void setUser(String user) { this.user = user; }

    public Date getDt(){ return dt; }

    public void setDt(Date dt) { this.dt = dt; }

    public String getType() { return this.TP_HEARTBEAT; }

    public List<Integer> getIntervals() { return intervals; }

    public Integer getHeartRate() { return heartrate; }

    public List<Integer> getValue() {
        return this.intervals;
    }
}
