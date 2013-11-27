package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.ast.Str;
import org.yinwang.pysonar.types.Type;

import java.util.HashSet;
import java.util.Set;


public class Binding implements Comparable<Object> {

    public enum Kind {
        ATTRIBUTE,    // attr accessed with "." on some other object
        CLASS,        // class definition
        CONSTRUCTOR,  // __init__ functions in classes
        FUNCTION,     // plain function
        METHOD,       // static or instance method
        MODULE,       // file
        PARAMETER,    // function param
        SCOPE,        // top-level variable ("scope" means we assume it can have attrs)
        VARIABLE      // local variable
    }


    private boolean isStatic = false;         // static fields/methods
    private boolean isSynthetic = false;      // auto-generated bindings
    private boolean isReadonly = false;       // non-writable attributes
    private boolean isDeprecated = false;     // documented as deprecated
    private boolean isBuiltin = false;        // not from a source file

    @NotNull
    private String name;     // unqualified name
    public Node origin;
    @NotNull
    private String qname;    // qualified name
    private Type type;       // inferred type
    public Kind kind;        // name usage context

    @NotNull
    private Set<Ref> refs;


    public Binding(@NotNull String id, Node origin, @NotNull Type type, @NotNull Kind kind) {
        this.name = id;
        this.origin = origin;
        this.qname = type.getTable().getPath();
        this.type = type;
        this.kind = kind;
        refs = new HashSet<>(1);
        Indexer.idx.registerBinding(this);
    }


    @NotNull
    public String getName() {
        return name;
    }


    public void setQname(@NotNull String qname) {
        this.qname = qname;
    }


    @NotNull
    public String getQname() {
        return qname;
    }


    public void addRef(Ref ref) {
        getRefs().add(ref);
    }


    public void setType(Type type) {
        this.type = type;
    }


    public Type getType() {
        return type;
    }


    public void setKind(Kind kind) {
        this.kind = kind;
    }


    public Kind getKind() {
        return kind;
    }


    public void markStatic() {
        isStatic = true;
    }


    public boolean isStatic() {
        return isStatic;
    }


    public void markSynthetic() {
        isSynthetic = true;
    }


    public boolean isSynthetic() {
        return isSynthetic;
    }


    public void markReadOnly() {
        isReadonly = true;
    }


    public boolean isReadOnly() {
        return isReadonly;
    }


    public boolean isDeprecated() {
        return isDeprecated;
    }


    public void markDeprecated() {
        isDeprecated = true;
    }


    public boolean isBuiltin() {
        return isBuiltin;
    }


    public void markBuiltin() {
        isBuiltin = true;
    }


    public Set<Ref> getRefs() {
        return refs;
    }


    @Nullable
    public String getFile() {
        return origin.getFile();
    }


    public int getIdentStart() {
        if (origin.isModule()) {
            return 0;
        } else {
            return origin.start;
        }
    }


    public int getIdentEnd() {
        if (origin.isModule()) {
            return 0;
        } else {
            return origin.end;
        }
    }


    public int getIdentLength() {
        return getIdentEnd() - getIdentStart();
    }


    private boolean isNameOfDefinition() {
        return (origin.getParent() != null &&
                origin.getParent().isDefinition() &&
                origin.getParent().getName() == origin);
    }


    public int getBodyStart() {
        if (isNameOfDefinition()) {
            return origin.getParent().start;
        } else {
            return origin.start;
        }
    }


    public int getBodyEnd() {
        if (isNameOfDefinition()) {
            return origin.getParent().end;
        } else {
            return origin.end;
        }
    }


    public Str getDocString() {
        return origin.getDocString();
    }


    // sorted by location
    public int compareTo(@NotNull Object o) {
        return getIdentStart() - ((Binding) o).getIdentStart();
    }


    @NotNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<Binding:");
        sb.append(":qname=").append(qname);
        sb.append(":type=").append(type);
        sb.append(":kind=").append(kind);
        sb.append(":refs=");
        if (getRefs().size() > 10) {
            sb.append("[");
            sb.append(refs.iterator().next());
            sb.append(", ...(");
            sb.append(refs.size() - 1);
            sb.append(" more)]");
        } else {
            sb.append(refs);
        }
        sb.append(">");
        return sb.toString();
    }

}
