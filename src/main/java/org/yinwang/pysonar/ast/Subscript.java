package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;


public class Subscript extends Node {

    @NotNull
    public Node value;
    @NotNull
    public Node slice;  // an NIndex or NSlice


    public Subscript(@NotNull Node value, @NotNull Node slice, int start, int end) {
        super(start, end);
        this.value = value;
        this.slice = slice;
        addChildren(value, slice);
    }


    @NotNull
    @Override
    public Type resolve(Scope s) {
        Type vt = resolveExpr(value, s);
        Type st = resolveExpr(slice, s);

        if (vt.isUnionType()) {
            Type retType = Indexer.idx.builtins.unknown;
            for (Type t : vt.asUnionType().getTypes()) {
                retType = UnionType.union(retType, getSubscript(t, st, s));
            }
            return retType;
        } else {
            return getSubscript(vt, st, s);
        }
    }


    @NotNull
    private Type getSubscript(@NotNull Type vt, @NotNull Type st, Scope s) {
        if (vt.isUnknownType()) {
            return Indexer.idx.builtins.unknown;
        } else if (vt.isListType()) {
            return getListSubscript(vt, st, s);
        } else if (vt.isTupleType()) {
            return getListSubscript(vt.asTupleType().toListType(), st, s);
        } else if (vt.isDictType()) {
            ListType nl = new ListType(vt.asDictType().valueType);
            return getListSubscript(nl, st, s);
        } else if (vt.isStrType()) {
            if (st.isListType() || st.isNumType()) {
                return vt;
            } else {
                Indexer.idx.putProblem(this, "Possible KeyError (wrong type for subscript)");
                return Indexer.idx.builtins.unknown;
            }
        } else {
            return Indexer.idx.builtins.unknown;
        }
    }


    @NotNull
    private Type getListSubscript(@NotNull Type vt, @NotNull Type st, Scope s) {
        if (vt.isListType()) {
            if (st.isListType()) {
                return vt;
            } else if (st.isNumType()) {
                return vt.asListType().getElementType();
            } else {
                Type sliceFunc = vt.getTable().lookupAttrType("__getslice__");
                if (sliceFunc == null) {
                    addError("The type can't be sliced: " + vt);
                    return Indexer.idx.builtins.unknown;
                } else if (sliceFunc.isFuncType()) {
                    return Call.apply(sliceFunc.asFuncType(), null, null, null, null, this);
                } else {
                    addError("The type's __getslice__ method is not a function: " + sliceFunc);
                    return Indexer.idx.builtins.unknown;
                }
            }
        } else {
            return Indexer.idx.builtins.unknown;
        }
    }


    @NotNull
    @Override
    public String toString() {
        return "<Subscript:" + value + ":" + slice + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(value, v);
            visitNode(slice, v);
        }
    }
}
