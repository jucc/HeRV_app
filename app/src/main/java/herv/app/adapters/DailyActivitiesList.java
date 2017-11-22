/**
 * Lifegrep daily activities list
 * Juliana Cavalcanti - 2017/03
 */
package herv.app.adapters;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * DEPRECATED
**/
public class DailyActivitiesList {

    public static HashMap<String, List<String>> getData() {

        HashMap<String, List<String>> listDetail = new HashMap<String, List<String>>();

        List<String> laying = new ArrayList<String>();
        laying.add("sleeping");
        laying.add("resting");

        List<String> sitting = new ArrayList<String>();
        sitting.add("study/work");
        sitting.add("lecture watching");
        sitting.add("eating");
        sitting.add("playing");
        sitting.add("leisure (TV, read,...)");
        sitting.add("talking");
        sitting.add("driving");

        List<String> standing = new ArrayList<String>();
        standing.add("household chores");
        standing.add("waiting");
        standing.add("presenting lecture");
        standing.add("talking");

        List<String> moving = new ArrayList<String>();
        moving.add("walking (leisure, no hurry)");
        moving.add("walking briskly");
        moving.add("low intensity exercise");
        moving.add("high intensity exercise");

        listDetail.put("LAYING DOWN", laying);
        listDetail.put("SITTING", sitting);
        listDetail.put("STANDING", standing);

        listDetail.put("MOVING", moving);

        return listDetail;
    }
}
