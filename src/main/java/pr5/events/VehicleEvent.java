package pr5.events;

import pr5.exception.SimulatorError;
import pr5.ini.IniSection;
import pr5.model.RoadMap;
import pr5.model.Vehicle;

/**
 * Creates a new vehicle.
 */
public class VehicleEvent extends Event {

    protected final String id;
    protected final int maxSpeed;
    protected final String[] itinerary;

    /**
     * Class constructor specifying time, id, maximum speed and itinerary.
     *
     * @param time Time
     * @param id Identification value
     * @param maxSpeed Maximum speed
     * @param itinerary Itinerary
     */
    public VehicleEvent(int time, String id, int maxSpeed, String[] itinerary) {
        super(time);
        this.id = id;
        this.maxSpeed = maxSpeed;
        this.itinerary = itinerary;
    }

    @Override
    public void execute(RoadMap roadmap) {
        try {
            roadmap.addVehicle(new Vehicle(id, maxSpeed,
                    roadmap.getItinerary(itinerary)));
        } catch (SimulatorError e) {
            throw e;
        }
    }

    /**
     * Builds the vehicle event.
     *
     * @see Event.Builder
     */
    public static class Builder implements Event.Builder {

        @Override
        public Event parse(IniSection sec) {
            if (!"new_vehicle".equals(sec.getTag())) {
                return null;
            }
            return new VehicleEvent(
                    parseInt(sec, "time", 0),
                    parseString(sec, "id"),
                    parseInt(sec, "max_speed", 1),
                    parseStringList(sec, "itinerary")
            );
        }

    }
}
