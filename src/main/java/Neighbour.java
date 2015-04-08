/**
 * Created by Tharindu Wijewardane on 2015-03-06.
 */
public class Neighbour {
    public Neighbour() {
    }

    public Neighbour(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    String ipAddress;
    int port;

    @Override
    public int hashCode() {
        return (ipAddress + port).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Neighbour) {
            return ((Neighbour) obj).ipAddress.equalsIgnoreCase(this.ipAddress) && ((Neighbour) obj).port == this.port;
        }
        return false;
    }
}
