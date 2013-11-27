package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.ast.*;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.List;


/**
 * Bind names to scopes, including destructuring assignment.
 */
public class Binder {

    // If kind if unspecified, default to VARIABLE
    public static void bind(@NotNull Scope s, @NotNull Node target, @NotNull Type rvalue) {
        bind(s, target, rvalue, Binding.Kind.VARIABLE);
    }


    public static void bind(@NotNull Scope s, @NotNull Node target, @NotNull Type rvalue, Binding.Kind kind) {
        if (target instanceof Name) {
            bind(s, target.asName(), rvalue, kind);
        } else if (target instanceof Tuple) {
            bind(s, target.asTuple().elts, rvalue, kind);
        } else if (target instanceof NList) {
            bind(s, target.asNList().elts, rvalue, kind);
        } else if (target instanceof Attribute) {
            target.asAttribute().setAttr(s, rvalue);
        } else if (target instanceof Subscript) {
            Subscript sub = (Subscript) target;
            Type valueType = Node.resolveExpr(sub.value, s);
            Node.resolveExpr(sub.slice, s);
            if (valueType instanceof ListType) {
                ListType t = valueType.asListType();
                t.setElementType(UnionType.union(t.getElementType(), rvalue));
            }
        } else if (target != null) {
            Indexer.idx.putProblem(target, "invalid location for assignment");
        }
    }


    // Name
    public static void bind(@NotNull Scope s, @NotNull Name name, @NotNull Type rvalue, Binding.Kind kind) {
        if (s.isGlobalName(name.id)) {
            Binding b = new Binding(name.id, name, rvalue, kind);
            s.getGlobalTable().update(name.id, b);
            Indexer.idx.putRef(name, b);
        } else {
            s.insert(name.id, name, rvalue, kind);
        }
    }


    // lists and tuples
    public static void bind(@NotNull Scope s, @NotNull List<Node> xs, @NotNull Type rvalue, Binding.Kind kind) {
        if (rvalue.isTupleType()) {
            List<Type> vs = rvalue.asTupleType().getElementTypes();
            if (xs.size() != vs.size()) {
                reportUnpackMismatch(xs, vs.size());
            } else {
                for (int i = 0; i < xs.size(); i++) {
                    bind(s, xs.get(i), vs.get(i), kind);
                }
            }
        } else if (rvalue.isListType()) {
            bind(s, xs, rvalue.asListType().toTupleType(xs.size()), kind);
        } else if (rvalue.isDictType()) {
            bind(s, xs, rvalue.asDictType().toTupleType(xs.size()), kind);
        } else if (rvalue.isUnknownType()) {
            for (Node x : xs) {
                bind(s, x, Indexer.idx.builtins.unknown, kind);
            }
        } else {
            Indexer.idx.putProblem(xs.get(0).getFile(),
                    xs.get(0).start,
                    xs.get(xs.size() - 1).end,
                    "unpacking non-iterable: " + rvalue);
        }
    }


    // iterator
    public static void bindIter(@NotNull Scope s, Node target, @NotNull Node iter, Binding.Kind kind) {
        Type iterType = Node.resolveExpr(iter, s);

        if (iterType.isListType()) {
            bind(s, target, iterType.asListType().getElementType(), kind);
        } else if (iterType.isTupleType()) {
            bind(s, target, iterType.asTupleType().toListType().getElementType(), kind);
        } else {
            List<Binding> ents = iterType.getTable().lookupAttr("__iter__");
            if (ents != null) {
                for (Binding ent : ents) {
                    if (ent == null || !ent.getType().isFuncType()) {
                        if (!iterType.isUnknownType()) {
                            Indexer.idx.putProblem(iter, "not an iterable type: " + iterType);
                        }
                        bind(s, target, Indexer.idx.builtins.unknown, kind);
                    } else {
                        bind(s, target, ent.getType().asFuncType().getReturnType(), kind);
                    }
                }
            } else {
                bind(s, target, Indexer.idx.builtins.unknown, kind);
            }
        }
    }


    // helper for destructuring binding
    private static void reportUnpackMismatch(@NotNull List<Node> xs, int vsize) {
        int xsize = xs.size();
        int beg = xs.get(0).start;
        int end = xs.get(xs.size() - 1).end;
        int diff = xsize - vsize;
        String msg;
        if (diff > 0) {
            msg = "ValueError: need more than " + vsize + " values to unpack";
        } else {
            msg = "ValueError: too many values to unpack";
        }
        Indexer.idx.putProblem(xs.get(0).getFile(), beg, end, msg);
    }
}
