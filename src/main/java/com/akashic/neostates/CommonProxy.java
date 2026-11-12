package com.akashic.neostates;

public class CommonProxy {

    public void init() {}

    public void postInit() {}

    /** A layer message from the server; only the client has anything to do with it. */
    public void receive(Object message) {}
}
