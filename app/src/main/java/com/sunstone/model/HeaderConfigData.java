package com.sunstone.model;

public class HeaderConfigData {
    String attributeName;
    String attributeMeaning;
    boolean checked;
    int position;

    public HeaderConfigData(String name, String meaning, boolean b, int pos){
        this.attributeName = name;
        this.attributeMeaning = meaning;
        this.checked = b;
        this.position = pos;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeMeaning() {
        return attributeMeaning;
    }

    public void setAttributeMeaning(String attributeMeaning) {
        this.attributeMeaning = attributeMeaning;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
