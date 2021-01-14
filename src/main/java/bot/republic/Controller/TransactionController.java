package bot.republic.Controller;

import bot.republic.RepublicApplication;
import bot.republic.entity.Player;
import bot.republic.entity.RepublicBot;
import bot.republic.entity.Request;
import bot.republic.entity.Voucher;
import bot.republic.model.Inbox;
import bot.republic.model.Outbox;
import bot.republic.pojo.ResponsePojo;
import bot.republic.pojo.VoucherPojo;
import bot.republic.utility.DBUtilInboxes;
import bot.republic.utility.DBUtilOutboxes;
import bot.republic.utility.InputValidator;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TransactionController {

    private static final String STATUS_FAILED = "Failed";
    private static final String STATUS_SUCCESS = "Success";
    private static final String TAG_STATUS_DEFAULT = STATUS_FAILED;

    private static final String MSG_ERROR_TRX_PLAYER_ID = "TrxID/PlayerID Tidak Valid";
    private static final String MSG_ERROR_TRX_ID_EXISTS = "TrxID Sudah Terdapat Di Database";
    private static final String MSG_ERROR_DENOM_INVALID = "Denom Tidak Valid";

    private static final String DENOM_TYPE_DIAMOND = "diamond";
    private static final String DENOM_TYPE_MEMBER = "member";

    private static final Map<String, String> denomsMap = getDenoms();

    private Request request;
    private Integer inboxId;
    private ResponsePojo responsePojo;
    private VoucherPojo voucherPojo;

    public TransactionController(Request request) {
        this.request = request;
    }

    public static TransactionController ofRequest(Request request) {
        return new TransactionController(request);
    }

    public ResponsePojo getResponsePojo() {
        initResponsePojo();
        initVoucherPojo();

        try {
            validateRequestAndStartTransaction();
        } catch (Exception e) {
            RepublicApplication.logger.info(e.getMessage());
        }

        printResponseJsonAndSaveToOutbox(new JSONObject(responsePojo));
        return responsePojo;
    }

    private void validateRequestAndStartTransaction() throws Exception {
        if (!areTrxIdAndPlayerIdValid()) {
            updateResponseMessage(MSG_ERROR_TRX_PLAYER_ID);
        } else {
            if (isTrxIdAlreadyExists())
                updateResponseMessage(MSG_ERROR_TRX_ID_EXISTS);
            else {
                if (!isDenomValid())
                    updateResponseMessage(MSG_ERROR_DENOM_INVALID);
                else {
                    saveToInbox(createNewInbox());
                    checkInboxisSavedAndStartTransaction();
                }
            }
        }
    }

    private boolean areTrxIdAndPlayerIdValid() {
        return (InputValidator.isInputNumeric(responsePojo.getTrxId()) && InputValidator.isInputNumeric(voucherPojo.getPlayerId()));
    }

    private void initResponsePojo() {
        responsePojo = ResponsePojo.byWithTrxIdStatusMessage(request.getTrxId(), TAG_STATUS_DEFAULT, "");
    }

    private void initVoucherPojo() {
        voucherPojo = VoucherPojo.byPlayerIdAndDenom(request.getPlayerId(), request.getDenom());
    }

    private void printResponseJsonAndSaveToOutbox(JSONObject jsonObject) {
        RepublicApplication.logger.info("Returning JSON : ");
        RepublicApplication.logger.info(jsonObject.toString(4));
        if (inboxId != null)
            saveToOutbox(createNewBoutbox());
    }

    private void checkInboxisSavedAndStartTransaction() {
        if (isSaveToInboxSucceed()) {
            Player player = Player.byMergedPlayerAndZoneId(voucherPojo.getPlayerId());
            Voucher voucher = Voucher.byPlayerDenomAndType(player, denomsMap.get(voucherPojo.getDenom()), getDenomType());
            RepublicBot republicBot = RepublicBot.withVoucher(voucher);
            Map<Boolean, String> transactionResult = republicBot.processTopTupAndGetMessage();
            if (transactionResult != null) {
                responsePojo.setVoucher(voucherPojo);
                for (Map.Entry<Boolean, String> entry : transactionResult.entrySet()) {
                    responsePojo.setStatus((entry.getKey()) ? STATUS_SUCCESS : STATUS_FAILED);
                    responsePojo.setMessage(entry.getValue());
                }
            }
        }
    }

    private Inbox createNewInbox() {
        String message = request.getTrxId() + "#" + request.getDenom() + "#" + request.getPlayerId();
        return new Inbox(message, getRequesParams(), 0, getJavaUtilDate(), request.getTrxId());
    }

    private void saveToOutbox(Outbox newBoutbox) {
        try {
            DBUtilOutboxes.saveOutbox(newBoutbox);
        } catch (Exception e) {
            RepublicApplication.logger.info(e.getMessage());
            RepublicApplication.logger.info("Failed Save To Outbox");
        }
    }

    private void saveToInbox(Inbox newInbox) {
        try {
            inboxId = DBUtilInboxes.saveInbox(newInbox);
        } catch (Exception e) {
            RepublicApplication.logger.info(e.getMessage());
            RepublicApplication.logger.info("Failed Save To DB");
        }
    }

    private Outbox createNewBoutbox() {
        return new Outbox(new JSONObject(responsePojo).toString(), null, getJavaUtilDate(), inboxId);
    }

    private boolean isSaveToInboxSucceed() {
        return inboxId != null;
    }

    private String getDenomType() {
        return (InputValidator.isInputNumeric(voucherPojo.getDenom())) ? DENOM_TYPE_DIAMOND : DENOM_TYPE_MEMBER;
    }

    private boolean isDenomValid() {
        return denomsMap.containsKey(request.getDenom());
    }

    private java.util.Date getJavaUtilDate() {
        return new java.util.Date();
    }

    private boolean isTrxIdAlreadyExists() {
        return DBUtilInboxes.isTrxIdExists(request.getTrxId());
    }

    private void updateResponseMessage(String message) {
        responsePojo.setMessage(message);
    }

    private String getRequesParams() {
        return request.getPlayerId() + "/" + request.getDenom() + "/" + request.getTrxId();
    }

    private static Map<String, String> getDenoms() {
        return getDenomsLocatorInMap();
    }

    static Map<String, String> getDenomsLocatorInMap() {
        Map<String, String> denomsMap = new HashMap<>();

        denomsMap.put("14", "1");
        denomsMap.put("42", "3");
        denomsMap.put("70", "5");
        denomsMap.put("141", "10");
        denomsMap.put("282", "20");
        denomsMap.put("345", "25");
        denomsMap.put("415", "30");
        denomsMap.put("708", "50");
        denomsMap.put("1446", "100");
        denomsMap.put("2976", "200");
        denomsMap.put("7502", "500");
        denomsMap.put("twilight", "/mobile-legends-twilight-pass");
        denomsMap.put("starlight", "/mobile-legends-starlight-member");
        denomsMap.put("starlightplus", "/mobile-legends-starlight-member-plus");

        return denomsMap;
    }

    /*private java.sql.Date getjavaSqlDate(){
        return new java.sql.Date(new java.util.Date().getTime());
    }*/

}
