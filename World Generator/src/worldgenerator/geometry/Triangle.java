package worldgenerator.geometry;

import java.util.ArrayList;

public final class Triangle {

    private ArrayList<Site> sites;

    public ArrayList<Site> getSites() {
        return sites;
    }

    public Triangle(Site a, Site b, Site c) {
        sites = new ArrayList<Site>();
        sites.add(a);
        sites.add(b);
        sites.add(c);
    }

    public void dispose() {
        sites.clear();
        sites = null;
    }
}