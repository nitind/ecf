package org.eclipse.ecf.provider.comm.tcp;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.eclipse.ecf.provider.Trace;

public class ExObjectOutputStream extends ObjectOutputStream {

    private boolean replace = false;

    public static final Trace debug = Trace.create("connection");

    public ExObjectOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    public ExObjectOutputStream(OutputStream out, boolean backwardCompatibility)
            throws IOException, SecurityException {
        this(out);
        if (backwardCompatibility) {
            try {
                super.enableReplaceObject(true);
                replace = true;
                debug("replaceObject");
            } catch (Exception e) {
                throw new IOException(
                        "Could not setup backward compatibility object replacers for ExObjectOutputStream");
            }
        }
    }
    protected void debug(String msg) {
        if (Trace.ON && debug != null) {
            debug.msg(msg);
        }    	
    }
    protected void dumpStack(String msg, Throwable e) {
        if (Trace.ON && debug != null) {
            debug.dumpStack(e,msg);
        }    	
    }
}