package gollorum.signpost.utils;

public enum ServerType {

    HostingClient(true, true),
    Dedicated(true, false),
    ConnectedClient(false, true);

    public final boolean isServer;
    public final boolean isClient;

    ServerType(boolean isServer, boolean isClient) {
        this.isServer = isServer;
        this.isClient = isClient;
    }

}