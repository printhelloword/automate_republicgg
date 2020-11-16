package bot.republic.pojo;

import bot.republic.entity.Player;

public class VoucherPojo {
    private String playerId;
    private String denom;

    public static VoucherPojo byPlayerIdAndDenom(String playerId, String denom){
        return new VoucherPojo(playerId, denom);
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getDenom() {
        return denom;
    }

    public void setDenom(String denom) {
        this.denom = denom;
    }

    public VoucherPojo() {
    }

    public VoucherPojo(String playerId, String denom) {
        this.playerId = playerId;
        this.denom = denom;
    }
}
