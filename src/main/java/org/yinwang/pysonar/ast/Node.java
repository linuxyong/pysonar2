package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.ArrayList;
import java.util.List;


public abstract class Node implements java.io.Serializable {

    public int start = -1;
    public int end = -1;

    @Nullable
    protected Node parent = null;


    public Node() {
    }


    public Node(int start, int end) {
        this.start = start;
        this.end = end;
    }


    public void setParent(Node parent) {
        this.parent = parent;
    }


    @Nullable
    public Node getParent() {
        return parent;
    }


    @NotNull
    public Node getAstRoot() {
        if (parent == null) {
            return this;
        }
        return parent.getAstRoot();
    }


    public int length() {
        return end - start;
    }


    @Nullable
    public String getFile() {
        return parent != null ? parent.getFile() : null;
    }


    public void addChildren(@NotNull Node... nodes) {
        for (Node n : nodes) {
            if (n != null) {
                n.setParent(this);
            }
        }
    }


    public void addChildren(@Nullable List<? extends Node> nodes) {
        if (nodes != null) {
            for (Node n : nodes) {
                if (n != null) {
                    n.setParent(this);
                }
            }
        }
    }


    @Nullable
    public Str getDocString() {
        // find body if there is any
        Node body = null;
        if (this instanceof FunctionDef) {
            body = asFunctionDef().body;
        } else if (this instanceof ClassDef) {
            body = asClassDef().body;
        } else if (this instanceof Module) {
            body = asModule().body;
        }

        // find the first string in the body
        if (body instanceof Block && body.asBlock().seq.size() >= 1) {
            Node firstExpr = body.asBlock().seq.get(0);
            if (firstExpr instanceof Expr) {
                Node docstrNode = firstExpr.asExpr().value;
                if (docstrNode != null && docstrNode instanceof Str) {
                    return docstrNode.asStr();
                }
            }
        }
        return null;
    }


    @NotNull
    public static Type resolveExpr(@NotNull Node n, Scope s) {
        return n.resolve(s);
    }


    @NotNull
    abstract public Type resolve(Scope s);


    public boolean isCall() {
        return this instanceof Call;
    }


    public boolean isModule() {
        return this instanceof Module;
    }


    public boolean isClassDef() {
        return this instanceof ClassDef;
    }


    public boolean isFunctionDef() {
        return this instanceof FunctionDef;
    }


    public boolean isLambda() {
        return this instanceof Lambda;
    }


    public boolean isName() {
        return this instanceof Name;
    }


    public boolean isGlobal() {
        return this instanceof Global;
    }


    @NotNull
    public Call asCall() {
        return (Call) this;
    }


    @NotNull
    public Module asModule() {
        return (Module) this;
    }


    @NotNull
    public Block asBlock() {
        return (Block) this;
    }


    @NotNull
    public Str asStr() {
        return (Str) this;
    }


    @NotNull
    public ClassDef asClassDef() {
        return (ClassDef) this;
    }


    @NotNull
    public FunctionDef asFunctionDef() {
        return (FunctionDef) this;
    }


    @NotNull
    public Lambda asLambda() {
        return (Lambda) this;
    }


    @NotNull
    public Name asName() {
        return (Name) this;
    }


    @NotNull
    public Expr asExpr() {
        return (Expr) this;
    }


    @NotNull
    public NList asNList() {
        return (NList) this;
    }


    @NotNull
    public Attribute asAttribute() {
        return (Attribute) this;
    }


    @NotNull
    public Tuple asTuple() {
        return (Tuple) this;
    }


    @NotNull
    public Global asGlobal() {
        return (Global) this;
    }


    // Does the node bind names?
    public boolean isDefinition() {
        return (this instanceof ClassDef) || (this instanceof FunctionDef);
    }


    @Nullable
    public Name getName() {
        return null;
    }


    public boolean hasParent() {
        return parent != null;
    }


    // Does the node bind names?
    public boolean bindsName() {
        return (this instanceof Assign) ||
                (this instanceof ClassDef) ||
                (this instanceof FunctionDef) ||
                (this instanceof Comprehension) ||
                (this instanceof ExceptHandler) ||
                (this instanceof For) ||
                (this instanceof Import) ||
                (this instanceof ImportFrom);
    }


    protected void addError(String msg) {
        Indexer.idx.putProblem(this, msg);
    }


    /**
     * Utility method to resolve every node in {@code nodes} and
     * return the union of their types.  If {@code nodes} is empty or
     * {@code null}, returns a new {@link org.yinwang.pysonar.types.UnknownType}.
     */
    @NotNull
    protected Type resolveListAsUnion(@Nullable List<? extends Node> nodes, Scope s) {
        if (nodes == null || nodes.isEmpty()) {
            return Indexer.idx.builtins.unknown;
        }

        Type result = Indexer.idx.builtins.unknown;
        for (Node node : nodes) {
            Type nodeType = resolveExpr(node, s);
            result = UnionType.union(result, nodeType);
        }
        return result;
    }


    /**
     * Resolves each element of a node list in the passed scope.
     * Node list may be empty or {@code null}.
     */
    static protected void resolveList(@Nullable List<? extends Node> nodes, Scope s) {
        if (nodes != null) {
            for (Node n : nodes) {
                resolveExpr(n, s);
            }
        }
    }


    @Nullable
    static protected List<Type> resolveAndConstructList(@Nullable List<? extends Node> nodes, Scope s) {
        if (nodes == null) {
            return null;
        } else {
            List<Type> typeList = new ArrayList<>();
            for (Node n : nodes) {
                typeList.add(resolveExpr(n, s));
            }
            return typeList;
        }
    }


    public String toDisplay() {
        return "";
    }


    public abstract void visit(NodeVisitor visitor);


    protected void visitNode(@Nullable Node n, NodeVisitor v) {
        if (n != null) {
            n.visit(v);
        }
    }


    protected void visitNodeList(@Nullable List<? extends Node> nodes, NodeVisitor v) {
        if (nodes != null) {
            for (Node n : nodes) {
                if (n != null) {
                    n.visit(v);
                }
            }
        }
    }
}
