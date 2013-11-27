package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.*;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;

import java.io.File;


public class Module extends Node {

    public String name;
    public Block body;

    private String file;   // input source file path
    private String sha1;   // input source file sha1


    public Module(Block body, String file, int start, int end) {
        super(start, end);
        this.name = _.moduleName(file);
        this.body = body;
        this.file = _.unifyPath(file);
        this.sha1 = _.getSHA1(new File(file));
        addChildren(body);
    }


    public void setFile(@NotNull File path) {
        file = _.unifyPath(path);
        name = _.moduleName(file);
        sha1 = _.getSHA1(path);
    }


    @Override
    public String getFile() {
        return file;
    }


    public String getSHA1() {
        return sha1;
    }


    @NotNull
    @Override
    public Type resolve(@NotNull Scope s) {
        ModuleType mt = new ModuleType(name, file, Indexer.idx.globaltable);
        Binder.bind(s, this, mt, Binding.Kind.MODULE);  // module's origin is itself
        resolveExpr(body, mt.getTable());
        return mt;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Module:" + file + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(body, v);
        }
    }
}
