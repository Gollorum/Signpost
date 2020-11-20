package gollorum.signpost.utils;

public enum ServerType {

    HostingClient(true),
    Dedicated(true),
    ConnectedClient(false);

    public final boolean isServer;

    ServerType(boolean isServer) {
        this.isServer = isServer;
    }

}