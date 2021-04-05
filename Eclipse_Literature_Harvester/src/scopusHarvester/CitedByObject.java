/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package scopusHarvester;

/**
 *
 * @author tasosnent
 */
public class CitedByObject {
    private int count;
    private String link;

    public CitedByObject(){
        count = 0;
        link = "";
    }
    public CitedByObject(int citedBy, String citedByLink){
        this.count = citedBy;
        this.link = citedByLink;
    }       

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * @return the link
     */
    public String getLink() {
        return link;
    }

    /**
     * @param link the link to set
     */
    public void setLink(String link) {
        this.link = link;
    }
    
}
