package worldgenerator.geometry;

import java.util.ArrayList;

public final class SiteList implements Disposable {

    private ArrayList<Site> sites;
    private int index;
    private boolean sorted;

    public SiteList() {
        sites = new ArrayList<Site>();
        sorted = false;
    }

    @Override
    public void dispose() {
        if (sites != null) {
            for (Site site : sites) {
                site.dispose();
            }
            sites.clear();
            sites = null;
        }
    }

    public int push(Site site) {
        sorted = false;
        sites.add(site);
        return sites.size();
    }

    public int getLength() {
        return sites.size();
    }

    public Site next() {
        if (sorted == false) {
            throw new Error("SiteList::next():  sites have not been sorted");
        }
        if (index < sites.size()) {
            return sites.get(index++);
        } else {
            return null;
        }
    }

    public Rectangle getSitesBounds() {
        double xMIN, xMAX, yMIN, yMAX;
        
        if (sorted == false) {
            Site.sortSites(sites);
            index = 0;
            sorted = true;
        }
        
        if (sites.isEmpty()) {
            return new Rectangle(0, 0, 0, 0);
        }
        
        xMIN = Double.MAX_VALUE;
        xMAX = Double.MIN_VALUE;
        
        for (Site site : sites) {
            if (site.getX() < xMIN) {
                xMIN = site.getX();
            }
            if (site.getX() > xMAX) {
                xMAX = site.getX();
            }
        }
        yMIN = sites.get(0).getY();
        yMAX = sites.get(sites.size() - 1).getY();

        return new Rectangle(xMIN, yMIN, xMAX - xMIN, yMAX - yMIN);
    }
    
    public ArrayList<Point> siteCoords() {
        ArrayList<Point> coords = new ArrayList<Point>();
        for (Site site : sites) {
            coords.add(site.getCoordinates());
        }
        return coords;
    }

    public ArrayList<Circle> circles() {
        ArrayList<Circle> circles = new ArrayList<Circle>();
        for (Site site : sites) {
            double radius = 0;
            Edge nearestEdge = site.nearestEdge();
            if (!nearestEdge.isPartOfConvexHull()) {
                radius = nearestEdge.sitesDistance() * 0.5;
            }
            circles.add(new Circle(site.getX(), site.getY(), radius));
        }
        return circles;
    }

    public ArrayList<ArrayList<Point>> regions(Rectangle plotBounds) {
        ArrayList<ArrayList<Point>> regions = new ArrayList<ArrayList<Point>>();
        for (Site site : sites) {
            regions.add(site.region(plotBounds));
        }
        return regions;
    }
}