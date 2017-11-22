package herv.app.model;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Date;


public interface Event {

    public final static String fmtDateDB = "yyyy-MM-dd HH:mm:ss";

    //TODO switch to Enum
    public final static String TP_HEARTBEAT = "heartbeat";
    public final static String TP_START = "start";
    public final static String TP_STOP = "stop";
    public final static String TP_EVENT = "event";

    /**
     * Converts the event to the json format expected in lifegrep
     * @return JSON =
     * {
     *   "event": {"dt": dt, "type": type, "value1": value1, ..., "valuen":valuen}
     *   "user": user
     * }
     * @throws JSONException
     */
    public abstract JSONObject toJson() throws JSONException;

    /**
     * Converts the event to the csv format to be saved in a local file
     * Notice user won't be included to save space and bandwidth (it would be the same user for all
     * events in a local file, so the server can process the events and add the user when saving)
     * @return dt, event
     */
    public String toCSV();

    public Integer getUser();

    public String getType();

    public Date getDt();

}

