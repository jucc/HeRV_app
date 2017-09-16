package me.lifegrep.heart.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class Heartbeat {

    protected Integer user;
    protected Date dt;
    protected List<Integer> intervals; // more than one beat can be registered in the same second

    public final static String fmtDateDB = "yyyy-MM-dd HH:mm:ss";

    protected Heartbeat() {}

    // constructor without date (will use current date as value)
    public Heartbeat(Integer user, List<Integer> intervals) {
        this.user = user;
        this.intervals = intervals;
        this.dt = new Date();
    }

    // constructor with date
    public Heartbeat(Integer user, List<Integer> intervals, Date dt) {
        this.user = user;
        this.intervals = intervals;
        this.dt = dt;
    }

    /**
     * Converts the event to the json format expected for heartbeats
     * @return JSON = { "dt": dt, "user": userID, "values": [RR1, .. RRN] }
     * @throws JSONException
     */
    public JSONObject toJson () throws JSONException {

        //TODO get rid of JSONObject and use GSON or Jackson instead
        JSONObject event = new JSONObject();

        // build event with dt, type and value(s)
        event.put("user", this.user);
        event.put("dt", new SimpleDateFormat(fmtDateDB).format(this.getDt()));

        JSONArray rr = new JSONArray();
        for (Integer interval : this.intervals ) {
            rr.put(interval);
        }
        event.put("intervals", rr);

        return event;
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

    public Integer getUser() { return user; }

    public void setUser(Integer user) { this.user = user; }

    public Date getDt(){ return dt; }

    public void setDt(Date dt) { this.dt = dt; }

    public List<Integer> getIntervals() { return intervals; }

    public void setIntervals(List<Integer> intervals) { this.intervals = intervals; }
}
