package com.sunstone.model;

import java.util.ArrayList;

public class LolanObjectToUpdate {

    private int[] pathToUpdate;
    private String valueToUpdate;
    private String lolanPathName;

    private ArrayList<LolanObjectToUpdate> objectsToUpdate = new ArrayList<>();

    public LolanObjectToUpdate(int[] pathToUpdate, String valueToUpdate, String lolanPathName) {
        this.pathToUpdate = pathToUpdate;
        this.valueToUpdate = valueToUpdate;
        this.lolanPathName = lolanPathName;
    }

    public int[] getPathToUpdate() {
        return pathToUpdate;
    }

    public void setPathToUpdate(int[] pathToUpdate) {
        this.pathToUpdate = pathToUpdate;
    }

    public String getValueToUpdate() {
        return valueToUpdate;
    }

    public void setvalueToUpdate(String valueToUpdate) {
        this.valueToUpdate = valueToUpdate;
    }

    public String getlolanPathName() {
        return lolanPathName;
    }

    public void setlolanPathName(String lolanPathName) {
        this.lolanPathName = lolanPathName;
    }

    public void addObjectToUpdate(LolanObjectToUpdate lolanObjectToUpdate){
        objectsToUpdate.add(lolanObjectToUpdate);
    }

    public ArrayList<LolanObjectToUpdate> getObjectsToUpdate(){
        return objectsToUpdate;
    }

    public ArrayList<int[]> getPathsToUpdate(){
        ArrayList<int[]> paths = new ArrayList();
        for(int i=0; i<objectsToUpdate.size(); i++){
            paths.add(objectsToUpdate.get(i).getPathToUpdate());
        }
        return paths;
    }

    public ArrayList<String> getValuesToUpdate(){
        ArrayList<String> values = new ArrayList();
        for(int i=0; i<objectsToUpdate.size(); i++){
            values.add(objectsToUpdate.get(i).getValueToUpdate());
        }
        return values;
    }

    public ArrayList<String> getPathNamesToUpdate(){
        ArrayList<String> paths = new ArrayList();
        for(int i=0; i<objectsToUpdate.size(); i++){
            paths.add(objectsToUpdate.get(i).getlolanPathName());
        }
        return paths;
    }

    public void clean(){
        objectsToUpdate.clear();
    }
}
