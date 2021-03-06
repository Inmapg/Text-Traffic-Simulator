package pr6.model;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import pr6.events.Event;
import pr6.exception.SimulatorError;
import pr6.ini.Ini;
import pr6.util.MultiTreeMap;

/**
 * Simulates a system of vehicles driving through some roads and around
 * specified junctions.
 */
public class TrafficSimulator {

    private OutputStream output;
    /**
     * Map of events to be executed ordered by the time when they will be
     * executed
     */
    private MultiTreeMap<Integer, Event> mapOfEvents = new MultiTreeMap<>((a, b) -> a - b);
    /**
     * Internal counter, current number of ticks executed
     */
    private int ticks = 0;
    /**
     * Road map storing all the objects in the simulatation
     */
    private RoadMap roadMap = new RoadMap();
    /**
     * List of observers to be used during the GUI execution
     */
    private final List<TrafficSimulatorListener> listeners = new ArrayList<>();

    /**
     * Class Constructor specifying output stream.
     *
     * @param output
     */
    public TrafficSimulator(OutputStream output) {
        this.output = output;
    }

    /**
     * Given a certain SimulatedObject list it generates its report into a ini.
     *
     * @param ini
     * @param simObjectList
     */
    public void writeReport(Ini ini, List<? extends SimulatedObject> simObjectList) {
        simObjectList.forEach((sim) -> {
            ini.addSection(sim.generateReport(ticks));
        });
    }

    /**
     * Given a certain SimulatedObject it generates its report.
     *
     * @param simObject
     * @throws SimulatorError Thrown when there is a problem with the output
     */
    private void writeReport(SimulatedObject simObject) throws SimulatorError {
        try {
            simObject.generateReport(ticks).store(output);
            output.write('\n');
        } catch (IOException e) {
            throw new SimulatorError("Error with " + simObject.getClass()
                    + " while storing report...", e);
        }
    }

    /**
     * Main loop of the simulator. It executes the events for the current time,
     * invoke the method advance for roads and junctions and increases the
     * internal counter. Finally, it writes the report for all the objects in
     * the simulation.
     *
     * @param numberOfTicks Number of repetitions
     */
    public void run(int numberOfTicks) {
        int timeLimit = ticks + numberOfTicks;
        try {
            while (ticks < timeLimit) {
                // Execute the events for the current time
                advanceEvents();
                // Invoke method advance for roads
                roadMap.getRoads().forEach((Road r) -> r.advance());
                // Invoke method advance for junction
                roadMap.getJunctions().forEach((Junction j) -> j.advance());
                // Current time increases
                ticks++;
                // listeners are notified
                notifyAdvanced();
                // Write report
                if (output != null) {
                    roadMap.getJunctions().forEach((Junction j) -> writeReport(j));
                    roadMap.getRoads().forEach((Road r) -> writeReport(r));
                    roadMap.getVehicles().forEach((Vehicle v) -> writeReport(v));
                }
            }
        } catch (Exception e) {
            notifyError(new SimulatorError("Error in TrafficSimulator at "
                    + ticks + " time: \n-> " + e.getMessage(), e));
        }
    }

    /**
     * Adds a new event to the simulation. Given a new event it is added to the
     * list of events to be executed during the execution of the simulation.
     *
     * @param event
     */
    public void addEvent(Event event) {
        mapOfEvents.putValue(event.getScheduleTime(), event);
        notifyEventAdded();
    }

    /**
     * Sets to its initial value the simulation.
     */
    public void reset() {
        mapOfEvents = new MultiTreeMap<>((a, b) -> a - b);
        roadMap = new RoadMap();
        this.output = null;
        ticks = 0;
        notifyReset();
    }

    /**
     * Changes the output stream. It changes the object output to a new value of
     * OutputStream.
     *
     * @param output Output stream
     */
    public void setOutputStream(OutputStream output) {
        this.output = output;
    }

