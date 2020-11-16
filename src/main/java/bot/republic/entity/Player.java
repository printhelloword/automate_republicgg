package bot.republic.entity;

public class Player {
    private String playerId;
    private String zoneId;

    public String getPlayerId() {
        return playerId;
    }

    public static Player byMergedPlayerAndZoneId(String playerId){
        String[] splittedPlayerIdAndZoneid = getSplittedPlayerIdAndZoneId(playerId);
        return new Player(splittedPlayerIdAndZoneid[0], splittedPlayerIdAndZoneid[1]);
    }

    private static String[] getSplittedPlayerIdAndZoneId(String playerId) {
        String[] playerIdAndZoneId = new String[2];
        if (playerId.length() < 14) {
            playerIdAndZoneId[0] = playerId.substring(0, playerId.length() - 4);
            playerIdAndZoneId[1] = playerId.substring(playerId.length() - 4);
        } else {
            playerIdAndZoneId[0] = playerId.substring(0, playerId.length() - 5);
            playerIdAndZoneId[1] = playerId.substring(playerId.length() - 5);
        }
        return playerIdAndZoneId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public Player() {
    }

    public Player(String playerId, String zoneId) {
        this.playerId = playerId;
        this.zoneId = zoneId;
    }
}
