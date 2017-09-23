package me.lifegrep.heart.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Heartbeat implements Event {

    protected Integer user;
    protected Date dt;
    protected List<Integer> intervals; // more than one beat can be registered in the same second
    protected Integer heartrate;

    public final static String fmtDateDB = "yyyy-MM-dd HH:mm:ss";

    protected Heartbeat() {}

    public Heartbeat(Integer user, List<Integer> intervals) {
        this.user = user;
        this.intervals = intervals;
        this.dt = new Date();
    }

    public Heartbeat(Integer user, List<Integer> intervals, Date dt) {
        this.user = user;
        this.intervals = intervals;
        this.dt = dt;
    }

    public Heartbeat(Integer user, Integer interval) {
        this.user = user;
        this.intervals = new ArrayList<Integer>();
        intervals.add(interval);
        this.dt = new Date();
    }

    public Heartbeat(Integer user, Integer interval, Date dt) {
        this.user = user;
        this.intervals = new ArrayList<Integer>();
        intervals.add(interval);
        this.dt = dt;
    }

    public Heartbeat(List<Integer> intervals, Integer heartrate) {
        this.intervals = intervals;
        this.heartrate = heartrate;
        this.dt = new Date();
    }

    /**
     * Converts the event to the json format with beats from each second condensed
     * @return JSON = { "dt": dt, "user": userID, "values": [RR1, .. RRN] }
     */
    public JSONObject toJsonPerSecond () throws JSONException {

        //TODO get rid of JSONObject and use GSON or Jackson instead
        JSONObject event = new JSONObject();

        // build event with dt, type and value(s)
        if (this.user != null) {
            event.put("user", this.user);
        }
        event.put("dt", new SimpleDateFormat(fmtDateDB).format(this.getDt()));

        JSONArray rr = new JSONArray();
        for (Integer interval : this.intervals ) {
            rr.put(interval);
        }
        event.put("intervals", rr);

        return event;
    }


    /**
     * Converts the event to the json format where each RR interval is an object
     * @return JSON = { "dt": dt, "user": userID, "values": [RR1, .. RRN] }
     */
    public JSONObject toJson() throws JSONException {

        //TODO get rid of JSONObject and use GSON or Jackson instead
        JSONObject event = new JSONObject();

        // build event with dt, type and value(s)
        if (this.user != null) {
            event.put("user", this.user);
        }
        event.put("dt", new SimpleDateFormat(fmtDateDB).format(this.getDt()));
        event.put("interval", this.getIntervals().get(0));

        return event;
    }

    /**
     * Converts the event to the csv format to be saved in a local file
     * Notice user won't be included to save space and bandwidth (it would be the same user for all
     * events in a local file, so the server can process the events and add the user when saving)
     * @return list of arrays, one for each heartbeat found (usually 1 or 2)
     */
    public List<String> toCSVPerSecond() {
        List<String> res = new ArrayList<String>();
        String dt = new SimpleDateFormat(fmtDateDB).format(this.getDt());
        for (int i : this.getIntervals()) {
            StringBuilder sb = new StringBuilder();
            sb.append(dt);
            sb.append(", ");
            sb.append(i);
            res.add(sb.toString());
        }
        return res;
    }


    /**
     * Converts the event to the csv format to be saved in a local file
     * Notice user won't be included to save space and bandwidth (it would be the same user for all
     * events in a local file, so the server can process the events and add the user when saving)
     * @return list of arrays, one for each heartbeat found (usually 1 or 2)
     */
    public String toCSV() {
        String dt = new SimpleDateFormat(fmtDateDB).format(this.getDt());
        StringBuilder sb = new StringBuilder();
        for (int i =0; i <  intervals.size();  i++) {
            sb.append(dt);
            sb.append(",");
            sb.append(intervals.get(i));
            if (i != intervals.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }


    @Override
    public String toString() {
        try {
            return this.toJsonPerSecond().toString();
        } catch (JSONException ex) {
            ex.printStackTrace();
            return "bad JSON format";
        }
    }


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

    public Integer getUser() { return user; }

    public void setUser(Integer user) { this.user = user; }

    public Date getDt(){ return dt; }

    public void setDt(Date dt) { this.dt = dt; }

    public Integer getValue() { return this.intervals.get(0); }

    public String getType() { return this.TYPE_HEARTBEAT; }

    public List<Integer> getIntervals() { return intervals; }

    public void setIntervals(List<Integer> intervals) { this.intervals = intervals; }

    public Integer getHeartrate() { return heartrate; }

    public void setHeartrate(Integer heartrate) { this.heartrate = heartrate; }

}