    /**
     * Adds a new simulator listener to the simulation.
     *
     * @param newListener
     */
    public void addSimulatorListener(TrafficSimulatorListener newListener) {
        listeners.add(newListener);
        UpdateEvent ue = new UpdateEvent(EventType.REGISTERED);
        SwingUtilities.invokeLater(() -> newListener.registered(ue));
    }

    /**
     * Removes a simulator listener from the simulation.
     *
     * @param newListener
     */
    public void removeSimulatorListener(TrafficSimulatorListener newListener) {
        listeners.remove(newListener);
    }

    /**
     * Notifies the listeners in case of resetting the simulator.
     */
    private void notifyReset() {
        listeners.forEach((l) -> {
            l.reset(new UpdateEvent(EventType.RESET));
        });
    }

    /**
     * Notifies the listeners in case of adding a new event to the simulator.
     */
    private void notifyEventAdded() {
        listeners.forEach((l) -> {
            l.newEvent(new UpdateEvent(EventType.NEW_EVENT));
        });
    }

    /**
     * Notifies the listeners in case of advancing the simulator.
     */
    private void notifyAdvanced() {
        listeners.forEach((l) -> {
            l.advanced(new UpdateEvent(EventType.ADVANCED));
        });
    }

    /**
     * Notifies the listeners when an error occurs during the simulation.
     */
    private void notifyError(SimulatorError e) {
        listeners.forEach((l) -> {
            l.error(new UpdateEvent(EventType.ERROR), e);
        });
    }

    /**
     * Advances the events for an specific time.
     */
    private void advanceEvents() {
        ArrayList<Event> eventsList = mapOfEvents.getOrDefault(ticks, null);
        if (eventsList != null) {
            eventsList.forEach((e) -> {
                try {
                    e.execute(roadMap);
                } catch (Exception ex) {
                    notifyError(new SimulatorError("The event \""
                            + e.getClass().getSimpleName()
                            + "\" cannot be proccesed", ex));
                }
            });
        }
    }

    /**
     * Interfece which provides a way of dealing with events and the execution
     * of a TrafficSimulator externally.
     */
    public interface TrafficSimulatorListener {

        /**
         * Used to register an event.
         *
         * @param updateEvent
         */
        public void registered(UpdateEvent updateEvent);

        /**
         * Used when the simulator has been reset.
         *
         * @param updateEvent
         */
        public void reset(UpdateEvent updateEvent);

        /**
         * Used when a new event occurs.
         *
         * @param updateEvent
         */
        public void newEvent(UpdateEvent updateEvent);

        /**
         * Used when the simulator has advanced.
         *
         * @param updateEvent
         */
        public void advanced(UpdateEvent updateEvent);

        /**
         * Used when an error occurs during the simulation.
         *
         * @param updateEvent
         * @param e Exception
         */
        public void error(UpdateEvent updateEvent, Exception e);
    }

    /**
     * Contains the different types of listeners.
     */
    public enum EventType {
        REGISTERED, RESET, NEW_EVENT, ADVANCED, ERROR
    };

    /**
     * Contains the information of the event.
     */
    public class UpdateEvent {

        private final EventType type;

        /**
         * Class constructor specifying the type of event.
         *
         * @param eventType
         */
        public UpdateEvent(EventType eventType) {
            this.type = eventType;
        }

        /**
         * @return the type of event
         */
        public EventType getEvent() {
            return type;
        }

        /**
         * @return the road map
         */
        public RoadMap getRoadMap() {
            return roadMap;
        }

        /**
         * @return the queue of events up to the current time
         */
        public List<Event> getEventQueue() {
            List<Event> list = new ArrayList(mapOfEvents.valuesList());
            int counter = 0;
            while (counter < list.size()) {
                if (list.get(counter).getScheduleTime() >= ticks) {
                    break;
                }
                counter++;
            }
            return mapOfEvents.valuesList().subList(counter,
                    mapOfEvents.valuesList().size());
        }

        /**
         * @return the current time
         */
        public int getCurrentTime() {
            return ticks;
        }
    }
}
