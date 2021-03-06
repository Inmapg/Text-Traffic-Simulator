package pr6.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import pr6.ini.IniSection;

/**
 * Defines one of the main types of Simulated Object.
 *
 * @see SimulatedObject
 */
public class Junction extends SimulatedObject {

    private static final String SECTION_TAG_NAME = "junction_report";

    /**
     * Contains information about an incoming road to the junction.
     */
    protected class IncomingRoad {

        private boolean greenLight;
        /**
         * Queue of vehicles
         *
         * @see Vehicle
         */
        private final ArrayDeque<Vehicle> waiting = new ArrayDeque<>();
        /**
         * Incoming road
         */
        protected final Road road;

        /**
         * Class consctructor specifying road. greenLight is false-initialized.
         *
         * @param road Incoming road
         */
        public IncomingRoad(Road road) {
            this.road = road;
            greenLight = false;
        }

        /**
         * Returns the state of the trafficlight.
         *
         * @return green/red
         */
        protected String lightToString() {
            return (greenLight) ? "green" : "red";
        }

        /**
         * Moves first vehile from queue to next road.
         */
        protected void advanceFirstVehicle() {
            Vehicle movingVehicle = waiting.pollFirst(); // returns null when empty
            if (movingVehicle == null) {
                // empty queue
            } else {
                movingVehicle.moveToNextRoad();
            }
        }

        /**
         * @return Size of queue
         */
        protected int sizeOfQueue() {
            return waiting.size();
        }

        /**
         * Turns green the trafficlight.
         */
        protected void onGreenLight() {
            greenLight = true;
        }

        /**
         * Turns red the trafficlight.
         */
        protected void offGreenLight() {
            greenLight = false;
        }

        /**
         * Returns if trafficlight is green/red.
         *
         * @return red - false / green - true
         */
        protected boolean isGreenLight() {
            return greenLight;
        }

        /**
         * Prints the current state of the queue.
         *
         * @return State of the queue
         */
        protected String printQueue() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            if (!waiting.isEmpty()) {
                waiting.forEach((v) -> {
                    sb.append(v.getId()).append(',');
                });
            }
            if (sb.length() > 1) {
                return sb.substring(0, sb.length() - 1) + ']';
            } else {
                return sb.append(']').toString();
            }

        }

        @Override
        public String toString() {
            return "(" + road.getId() + "," + (greenLight ? "green" : "red")
                    + printQueue() + ")";
        }

    } // End of the internal class IncomingRoad

    /**
     * Next road to turn it trafficlight green.
     *
     * @see Road
     */
    protected Iterator<Road> nextRoad;
    protected IncomingRoad currentRoad;
    protected IncomingRoad lastGreenLightRoad;

    /**
     * Associates roads with their respective incoming roads. LinkedHashMap used
     * to mantain insertion order.
     *
     * @see Road
     * @see IncomingRoad
     */
    protected Map<Road, IncomingRoad> incomingRoadMap = new LinkedHashMap<>();
    /**
     * Associates junctions with their respective outgoing roads.
     *
     * @see Road
     */
    private Map<Junction, Road> outgoingRoadMap = new HashMap<>();

    /**
     * Class constructor specifying id. The rest of attributes are
     * null-initialized.
     *
     * @param id
     */
    public Junction(String id) {
        super(id);
        lastGreenLightRoad = null;
        currentRoad = null;
        nextRoad = null;
    }

    @Override
    protected String getReportSectionTag() {
        return SECTION_TAG_NAME;
    }

    /**
     * Puts a vehicle in the junction.
     *
     * @param newVehicle
     */
    public void enter(Vehicle newVehicle) {
        incomingRoadMap.get(newVehicle.getRoad()).waiting.offer(newVehicle);
    }

    /**
     * Adds an incoming road to the junction.
     *
     * @param newRoad
     */
    public void addIncomingRoad(Road newRoad) {
        incomingRoadMap.put(newRoad, createIncomingRoadQueue(newRoad));
    }

    /**
     * Adds an outgoing road from the junction.
     *
     * @param newRoad
     * @param newJunction
     */
    public void addOutGoingRoad(Road newRoad, Junction newJunction) {
        outgoingRoadMap.put(newJunction, newRoad);
    }

    /**
     * Creates an incoming road queue.
     *
     * @param road
     * @return Incoming road queue
     */
    protected IncomingRoad createIncomingRoadQueue(Road road) {
        return new IncomingRoad(road);
    }

    /**
     * Returns the road that goes to the destination junction.
     *
     * @param destinationJunction
     * @return Road if found, null if not
     */
    public Road roadTo(Junction destinationJunction) {
        return outgoingRoadMap.get(destinationJunction);
    }

    @Override
    public void advance() {
        if (!incomingRoadMap.isEmpty()) {
            if (currentRoad != null) {
                currentRoad.advanceFirstVehicle();
            }
            switchLights();
        }
    }

    /**
     * Returns the next road on the incoming road map.
     *
     * @return next road
     */
    protected IncomingRoad getNextRoad() {
        if (nextRoad == null || !nextRoad.hasNext()) {
            nextRoad = incomingRoadMap.keySet().iterator();
        }
        return incomingRoadMap.get(nextRoad.next());
    }

    /**
     * Changes the trafficlights of the roads.
     */
    protected void switchLights() {
        currentRoad = getNextRoad();
        if (lastGreenLightRoad != null) {
            lastGreenLightRoad.offGreenLight();
        }
        currentRoad.onGreenLight();
        lastGreenLightRoad = currentRoad;
    }

    /**
     *
     * @param road
     * @return true if road's traffic light has green light, false if not
     */
    public boolean isTrafficLightOn(Road road) {
        return incomingRoadMap.get(road).isGreenLight();
    }

    @Override
    protected void fillReportDetails(IniSection sec) {
        StringBuilder sb = new StringBuilder();
        if (!incomingRoadMap.isEmpty()) {
            incomingRoadMap.values().forEach((ir) -> {
                sb.append('(')
                        .append(ir.road.getId()).append(',')
                        .append(ir.lightToString()).append(',')
                        .append(ir.printQueue())
                        .append("),");
            });
            sec.setValue("queues", sb.substring(0, sb.length() - 1));
        } else {
            sec.setValue("queues", "");
        }
    }

    @Override
    public void describe(Map<String, String> out) {
        super.describe(out);
        ArrayList<String> green = new ArrayList<>();
        ArrayList<String> red = new ArrayList<>();
        incomingRoadMap.values().forEach(ir -> {
            if (ir.greenLight) {
                green.add(ir.toString());
            } else {
                red.add(ir.toString());
            }
        });
        out.put("Green", "[" + String.join(",", green) + "]");
        out.put("Red", "[" + String.join(",", red) + "]");
    }
}
