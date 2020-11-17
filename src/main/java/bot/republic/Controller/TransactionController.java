package bot.republic.Controller;

import bot.republic.RepublicApplication;
import bot.republic.entity.RepublicBot;
import bot.republic.entity.Player;
import bot.republic.entity.Voucher;
import bot.republic.model.Inbox;
import bot.republic.model.Outbox;
import bot.republic.pojo.ResponsePojo;
import bot.republic.pojo.VoucherPojo;
import bot.republic.utility.DBUtilInboxes;
import bot.republic.utility.DBUtilOutboxes;
import bot.republic.utility.InputValidator;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TransactionController {

    private static final String STATUS_FAILED = "Failed";
    private static final String STATUS_SUCCESS = "Success";
    private static final String TAG_STATUS_DEFAULT = STATUS_FAILED;
    private static final String TAG_MESSAGE_DEFAULT = " ";

    private static final String TAG_TRX_ID = "Trx ID";
    private static final String TAG_PLAYER_ID = "Player ID";

    private static final String MSG_ERROR_TRX_PLAYER_ID = "TrxID/PlayerID Tidak Valid";
    private static final String MSG_ERROR_TRX_ID_EXISTS = "TrxID Sudah Terdapat Di Database";
    private static final String MSG_ERROR_DENOM_INVALID = "Denom Tidak Valid";
    private static final String MSG_ERROR_INBOX_UNSAVED = "Failed Save to Inbox";
    private static final String MSG_ERROR_OUTBOX_UNSAVED = "Failed Save to Outbox";
    private static final String MSG_ERROR_UNKNOWN = "Unknown";

    private static final String DENOM_TYPE_DIAMOND = "diamond";
    private static final String DENOM_TYPE_MEMBER = "member";

    private static final Map<String, String> denomsMap = new HashMap<>();
    // ... insert stuff into map
    // eg: map.add("something", new MyObject());

    private RepublicBot republic;

    private static final boolean STATUS_DEFAULT_TRUE = true;
    private static final boolean STATUS_DEFAULT_FALSE = false;

    private Integer inboxId;

    private ResponsePojo responsePojo;
    private VoucherPojo voucherPojo;
    private String response;

    private String playerId;

    private JSONObject jsonObject;

    @GetMapping("/trx/{playerId}/{denom}/{trxId}")
    public String topUp(
            @PathVariable String playerId,
            @PathVariable String denom,
            @PathVariable String trxId,
            HttpServletRequest request)
            throws IOException {

        initDenomsUri();
        printRequest(request.getRequestURI());

        initResponsePojo(trxId, TAG_STATUS_DEFAULT, MSG_ERROR_TRX_PLAYER_ID);
        initVoucherPojo(playerId, denom);

        try {
            if (!areTrxIdAndPlayerIdValid()){
                updateResponseMessage(MSG_ERROR_TRX_PLAYER_ID);
            }else{
                if (isTrxIdAlreadyExists(responsePojo.getTrxId()))
                    updateResponseMessage(MSG_ERROR_TRX_ID_EXISTS);
                else {
                    if (!isDenomValid(voucherPojo.getDenom()))
                        updateResponseMessage(MSG_ERROR_DENOM_INVALID);
                    else {
                        saveToInbox(createNewInbox(trxId, playerId, denom, request));
                        checkInboxisSavedAndStartTransaction();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            RepublicApplication.logger.info("Transaction Failed");
        }


        jsonObject = new JSONObject(responsePojo);
        response = jsonObject.toString();

        printResponseLogAndSaveOutbox();

        return response;
    }

    private boolean areTrxIdAndPlayerIdValid() {
        return (InputValidator.isInputNumeric(responsePojo.getTrxId()) && InputValidator.isInputNumeric(voucherPojo.getPlayerId()));
    }

    private boolean areTrxIdAndPlayerIdNumeric(String trxId, String playerId) {
        return (InputValidator.isInputNumeric(trxId) && InputValidator.isInputNumeric(playerId));
    }

    private void initResponsePojo(String trxId, String defaultStatus, String defaultMessage) {
        responsePojo = ResponsePojo.byWithTrxIdStatusMessage(trxId, defaultStatus, defaultMessage);
    }

    private void initVoucherPojo(String playerId, String denom)
    {
        voucherPojo = VoucherPojo.byPlayerIdAndDenom(playerId, denom);
    }

    private void printRequest(String requestURI) {
        RepublicApplication.logger.info("Incoming Request " + requestURI);
    }

    private void printResponseLogAndSaveOutbox() {
        RepublicApplication.logger.info("Returning JSON : ");
        RepublicApplication.logger.info(jsonObject.toString(4));
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

    private Inbox createNewInbox(String trxId, String playerId, String denom, HttpServletRequest request) {
        String message = trxId + "#" + denom + "#" + playerId;
        return new Inbox(message, request.getRequestURI(), 0, getJavaUtilDate(), trxId);
    }

    private void saveToOutbox(Outbox newBoutbox) {
        try {
            Integer outboxId = DBUtilOutboxes.saveOutbox(newBoutbox);
        } catch (Exception e) {
            e.printStackTrace();
            RepublicApplication.logger.info("Failed Save To Outbox");
        }
    }

    private void saveToInbox(Inbox newInbox) {
        try {
            inboxId = DBUtilInboxes.saveInbox(newInbox);
        } catch (Exception e) {
            e.printStackTrace();
            RepublicApplication.logger.info("Failed Save To DB");
        }
    }

    private Outbox createNewBoutbox() {
        return new Outbox(response, null, getJavaUtilDate(), inboxId);
    }

    private boolean isSaveToInboxSucceed() {
        return inboxId != null;
    }

    private String getDenomType() {
        return (InputValidator.isInputNumeric(voucherPojo.getDenom())) ? DENOM_TYPE_DIAMOND : DENOM_TYPE_MEMBER;
    }

    private boolean isDenomValid(String denom) {
        if (denomsMap.containsKey(denom))
            return true;
        else return false;
    }

    private java.util.Date getJavaUtilDate() {
        return new java.util.Date();
    }

//    private java.sql.Date getjavaSqlDate(){
//        return new java.sql.Date(new java.util.Date().getTime());
//    }

    private boolean isTrxIdAlreadyExists(String trxId) {
        return DBUtilInboxes.isTrxIdExists(trxId);
    }



    private void updateResponseMessage(String message) {
        responsePojo.setMessage(message);
    }

    private static void initDenomsUri() {
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
    }

}
