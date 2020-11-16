package bot.republic.entity;

public class Voucher {

    private Player player;
    private String denomUri;
    private String type;

    public String getType() {
        return type;
    }

    public Voucher() {

    }

    public Voucher(Player player, String denom, String type) {
        this.player = player;
        this.denomUri = denom;
        this.type=type;
    }

    public static Voucher byPlayerDenomAndType(Player player, String denom, String type){
        return new Voucher(player, denom, type);
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public String getDenomUri() {
        return denomUri;
    }




}
