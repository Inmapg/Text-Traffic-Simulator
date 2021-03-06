package pr6.model;

import pr6.ini.IniSection;

/**
 * Defines a time slice junction.
 *
 * @see Junction
 */
public class TimeSliceJunction extends Junction {

    protected TimeSliceIncomingRoad currentRoad;

    /**
     * Contains information about an incoming time slice road to the junction.
     *
     * @see IncomingRoad
     */
    protected class TimeSliceIncomingRoad extends IncomingRoad {

        private int intervalTime;
        protected int timeSpent = 0;
        private boolean completelyUsed = false;
        private boolean used = false;

        /**
         * Class constructor specifying the road. The rest of attributes are
         * zero-initialized.
         *
         * @param road
         */
        public TimeSliceIncomingRoad(Road road) {
            super(road);
            intervalTime = 0;
        }

        /**
         * Class constructor specifying road and interval time.
         *
         * @param road
         * @param intervalTime
         */
        public TimeSliceIncomingRoad(Road road, int intervalTime) {
            super(road);
            this.intervalTime = intervalTime;
        }

        @Override
        protected void onGreenLight() {
            super.onGreenLight();
            completelyUsed = true;
            used = false;
            timeSpent = 0;
        }

        /**
         * @return if time is greater than the interval of time.
         */
        public final boolean timeIsOver() {
            return timeSpent >= intervalTime;
        }

        @Override
        protected void advanceFirstVehicle() {
            timeSpent++;
            int queueBefore = sizeOfQueue();
            super.advanceFirstVehicle();
            completelyUsed = queueBefore > sizeOfQueue() && completelyUsed;
            used = used || completelyUsed;
        }

        /**
         * @return If time completely used.
         */
        public final boolean completelyUsed() {
            return completelyUsed;
        }

        /**
         * @return If time is used.
         */
        public final boolean used() {
            return used;
        }

        /**
         * Sets the interval time.
         *
         * @param intervalTime
         */
        public void setIntervalTime(int intervalTime) {
            this.intervalTime = intervalTime;
        }

        /**
         * Returns the interval time.
         *
         * @return interval time
         */
        public int getIntervalTime() {
            return intervalTime;
        }

        /**
         * Resets time spent to zero.
         */
        public void reset() {
            timeSpent = 0;
        }
    } // End of the internal class TimeSliceIncomingRoad

    /**
     * Class constructor specifying the id.
     *
     * @param id
     */
    public TimeSliceJunction(String id) {
        super(id);
    }

    @Override
    protected IncomingRoad createIncomingRoadQueue(Road r) {
        return new TimeSliceIncomingRoad(r);
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

    @Override
    protected void fillReportDetails(IniSection sec) {
        StringBuilder sb = new StringBuilder();
        if (!incomingRoadMap.isEmpty()) {
            incomingRoadMap.values().forEach((ir) -> {
                if (ir.isGreenLight()) {
                    sb.append('(').
                            append(ir.road.getId()).append(',').append(ir.lightToString())
                            .append(":").append(
                            ((TimeSliceIncomingRoad) ir).getIntervalTime()
                            - ((TimeSliceIncomingRoad) ir).timeSpent)
                            .append(',').append(ir.printQueue())
                            .append("),");
                } else {
                    sb.append('(').append(ir.road.getId()).append(',')
                            .append(ir.lightToString()).append(',').append(ir.printQueue())
                            .append("),");
                }
            });
            sec.setValue("queues", sb.substring(0, sb.length() - 1));
        } else {
            sec.setValue("queues", "");
        }
    }
}
