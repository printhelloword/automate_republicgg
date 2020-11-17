package bot.republic.utility;

import bot.republic.RepublicApplication;

public class InputValidator {

    public static boolean isInputNumeric(String input) {
        boolean status = false;
        try {
            Long.parseLong(input);
            status = true;
        } catch (Exception e) {
            RepublicApplication.logger.info("Input Not Numeric");
        }
        return status;
    }
}
