package bot.republic.Controller;

import bot.republic.RepublicApplication;
import bot.republic.entity.RepublicBot;
import bot.republic.entity.Player;
import bot.republic.entity.Voucher;
import bot.republic.model.Inboxes;
import bot.republic.model.Outboxes;
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

    private static final String TAG_TRX_ID = "Trx ID";
    private static final String TAG_PLAYER_ID = "Player ID";

    private static final String MSG_ERROR_TRX_PLAYER_ID = "Invalid Trx ID / Player ID";
    private static final String MSG_ERROR_TRX_ID_EXISTS = "Trx ID Already Exists";
    private static final String MSG_ERROR_DENOM_INVALID = "Invalid Denom";
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

    private Player player;
    private boolean isRequestValid;
    private Integer inboxId;
    private Integer outboxId;

    private ResponsePojo responsePojo;
    private VoucherPojo voucherPojo;
    private String response;

    private RepublicBot republicBot;
    private Voucher voucher;

    private Map<Boolean, String> transactionResult;

    private JSONObject jsonObject;

    @GetMapping("/trx/{playerId}/{denom}/{trxId}")
    public String topUp(
            @PathVariable String playerId,
            @PathVariable String denom,
            @PathVariable String trxId,
            HttpServletRequest request)
            throws IOException {

        initDenomsUri();
        initResponseAndValidateRequest(trxId, playerId, denom);
        if (isReadyForTransaction()) {
            saveToInbox(createNewInbox(trxId, playerId, denom, request));
            try {
                saveInboxANdStartTransaction();
            }catch (Exception e){
                e.printStackTrace();
                RepublicApplication.logger.info("Transaction Failed");
            }
        }

        jsonObject = new JSONObject(responsePojo);
        response = jsonObject.toString();

        printResponseLogAndSaveOutbox();

        return response;
    }

    private void printResponseLogAndSaveOutbox() {
        RepublicApplication.logger.info("Returning JSON : ");
        RepublicApplication.logger.info(jsonObject.toString(4));
        saveToOutbox(createNewBoutbox());
    }

    private void saveInboxANdStartTransaction(){
        if (isSaveToInboxSucceed()) {
            player = Player.byMergedPlayerAndZoneId(voucherPojo.getPlayerId());
            voucher = Voucher.byPlayerDenomAndType(player, denomsMap.get(voucherPojo.getDenom()), getDenomType());
            republicBot = RepublicBot.withVoucher(voucher);
            transactionResult = republicBot.processTopTupAndGetMessage();
            if (transactionResult != null) {
                for (Map.Entry<Boolean, String> entry : transactionResult.entrySet()) {
                    responsePojo.setStatus((entry.getKey()) ? STATUS_SUCCESS : STATUS_FAILED);
                    responsePojo.setMessage(entry.getValue());
                }
            }
        }
    }

    private Inboxes createNewInbox(String trxId, String playerId, String denom, HttpServletRequest request) {
        String message = trxId + "#" + denom + "#" + playerId;
        return new Inboxes(message, request.getRequestURI(), 0, getJavaUtilDate(), trxId);
    }

    private void saveToOutbox(Outboxes newBoutbox) {
        try {
            outboxId = DBUtilOutboxes.saveOutbox(newBoutbox);
        } catch (Exception e) {
            e.printStackTrace();
            RepublicApplication.logger.info("Failed Save To Outbox");
        }
    }

    private void saveToInbox(Inboxes newInbox) {
        try {
            inboxId = DBUtilInboxes.saveInbox(newInbox);
        } catch (Exception e) {
            e.printStackTrace();
            RepublicApplication.logger.info("Failed Save To DB");
        }
    }

    private Outboxes createNewBoutbox() {
        return new Outboxes(response, null, getJavaUtilDate(), inboxId);
    }

    private boolean isSaveToInboxSucceed() {
        return inboxId != null;
    }

    private String getDenomType() {
        return (InputValidator.isInputNumeric(voucherPojo.getDenom())) ? DENOM_TYPE_DIAMOND : DENOM_TYPE_MEMBER;
    }

    private void initResponseAndValidateRequest(String trxId, String playerId, String denom) {
        responsePojo = ResponsePojo.byWithTrxIdStatusMessage(trxId, TAG_STATUS_DEFAULT, MSG_ERROR_UNKNOWN );
        if (!areTrxIdAndPlayerIdNumeric(trxId, playerId))
            updateResponseMessage(MSG_ERROR_TRX_PLAYER_ID);
        else {
            if (isTrxIdAlreadyExists(trxId))
                updateResponseMessage(MSG_ERROR_TRX_ID_EXISTS);
            else {
                if (!isDenomValid(denom))
                    updateResponseMessage(MSG_ERROR_DENOM_INVALID);
                else {
                    voucherPojo = VoucherPojo.byPlayerIdAndDenom(playerId, denom);
                    isRequestValid = true;
                }
            }
        }
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

    private boolean isReadyForTransaction() {
        if (voucherPojo != null)
            return true;
        else return false;
    }

    private boolean isTrxIdAlreadyExists(String trxId) {
        if (DBUtilInboxes.isTrxIdExists(trxId))
            return true;
        else return false;
    }

    private boolean areTrxIdAndPlayerIdNumeric(String trxId, String playerId) {
        if (!InputValidator.isInputNumeric(trxId) || !InputValidator.isInputNumeric(playerId))
            return false;
        else
            return true;
    }

    private void updateResponseMessage(String message) {
        responsePojo.setMessage(message);
    }

    private static void initDenomsUri() {
        RepublicApplication.logger.info("Init Denoms");
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
