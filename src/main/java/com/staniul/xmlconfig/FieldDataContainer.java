package com.staniul.xmlconfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FieldDataContainer {
    private Field field;
    private String entry;
    private List<?> values;

    public FieldDataContainer(Field field) {
        this(field, field.getName());
    }

    public FieldDataContainer(Field field, String name) {
        this.field = field;
        this.entry = name;
        values = new ArrayList<>();
    }

    public Field getField() {
        return field;
    }

    public String getEntry() {
        return entry;
    }

    public List<?> getValues() {
        return values;
    }

    public void setValues (List<?> list) {
        values = list;
    }
}
