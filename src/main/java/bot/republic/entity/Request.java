package bot.republic.entity;

public class Request {
    private String trxId;
    private String denom;
    private String playerId;

    public Request() {
    }

    public Request(String trxId, String denom, String playerId) {
        this.trxId = trxId;
        this.denom = denom;
        this.playerId = playerId;
    }

    public static Request ofParams(String trxId, String denom, String playerId) {
        return new Request(trxId, denom, playerId);
    }

    public String getTrxId() {
        return trxId;
    }

    public String getDenom() {
        return denom;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String mergedRequestSeparatedBySlash(){
        return playerId+ "/" +denom+ "/" +trxId;
    }
}
