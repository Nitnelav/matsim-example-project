package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class ProcessOutputEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

    Map<Id<Link>, LinkStatStruct> links = new HashMap<Id<Link>, LinkStatStruct>();

    @Override
    public void handleEvent(LinkEnterEvent event) {
        // System.out.println("Link Entered ! " + event.toString());

        Id<Link> linkId = event.getLinkId();
        Id<Vehicle> vehicleId = event.getVehicleId();
        double time = event.getTime();

        if (!links.containsKey(linkId)) {
            LinkStatStruct stats = new LinkStatStruct();
            links.put(linkId, stats);
        }

        LinkStatStruct stats = links.get(linkId);

        stats.vehicleEnterAt(vehicleId, time);

        links.put(linkId, stats);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        // System.out.println("Link Leaved ! " + event.toString());

        Id<Link> linkId = event.getLinkId();
        Id<Vehicle> vehicleId = event.getVehicleId();

        double time = event.getTime();

        if (!links.containsKey(linkId)) {
            LinkStatStruct stats = new LinkStatStruct();
            links.put(linkId, stats);
        }

        LinkStatStruct stats = links.get(linkId);
        stats.vehicleLeaveAt(vehicleId, time);
        links.put(linkId, stats);
    }

    public void initLinks(Map<Id<Link>, Link> netLinks) {
        for (Map.Entry<Id<Link>, Link> entry: netLinks.entrySet()) {
            Id<Link> linkId = entry.getKey();
            Link link = entry.getValue();

            if (!links.containsKey(linkId)) {
                LinkStatStruct stats = new LinkStatStruct();
                stats.setLink(link);
                links.put(linkId, stats);
            }
        }
    }
}
