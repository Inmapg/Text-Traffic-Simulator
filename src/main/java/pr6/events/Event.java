package pr6.events;

import java.util.Map;
import pr6.ini.IniSection;
import pr6.model.RoadMap;
import pr6.view.Describable;

/**
 * Creates and executes the different types of events.
 */
public abstract class Event implements Comparable<Event>, Describable {

    private final Integer internalTime;

    /**
     * Class constructor specifying time
     *
     * @param internalTime
     */
    public Event(int internalTime) {
        this.internalTime = internalTime;
    }

    /**
     * Executes the event.
     *
     * @param roadmap Information about the current situation in the simulator
     */
    public abstract void execute(RoadMap roadmap);

    /**
     * Returns the time of the event.
     *
     * @return current internal time
     */
    public int getScheduleTime() {
        return internalTime;
    }

    @Override
    public int compareTo(Event e) {
        return internalTime.compareTo(e.getScheduleTime());
    }

    @Override
    public void describe(Map<String, String> out) {
        out.put("Time", "" + internalTime);
    }

    /**
     * Interface to build an event.
     */
    public static interface Builder {

        /**
         * Parses an Event given an IniSection.
         *
         * @param ini Information about the event
         * @return parsed event
         */
        public abstract Event parse(IniSection ini);

        /**
         * Parses an identification.
         *
         * @param sec Information about the event
         * @param key Identification value
         * @return valid id
         */
        default String parseString(IniSection sec, String key) {
            String v = sec.getValue(key);
            if (!v.matches("[a-zA-Z0-9_]+")) { 
                throw new IllegalArgumentException(
                         (v.isEmpty()) ?  "Not " + key + " provided" : 
                            v+" is not a valid " + key);
            }
            return v;
        }

        /**
         * Parses an integer value.
         *
         * @param sec Information about the event
         * @param key Identification word
         * @param minValue
         * @return Valid integer value
         */
        default int parseInt(IniSection sec, String key, int minValue) {
            String s = sec.getValue(key);
            if (s == null) {
                throw new NullPointerException("Error at parseInt() with key \""
                        + key + "\" in " + sec.getTag());
            }
            int v = Integer.parseInt(s);
            if (v < minValue) {
                throw new IllegalArgumentException(v + " is not a valid " + key);
            }
            return v;
        }

        /**
         * Parses a list of strings.
         *
         * @param sec Information about the event
         * @param key Identification word
         * @return List of correct identification words
         */
        default String[] parseStringList(IniSection sec, String key) {
            String[] v = sec.getValue(key).split("[, ]+");
            for (String c : v) {
                if (!c.matches("[a-zA-Z0-9_]+")) {
                    throw new IllegalArgumentException(c + " is not a valid id"
                            + " in the list " + key);
                }
            }
            return v;
        }

        /**
         * Parses a double value.
         *
         * @param sec Information about the event
         * @param key Identification word
         * @param min
         * @param max
         * @return Valid value
         */
        default double parseDouble(IniSection sec, String key, double min, double max) {
            double v = Double.parseDouble(sec.getValue(key));
            if (v < min || v > max) {
                throw new IllegalArgumentException(v + " is not a valid "
                        + key + " it must be contained in [" + min + ","
                        + max + "]");
            }
            return v;
        }

        /**
         * Parses a long value.
         *
         * @param sec Information about the event
         * @param key Identificacion word
         * @return Valid value
         */
        default long parseLongOrMills(IniSection sec, String key) {
            String parv = sec.getValue(key);
            Long v;
            if (parv == null) {
                v = System.currentTimeMillis();
            } else {
                v = Long.parseLong(parv);
                if (v < 0) {
                    throw new IllegalArgumentException(v + " is not a valid "
                            + key + " it must be positive");
                }
            }
            return v;
        }

    }

}
