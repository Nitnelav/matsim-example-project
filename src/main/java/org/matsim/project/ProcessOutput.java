package org.matsim.project;

import org.locationtech.jts.geom.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.noise_planet.noisemodelling.propagation.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ProcessOutput {

    public static void main(String[] args) {
        String folder = "";
        if (args.length == 0) {
            folder = "C:\\Users\\valen\\Documents\\IFSTTAR\\GitHub\\matsim-example-project\\scenarios\\lorient\\output2";
        } else {
            folder = args[0];
        }

        String eventFile = folder + "\\output_events.xml.gz";
        String networkFile = folder + "\\output_network.xml.gz";
        String configFile = folder + "\\output_config.xml";

        Network network = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
        MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
        networkReader.readFile(networkFile);

        Map<Id<Link>, Link> links = (Map<Id<Link>, Link>) network.getLinks();

        EventsManager evMgr = EventsUtils.createEventsManager();
        ProcessOutputEventHandler evHandler = new ProcessOutputEventHandler();

        evHandler.initLinks((Map<Id<Link>, Link>) links);

        evMgr.addHandler(evHandler);

        MatsimEventsReader eventsReader = new MatsimEventsReader(evMgr);

        eventsReader.readFile(eventFile);

        GeometryFactory factory = new GeometryFactory();

        List<Geometry> linkGeometries = new ArrayList<Geometry>();
        List<Coordinate> receivers = new ArrayList<Coordinate>();

        boolean hasReceiver = false;
        
        try {
            FileWriter outFile = new FileWriter(folder + "\\analysis.csv");
            outFile.write(LinkStatStruct.getTableStringHeader() + "\n");
            for (Map.Entry<Id<Link>, LinkStatStruct> entry : evHandler.links.entrySet()) {
                Id<Link> linkId = entry.getKey();
                LinkStatStruct linkStatStruct = entry.getValue();
                outFile.write(linkStatStruct.toTableString() + "\n");
                System.out.println(linkStatStruct.toString());
                linkGeometries.add(factory.createLineString(linkStatStruct.getGeometry()));
                if (!hasReceiver) {
                    receivers.add(linkStatStruct.getGeometry()[0]);
                    hasReceiver = true;
                }
            }
            outFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Geometry envelope = factory.createGeometryCollection(GeometryFactory.toGeometryArray(linkGeometries)).getEnvelope();
        Coordinate[] coordinates = envelope.getCoordinates();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(coordinates[0], coordinates[2]);
        Envelope cellEnvelope2 = envelope.getEnvelopeInternal();

        //Create obstruction test object
        MeshBuilder mesh = new MeshBuilder();

        try {
            mesh.finishPolygonFeeding(cellEnvelope2);
        } catch (LayerDelaunayError layerDelaunayError) {
            layerDelaunayError.printStackTrace();
        }

        //Retrieve Delaunay triangulation of scene
        FastObstructionTest manager = new FastObstructionTest(mesh.getPolygonWithHeight(), mesh.getTriangles(),
                mesh.getTriNeighbors(), mesh.getVertices());

        PropagationProcessData rayData = new PropagationProcessData(manager);
        rayData.addReceiver(0, receivers.get(0));
        for (int i = 0; i < linkGeometries.size(); i++) {
            rayData.addSource((long) i, linkGeometries.get(i).getCentroid());
        }
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.maxSrcDist = 1500;
        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);

        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
        ComputeRays computeRays = new ComputeRays(rayData);


        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        try {
            GeoJSONDocument geoJSONDocument = new GeoJSONDocument(new FileOutputStream(folder + "\\out.geojson"));
            geoJSONDocument.writeHeader();
            for (PropagationPath path: propDataOut.getPropagationPaths()) {
                geoJSONDocument.writeRay(path);
            }
            geoJSONDocument.writeFooter();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < propDataOut.getVerticesSoundLevel().size(); i++) {
            ComputeRaysOut.VerticeSL lvl = propDataOut.getVerticesSoundLevel().get(i);
            System.out.println(Arrays.toString(lvl.value));
        }
    }
}
