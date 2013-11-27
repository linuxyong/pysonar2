package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Scope;


public class ClassType extends Type {

    private String name;
    private InstanceType canon;
    private Type superclass;


    public ClassType(@NotNull String name, @Nullable Scope parent) {
        this.name = name;
        this.setTable(new Scope(parent, Scope.ScopeType.CLASS));
        this.getTable().setType(this);
        if (parent != null) {
            this.getTable().setPath(parent.extendPath(name));
        } else {
            this.getTable().setPath(name);
        }
    }


    public ClassType(@NotNull String name, Scope parent, @Nullable ClassType superClass) {
        this(name, parent);
        if (superClass != null) {
            addSuper(superClass);
        }
    }


    public void setName(String name) {
        this.name = name;
    }


    public String getName() {
        return name;
    }


    public void addSuper(@NotNull Type superclass) {
        this.superclass = superclass;
        getTable().addSuper(superclass.getTable());
    }


    public InstanceType getCanon() {
        if (canon == null) {
            canon = new InstanceType(this, null, null);
        }
        return canon;
    }


    @Override
    public boolean equals(Object other) {
        return this == other;
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(getName()).append(">");
        return sb.toString();
    }
}
