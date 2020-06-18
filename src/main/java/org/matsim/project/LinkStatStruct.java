package org.matsim.project;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;
import org.noise_planet.noisemodelling.emission.EvaluateRoadSourceCnossos;
import org.noise_planet.noisemodelling.emission.RSParametersCnossos;

import java.util.*;

public class LinkStatStruct {

    private Map<String, Integer> vehicleCounter = new HashMap<String, Integer>();
    private Map<String, ArrayList<Double> > travelTimes = new HashMap<String, ArrayList<Double> >();
    private Map<Id<Vehicle>, Double> enterTimes = new HashMap<Id<Vehicle>, Double>();
    private Map<String, ArrayList<Double> > acousticLevels = new HashMap<String, ArrayList<Double> >();
    private Link link;
    private boolean DEN = false;

    public void vehicleEnterAt(Id<Vehicle> vehicleId, double time) {
        String timeString = getTimeString(time);
        if (!enterTimes.containsKey(vehicleId)) {
            enterTimes.put(vehicleId, time);
        }
    }
    public void vehicleLeaveAt(Id<Vehicle> vehicleId, double time) {
        String timeString = getTimeString(time);
        if (!travelTimes.containsKey(timeString)) {
            travelTimes.put(timeString, new ArrayList<Double>());
        }
        if (enterTimes.containsKey(vehicleId)) {
            double enterTime = enterTimes.get(vehicleId);
            travelTimes.get(timeString).add(time - enterTime);
            enterTimes.remove(vehicleId);
            incrementVehicleCount(timeString);
        }
    }
    public void incrementVehicleCount(String timeString) {
        if (!vehicleCounter.containsKey(timeString)) {
            vehicleCounter.put(timeString, 1);
            return;
        }
        vehicleCounter.put(timeString, vehicleCounter.get(timeString) + 1);
    }
    public int getVehicleCount(String timeString) {
        if (!vehicleCounter.containsKey(timeString)) {
            return 0;
        }
        return vehicleCounter.get(timeString);
    }
    public double getMeanTravelTime(String timeString) {
        if (!vehicleCounter.containsKey(timeString)) {
            return 0.0;
        }
        if (vehicleCounter.get(timeString) == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (int i = 0; i < travelTimes.get(timeString).size(); i++) {
            sum += travelTimes.get(timeString).get(i);
        }
        return (sum / vehicleCounter.get(timeString));
    }

    public double getMaxTravelTime(String timeString) {
        if (!vehicleCounter.containsKey(timeString)) {
            return 0.0;
        }
        double max = 0.0;
        for (int i = 0; i < travelTimes.get(timeString).size(); i++) {
            if (travelTimes.get(timeString).get(i) > max) {
                max = travelTimes.get(timeString).get(i);
            }
        }
        return max;
    }
    public double getMinTravelTime(String timeString) {
        if (!vehicleCounter.containsKey(timeString)) {
            return 0.0;
        }
        double min = -1;
        for (int i = 0; i < travelTimes.get(timeString).size(); i++) {
            if (min <= 0 || travelTimes.get(timeString).get(i) < min) {
                min = travelTimes.get(timeString).get(i);
            }
        }
        return min;
    }
    private String getTimeString(double time) {
        if (DEN) {
            String timeString = "D";
            if (time >= 6 * 3600 && time < 18 * 3600) {
                timeString = "D";
            }
            if (time >= 18 * 3600 && time < 22 * 3600) {
                timeString = "E";
            }
            if (time >= 22 * 3600 || time < 6 * 3600) {
                timeString = "N";
            }
            return timeString;
        } else {
            int start = (int) (time / 3600);
            return start + "_" + (start + 1);
        }
    }

    public void setLink(Link link) {
        this.link = link;
    }

    public double[] getSourceLevels(String timeString) {
        double vehicleCount = getVehicleCount(timeString);
        double averageSpeed = Math.round(3.6 * link.getLength() / getMeanTravelTime(timeString));

        int[] freqs = {63, 125, 250, 500, 1000, 2000, 4000, 8000};
        double[] result = new double[freqs.length];
        for (int i = 0; i < freqs.length; i++) {
            RSParametersCnossos rsParametersCnossos = new RSParametersCnossos(
                    averageSpeed,0.0,0.0,0.0,0.0,
                    vehicleCount,0.0,0.0,0.0,0.0,
                    freqs[i],20.0,"NL08",0.0,0.0,
                    100,2);

            result[i] = EvaluateRoadSourceCnossos.evaluate(rsParametersCnossos);
        }
        return result;
    }
    public Coordinate[] getGeometry() {
        if (link.getAttributes().getAsMap().containsKey("geometry")) {
            Coord[] coords = ((Coord[]) link.getAttributes().getAttribute("geometry"));
            Coordinate[] result = new Coordinate[coords.length];
            for (int i = 0; i < coords.length; i++) {
                result[i] = new Coordinate(coords[i].getX(), coords[i].getY());
            }
            return result;
        } else {
            Coordinate[] result = new Coordinate[2];
            result[0] = new Coordinate(
                    link.getFromNode().getCoord().getX(),
                    link.getFromNode().getCoord().getY()
            );
            result[1] = new Coordinate(
                    link.getToNode().getCoord().getX(),
                    link.getToNode().getCoord().getY()
            );
            return result;
        }
    }
    public String getGeometryString() {
        if (link.getAttributes().getAsMap().containsKey("geometry")) {
            Coord[] coords = ((Coord[]) link.getAttributes().getAttribute("geometry"));
            String result = "MULTILINESTRING ((";
            for (int i = 0; i < coords.length; i++) {
                if (i > 0) {
                    result += ", ";
                }
                result += coords[i].getX() + " " + coords[i].getY();
            }
            result += "))";
            return result;
        } else {
            String result = "MULTILINESTRING ((";
            result += link.getFromNode().getCoord().getX() + " " + link.getFromNode().getCoord().getY();
            result += ", ";
            result += link.getToNode().getCoord().getX() + " " + link.getToNode().getCoord().getY();
            result += "))";
            return result;
        }
    }
    public String getOsmId() {
        if (link.getAttributes().getAsMap().containsKey("origid")) {
            return link.getAttributes().getAttribute("origid").toString();
        } else if (link.getId().toString().contains("_")) {
            return link.getId().toString().split("_")[0];
        } else {
            return String.valueOf(Long.parseLong(link.getId().toString()) / 1000);
        }
    }
    public String toString() {
        String out = "";
        out += "Link Id : " + link.getId() + " ----------- \n";
        out += "Osm Id : " + getOsmId() + "\n";
        out += "Geometry : " + getGeometryString() + "\n";
        String[] den = {"D", "E", "N"};
        String[] clock = new String[24];
        for (int i = 0; i < 24; i++) {
            clock[i] = (i + "_" + (i+1));
        }
        String[] timeStrings = DEN ? den : clock;
        for (String timeString : timeStrings) {
            out += ("\tTime : " + timeString + " ----------- \n");
            out += ("\t\tVehicle Counter : " + getVehicleCount(timeString) + "\n");
            if (getVehicleCount(timeString) != 0) {
                int[] freqs = {63, 125, 250, 500, 1000, 2000, 4000, 8000};
                out += ("\t\tLw : [");
                double[] levels = getSourceLevels(timeString);
                for (int i = 0; i < levels.length; i++) {
                    if (i > 0) {
                        out += ", ";
                    }
                    out += String.format(Locale.ROOT,"%.2f", levels[i]);
                }
                out += ("]\n");
                //outFile.write("\t\tTravel Times : " + linkStatStruct.travelTimes.toString() + "\n");
                //System.out.println("\t\tTravel Times : " + linkStatStruct.travelTimes.toString() + "");
                out += ("\t\tMean Travel Times : " + Math.round(getMeanTravelTime(timeString)) + " seconds\n");
                out += ("\t\tLength : " + link.getLength() + " meters\n");
                out += ("\t\tMin Speed : " + Math.round(3.6 * link.getLength() / getMaxTravelTime(timeString)) + " km/h\n");
                out += ("\t\tMean Speed : " + Math.round(3.6 * link.getLength() / getMeanTravelTime(timeString)) + " km/h\n");
                out += ("\t\tMax Speed : " + Math.round(3.6 * link.getLength() / getMinTravelTime(timeString)) + " km/h\n");
                out += ("\t\tSpeed Limit : " + Math.round(3.6 * link.getFreespeed()) + " km/h\n");
            }
        }
        return out;
    }
    public static String getTableStringHeader() {
        String out = "";
        out += "LINK_ID\t";
        out += "OSM_ID\t";
        out += "THE_GEOM\t";
        out += "LV_D\t";
        out += "LV_E\t";
        out += "LV_N\t";
        out += "LV_SPD_D\t";
        out += "LV_SPD_E\t";
        out += "LV_SPD_N\t";
        return out;
    }
    public String toTableString() {
        String out = "";
        out += link.getId() + "\t";
        out += getOsmId() + "\t";
        out += getGeometryString() + "\t";
        String[] den = {"D", "E", "N"};
        String[] clock = new String[24];
        for (int i = 0; i < 24; i++) {
            clock[i] = (i + "_" + (i+1));
        }
        String[] timeStrings = DEN ? den : clock;
        for (String timeString : timeStrings) {
            out += (getVehicleCount(timeString) + "\t");
        }
        for (String timeString : timeStrings) {
            if (getVehicleCount(timeString) != 0) {
                out += (Math.round(3.6 * link.getLength() / getMeanTravelTime(timeString)) + "\t");
            } else {
                out += "0\t";
            }
        }
        return out;
    }
}
