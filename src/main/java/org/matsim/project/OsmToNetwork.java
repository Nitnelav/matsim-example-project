package org.matsim.project;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class OsmToNetwork {

    private static final String inputFile = "C:\\Users\\valen\\Documents\\IFSTTAR\\GitHub\\matsim-example-project\\scenarios\\lorient\\lorient_clean_01.pbf";
    private static final String outputFile = "C:\\Users\\valen\\Documents\\IFSTTAR\\GitHub\\matsim-example-project\\scenarios\\lorient\\lorient-network.xml.gz";
    private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:2154");

    public static void main(String[] args) {

        SupersonicOsmNetworkReader reader = new SupersonicOsmNetworkReader.Builder()
                .setCoordinateTransformation(coordinateTransformation)
                .build();
        reader.setPrintLinkGeometry(true);
        Network network = reader.read(inputFile);

        new NetworkCleaner().run(network);

        new NetworkWriter(network).write(outputFile);
    }
}
