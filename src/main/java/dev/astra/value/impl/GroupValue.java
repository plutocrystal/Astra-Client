package dev.astra.value.impl;

import dev.astra.value.Value;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupValue extends Value<List<Value<?>>> {
    private boolean expanded = false;

    public GroupValue(String name, Value<?>... children) {
        super(name, new ArrayList<Value<?>>());
        getValue().addAll(Arrays.asList(children));
    }

    public List<Value<?>> getChildren() {
        return getValue();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }
}
